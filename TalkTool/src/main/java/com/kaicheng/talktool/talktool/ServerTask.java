package com.kaicheng.talktool.talktool;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerTask implements Runnable {

    // 客户端验证结果
    private final boolean clientVerifyResult = false;
    Heartbeat heartbeat = new Heartbeat();
    boolean stopTask = false;
    Socket socket;
    BufferedReader bufferReader;
    BufferedWriter bufferWriter;
    String clientAddress;
    // 存储从客户端收到的消息
    ConcurrentLinkedQueue<BaseProtocol> protocolQueue = new ConcurrentLinkedQueue<>();
    ConcurrentLinkedQueue<WriteTask> writeTaskQueue = new ConcurrentLinkedQueue<>();
    long heartIntervalMillis;
    long lastHeartTime;
    private String sessionId;
    private IDataRecvEvent iDataRecvEvent;

    public ServerTask() {
        //this.socket = socket;
    }

    public void setIDataRecvEvent(IDataRecvEvent iDataRecvEvent) {
        this.iDataRecvEvent = iDataRecvEvent;
    }

    public int sendData(WriteTask writeTask) {
        writeTaskQueue.add(writeTask);
        return 0;
    }

    private boolean init() {
        try {
            bufferReader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            bufferWriter = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8));
            clientAddress = socket.getRemoteSocketAddress().toString();

            heartIntervalMillis = ConfigUtil.getHeartbeatInterval() * 1000;
            lastHeartTime = 0;

            return true;
        } catch (Exception e) {
            Global.logger(e.toString());
        }

        return false;
    }

    @Override
    public void run() {
        if (!init()) {
            close();
            return;
        }
        Global.logger("audioUtil start a server task,client address: " + clientAddress);

        ServerReadTask readTask = new ServerReadTask();
        ServerWriteTask writeTask = new ServerWriteTask();
        readTask.start();
        writeTask.start();

        long loginTimeout = ConfigUtil.getLoginTimeout() * 1000;
        long connectStartTime = System.currentTimeMillis();

        // 读写线程可能由于读写 Socket 出错而将 stopTask 置为 true
        while (!stopTask) {
            try {
                // 这里对客户端登录状态进行检查，登录超时则断开连接
//                if (loginTimeout > 0 && !clientVerifyResult && System.currentTimeMillis() - connectStartTime > loginTimeout) {
//                    SocketUtil.writeMessage2Stream(
//                                    String.format("audioUtil client login timeout(%s seconds) !", ConfigUtil.getLoginTimeout()),
//                                    bufferWriter);
//                    Global.logger(String.format("audioUtil verify client timeout: %s s,will close socket..",
//                            ConfigUtil.getLoginTimeout()));
//                    break;
//                }

                if (clientVerifyResult) {
                    executeTimingTasks();
                }

                if (protocolQueue.isEmpty()) {
                    Thread.sleep(2000);
                    continue;
                }

                BaseProtocol protocol = protocolQueue.poll();

                // 当前收到的消息是注册成功的消息，那么服务器应该回复注册成功的消息给硬件
                iDataRecvEvent.RecvData(protocol);


                // 客户端验证通过后再执行其他业务
                if (!clientVerifyResult) {
                    continue;
                }

                heartbeat.setReceiveHeart(true);

            } catch (Exception e) {
                Global.logger("audioUtil server task error !!"+e.toString());
            }
        }

        close();
    }

    /**
     * 执行定时任务：心跳检测和名人名言推送
     */
    private void executeTimingTasks() throws IOException {
        if (System.currentTimeMillis() - lastHeartTime >= heartIntervalMillis) {
            // 做心跳检测
            if (heartbeat.isSendHeart()) {
                if (heartbeat.isReceiveHeart()) {
                    heartbeat = new Heartbeat();
                }

                heartbeat.setFailTimes(heartbeat.getFailTimes() + 1);
                if (heartbeat.getFailTimes() >= ConfigUtil.getHeartLimitTimes()) {
                    SocketUtil.writeMessage2Stream(
                            String.format("audioUtil 连续 %s个心跳包没有收到客户端的回复，服务端即将关闭连接！", heartbeat.getFailTimes()),
                            bufferWriter);
                    Global.logger(String.format("audioUtil do not get heartbeat reply from client %s times !",
                            heartbeat.getFailTimes()));
                    stopTask = true;
                }
            }

            writeTaskQueue.add(new WriteTask("Heartbeat", new ArrayList<>()));
            heartbeat.setSendHeart(true);
            lastHeartTime = System.currentTimeMillis();
        }

    }

    public Socket getSocket() {
        return this.socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    private void close() {
        try {
            // 延迟 5秒，让客户端读取 Socket 里剩余的消息
            Thread.sleep(5000);

            socket.close();
        } catch (Exception e) {
        }
        Global.logger("audioUtil socket is closed,client address: " + clientAddress);
    }

    public class ServerReadTask extends Thread {

        @Override
        public void run() {
            Global.logger("audioUtil start server read task,client address: " + clientAddress);

            try {
                while (!stopTask) {
                    BaseProtocol baseProtocol = SocketUtil.readMessageFromStream(bufferReader);

                    if (baseProtocol.getMessage() == null) {
                        Global.logger("audioUtil read socket message is null,maybe client closed socket..");
                        break;
                    }
                    Global.logger("audioUtil receive client message: " + baseProtocol.getMessage());
                    protocolQueue.offer(baseProtocol);
                }
            } catch (Exception e) {
                Global.logger("audioUtil server read task error !!"+e.toString());
            }

            stopTask = true;
        }

    }

    public class ServerWriteTask extends Thread {

        @Override
        public void run() {
            Global.logger("audioUtil start server write task,client address: " + clientAddress);

            try {
                while (!stopTask) {
                    if (writeTaskQueue.isEmpty()) {
                        Thread.sleep(2000);
                        continue;
                    }

                    WriteTask writeTask = writeTaskQueue.poll();
                    SocketUtil.writeMessageList2Stream(writeTask.getMessages(), bufferWriter);

                }
            } catch (SocketException se) {

            } catch (Exception e) {

            }

            stopTask = true;
        }

    }


}
