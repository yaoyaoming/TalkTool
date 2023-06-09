package com.kaicheng.talktool.talktool;

import android.Manifest;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.alibaba.fastjson.JSON;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AudioSendAndReceive implements IDataRecvEvent {
    /**
     * 音频板采集到的声音数据
     */
    public ConcurrentLinkedQueue<byte[]> sendDataQueue = new ConcurrentLinkedQueue<>();
    /**
     * 接收到的待播放到音频板的声音数据
     */
    public ConcurrentLinkedQueue<byte[]> recvDataQueue = new ConcurrentLinkedQueue<>();

    private static final String TAG = "AudioSendAndReceive";
    private AudioTrack mAudioTrack;
    public boolean isRunning = false;
    public boolean loginStatus = false;
    private final ClientConnect clientConnect = new ClientConnect();
    NetworkServer netServer = null;
    InetAddress udpTalkSendAddress = null;
    int udpTalkSendport = 0;

    int udpState = 0;
    public IAudioCallback audioCallback = null;

    Thread sendThread = null;
    private Context mContext;

    public AudioSendAndReceive(Context context) throws SocketException {
        mContext = context;
    }
    /**
     * 初始化通讯功能
     *
     * @return
     */
    public int init(IAudioCallback audioCallback) {
        this.audioCallback = audioCallback;
        netServer = new NetworkServer();
        netServer.init();
        netServer.serverTask.setIDataRecvEvent(this);

        return 0;
    }

    /**
     * 启动双向语音对讲功能
     *
     * @param volume              音量 0-100
     * @param intercomInputSource 对讲输入源
     * @return 0-正常开启,1-已经开启，无需重复开启
     */
    public int startTalk(int volume, IntercomInputSource intercomInputSource) {
        if (isRunning) {
            return 1;
        }
        //等待对讲服务器初始化登录成功
        int n = 0;
        while (!loginStatus) {
            ThreadUtil.sleep(100);
            n++;
            if (n > 100) {
                break;
            }
        }
        isRunning = true;
        //开启UDPclient，设置端口为9999
        StartTalkUdpConnect();
        //发送对讲命令给设备
        CommandStart commandStart = new CommandStart();
        commandStart.command = "start";
        commandStart.aecmode = "disable";
        commandStart.mode = "recvsend";
        commandStart.streamtype = "g711-a";
        commandStart.dataserver = "0.0.0.0";
        commandStart.dataserverport = 9999;
        if (volume == 0) {
            volume = 90;
        }
        commandStart.volume = volume;
        commandStart.samplerate = 8000;
        commandStart.buffer = "disable";
        commandStart.nodelay = null;
        commandStart.inputsource = intercomInputSource.toString();
        commandStart.inputgain = 20;
        commandStart.timeout = 0;
        commandStart.result = 0;
        commandStart.protocol = "rtp";
        commandStart.param = clientConnect.id;

        String jsontext1 = JSON.toJSONString(commandStart);
        // 判断收到的消息是否是正确的消息，是的话，回复

        List<String> msgs = new ArrayList<>();
        msgs.add(jsontext1);
        netServer.serverTask.sendData(new WriteTask("talk", msgs));
        return 0;
    }


    /**
     * 停止双向语音对讲功能
     *
     * @return
     */
    public int stopTalk() {
        //发送对讲命令给设备
        CommandStop commandStop = new CommandStop();
        commandStop.command = "stop";
        String jsontext1 = JSON.toJSONString(commandStop);
        // 判断收到的消息是否是正确的消息，是的话，回复

        List<String> msgs = new ArrayList<>();
        msgs.add(jsontext1);
        netServer.serverTask.sendData(new WriteTask("talk", msgs));
        isRunning = false;
        return 0;
    }

    public AudioSendAndReceive() throws SocketException {
    }
    private static String[] PERMISSION_AUDIO = {
            Manifest.permission.RECORD_AUDIO
    };
    // 1.定义服务器的地址、端口号、数据
    // 2.定义服务器端口
    // 3.创建发送端对象：发送端自带默认端口号
    DatagramSocket datagramSocket = new DatagramSocket(9999);
    /**
     * 是否本地播放接收到的声音数据
     */
    public boolean isPlayReceiveAudio = false;
    /**
     * 是否使用本地mic进行采集发送数据
     */
    public boolean isUseLocalVoice = false;


    private byte[] decodeData(byte[] g711Data, int length) {
        byte[] pcmData = new byte[length * 2];
        for (int i = 0, j = 0; i < length; i++, j += 2) {
            short pcmSample = ulawToLinearSample(g711Data[i]);
            pcmData[j] = (byte) (pcmSample & 0xff);
            pcmData[j + 1] = (byte) ((pcmSample >> 8) & 0xff);
        }
        return pcmData;
    }

    private short ulawToLinearSample(byte ulawSample) {
        ulawSample = (byte) (~ulawSample);
        short pcmSample = (short) (((ulawSample & 0x0F) << 8) | ((ulawSample & 0xF0) << 4));
        pcmSample += 132;
        return pcmSample;
    }

    /**
     * 开启音频对讲方法
     *
     * @return
     */
//申请录音权限
    private final int SAMPLE_RATE = 8000;
    //申请录音权限
    private static final int GET_RECODE_AUDIO = 1;
    private final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int mBufferSizeInBytes = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, mAudioFormat);

    private int recBuffSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

    private int StartTalkUdpConnect() {

        // 4.客户端启动成功，输出提示信息
        udpState = 0;
        udpTalkSendAddress = null;
        udpTalkSendport = 0;
        try {
            datagramSocket.setReceiveBufferSize(2048);
            datagramSocket.setSoTimeout(2000);
        } catch (Exception ex) {

        }

        Thread receiveAudioTh = new Thread(() -> {
            try {
                mAudioTrack = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(mAudioFormat)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build())
                        .setBufferSizeInBytes(mBufferSizeInBytes)
                        .build();
                mAudioTrack.play();
                udpState = 0;
                // 循环采集音频并发送到服务端
                while (true) {
                    try {
                        /*********************************************************************************************/
                        // 6.1 创建一个数据包对象接收数据
                        byte[] buf = new byte[1024 * 64];
                        InetAddress inetAddress = InetAddress.getByName(clientConnect.devip);
                        DatagramPacket packet = new DatagramPacket(buf,
                                0,
                                buf.length,
                                inetAddress,
                                clientConnect.devport);
                        // 6.2 接收服务器响应的数据
                        datagramSocket.receive(packet);
                        if (udpState == 0) {
                            //先将对方的端口号和ip地址接回来
                            udpTalkSendAddress = packet.getAddress();
                            udpTalkSendport = packet.getPort();
                            udpState = 1;
                        } else {
                            // 6.3 取出数据
                            int len = packet.getLength();

                            // 回来的是rtp数据，需要解析成g711数据，再进行解码
                            RtpPacket rtpPacket = new RtpPacket(buf, len);
                            if (rtpPacket.getPayloadType() == RtpPayloadType.G711_aLaw && rtpPacket.getPayload() != null) {
                                byte[] pcmBuf = CMG711.decode(rtpPacket.getPayload());
                                if (isPlayReceiveAudio) {
                                    mAudioTrack.write(pcmBuf, 0, pcmBuf.length);
                                }

                                if (sendDataQueue.size() < 100) {
                                    sendDataQueue.add(pcmBuf);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        Thread.sleep(1);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        receiveAudioTh.start();


//
        // 5.向服务端发送信息(播放)
        Thread sendAudioTh = new Thread(() -> {
            int sequenceNumber = 0;
            long timestamp = 0;
            try {
                AudioRecord audioRecord = null;
                byte[] voiceBuf = new byte[1024];
                if (isUseLocalVoice) {

                    int permission = ActivityCompat.checkSelfPermission(mContext,
                            Manifest.permission.RECORD_AUDIO);

                    audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, recBuffSize);

                    // 将 audioRecord 初始化为未录制状态的对象
                    if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                        audioRecord.startRecording();
                    }

                }

                while (true) {
                    try {
                        if (udpState == 1) {
                            byte[] buffer = new byte[160];
                            int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                            byte[] g711Buf = null;

                            g711Buf = CMG711.encode(buffer);

                            if (g711Buf != null) {
                                RtpPacket packet = new RtpPacket(RtpPayloadType.G711_aLaw,
                                        sequenceNumber,
                                        timestamp,
                                        g711Buf,
                                        g711Buf.length);
                                sequenceNumber++;
                                //System.out.println("sequenceNumber:" + sequenceNumber);
                                timestamp += g711Buf.length;
                                DatagramPacket packets = new DatagramPacket(packet.toArray(),
                                        packet.getLength(),
                                        udpTalkSendAddress,
                                        udpTalkSendport);
                                datagramSocket.send(packets);
                            }

                        }
                    } catch (Exception ex) {
                        Log.i(TAG, "******audioUtil **********sendAudioTh error" + ex.toString());
                    } finally {
                        Thread.sleep(10);
                    }
                }
            } catch (Exception e) {
                Log.i(TAG, "******audioUtil **********sendAudioTh error" + e.toString());
            }
        });
        sendAudioTh.start();


        return 0;
    }


    @Override
    public void RecvData(BaseProtocol protocol) {
        CommandRegister cr = JSON.parseObject(protocol.getMessage(), CommandRegister.class);
        Log.i(TAG, "******audioUtil ************RecvData************" + protocol.getMessage());
        if (cr != null) {
            if (cr.command.equals("register")) {
                Log.i(TAG, "******audioUtil ***********register**********" + protocol.getMessage());
                if ((cr.authentication != null) && (cr.session != null)) {
                    clientConnect.id = cr.id;
                    //回复消息
                    CommandRegister cr2 = new CommandRegister();
                    cr2.command = "register";
                    cr2.result = 200;
                    cr2.systick = 1;//每隔1秒上传一次心跳

                    String jsontext1 = JSON.toJSONString(cr2);
                    // 判断收到的消息是否是正确的消息，是的话，回复
                    List<String> msgs = new ArrayList<>();
                    msgs.add(jsontext1);
                    netServer.serverTask.sendData(new WriteTask("register", msgs));
                } else if (cr.result == 200) {
                    Log.i(TAG, "******audioUtil ************cr.result == 200************" + netServer.devip + "*****" + netServer.devport + "***********");
                    loginStatus = true;
                    clientConnect.devip = netServer.devip;
                    clientConnect.devport = netServer.devport;
                    clientConnect.socket = netServer.socket;
                    Log.i(TAG, "***audioUtil *****设备登录成功," + clientConnect);

                }
            }
            if (cr.command.equals("status")) {
                //设备当前状态信息上传
            }

            if (cr.command.equals("message")) {
                Log.i(TAG, "******audioUtil ************message**********"+ protocol.getMsgContent());
            }

            if (cr.command.equals("close")) {
            }
        }

    }
}

