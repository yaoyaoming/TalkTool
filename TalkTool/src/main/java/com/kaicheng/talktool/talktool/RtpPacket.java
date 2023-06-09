package com.kaicheng.talktool.talktool;


import android.media.Image;

/**
 * RTP(RFC3550)协议数据包
 * <p>
 * <p>
 * The RTP header has the following format:
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |V=2|P|X| CC    |M| PT          | sequence number               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | timestamp                                                     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | synchronization source (SSRC) identifier                      |
 * +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
 * | contributing source (CSRC) identifiers                        |
 * | ....                                                          |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class RtpPacket {

    /**
     * 每一个RTP包中都有前12个字节定长的头字段
     * The first twelve octets are present in every RTP packet
     */
    public static final int HeaderSize = 12;
    /**
     * payload type (PT)：7 bits
     * 这个字段定一个RTPpayload 的格式和在应用中定义解释。
     * profile 可能指定一个从payload type 码字到payload format 的默认静态映射。
     * 也可以通过non-RTP 方法来定义附加的payload type 码字(见第3 章)。
     * 在 RFC 3551[1]中定义了一系列的默认音视频映射。
     * 一个RTP 源有可能在会话中改变payload type，但是这个域在复用独立的媒体时是不同的。(见5.2 节)。
     * 接收者必须忽略它不识别的payload type。
     * This field identifies the format of the RTP payload and determines its interpretation by the
     * application. A profile may specify a default static mapping of payload type codes to payload
     * formats. Additional payload type codes may be defined dynamically through non-RTP means
     * (see Section 3). A set of default mappings for audio and video is specified in the companion
     * RFC 3551 [1]. An RTP source may change the payload type during a session, but this field
     * should not be used for multiplexing separate media streams (see Section 5.2).
     * A receiver must ignore packets with payload types that it does not understand.
     */
    private RtpPayloadType privatePayloadType = RtpPayloadType.G711_uLaw;
    /**
     * sequence number：16 bits
     * 每发送一个RTP 数据报文序列号值加一，接收者也可用来检测丢失的包或者重建报文序列。
     * 初始的值是随机的，这样就使得known-plaintext 攻击更加困难， 即使源并没有加密(见9。1)，
     * 因为要通过的translator 会做这些事情。关于选择随机数方面的技术见[17]。
     * The sequence number increments by one for each RTP data packet sent, and may be used
     * by the receiver to detect packet loss and to restore packet sequence. The initial value of the
     * sequence number should be random (unpredictable) to make known-plaintext attacks on
     * encryption more dificult, even if the source itself does not encrypt according to the method
     * in Section 9.1, because the packets may flow through a translator that does. Techniques for
     * choosing unpredictable numbers are discussed in [17].
     */
    private int privateSequenceNumber;
    /**
     * timestamp：32 bits
     * timestamp 反映的是RTP 数据报文中的第一个字段的采样时刻的时间瞬时值。
     * 采样时间值必须是从恒定的和线性的时间中得到以便于同步和jitter 计算(见第6.4.1 节)。
     * 必须保证同步和测量保温jitter 到来所需要的时间精度(一帧一个tick 一般情况下是不够的)。
     * 时钟频率是与payload 所携带的数据格式有关的，在profile 中静态的定义或是在定义格式的payload format 中，
     * 或通过non-RTP 方法所定义的payload format 中动态的定义。如果RTP 报文周期的生成，就采用虚拟的(nominal)
     * 采样时钟而不是从系统时钟读数。例如，在固定比特率的音频中，timestamp 时钟会在每个采样周期时加一。
     * 如果音频应用中从输入设备中读入160 个采样周期的块，the timestamp 就会每一块增加160，而不管块是否传输了或是丢弃了。
     * 对于序列号来说，timestamp 初始值是随机的。只要它们是同时(逻辑上)同时生成的，这些连续的的 RTP 报文就会有相同的timestamp，
     * 例如，同属一个视频帧。正像在MPEG 中内插视频帧一样，连续的但不是按顺序发送的RTP 报文可能含有相同的timestamp。
     * The timestamp reflects the sampling instant of the first octet in the RTP data packet. The
     * sampling instant must be derived from a clock that increments monotonically and linearly
     * in time to allow synchronization and jitter calculations (see Section 6.4.1). The resolution
     * of the clock must be suficient for the desired synchronization accuracy and for measuring
     * packet arrival jitter (one tick per video frame is typically not suficient). The clock frequency
     * is dependent on the format of data carried as payload and is specified statically in the profile
     * or payload format specification that defines the format, or may be specified dynamically for
     * payload formats defined through non-RTP means. If RTP packets are generated periodically,
     * the nominal sampling instant as determined from the sampling clock is to be used, not a
     * reading of the system clock. As an example, for fixed-rate audio the timestamp clock would
     * likely increment by one for each sampling period. If an audio application reads blocks covering
     * 160 sampling periods from the input device, the timestamp would be increased by 160 for
     * each such block, regardless of whether the block is transmitted in a packet or dropped as silent.
     */
    private long privateTimestamp;
    /**
     * RTP消息头
     */
    private byte[] _header;
    /**
     * RTP有效载荷长度
     */
    private int _payloadSize;
    /**
     * RTP有效载荷
     */
    private byte[] _payload;

    /**
     * RTP(RFC3550)协议数据包
     *
     * @param playloadType   数据报文有效载荷类型
     * @param sequenceNumber 数据报文序列号值
     * @param timestamp      数据报文采样时刻
     * @param data           数据
     * @param dataSize       数据长度
     */
    public RtpPacket(RtpPayloadType playloadType, int sequenceNumber, long timestamp, byte[] data, int dataSize) {
        // fill changing header fields
        setSequenceNumber(sequenceNumber);
        setTimestamp(timestamp);
        setPayloadType(playloadType);

        // build the header bistream
        _header = new byte[HeaderSize];

        // fill the header array of byte with RTP header fields
        _header[0] = (byte) ((getVersion() << 6) | (getPadding() << 5) | (getExtension() << 4) | getCC());
        _header[1] = (byte) ((getMarker() << 7) | getPayloadType().GetId());
        _header[2] = (byte) (getSequenceNumber() >> 8);
        _header[3] = (byte) (getSequenceNumber());
        for (int i = 0; i < 4; i++) {
            _header[7 - i] = (byte) (getTimestamp() >> (8 * i));
        }
        for (int i = 0; i < 4; i++) {
            _header[11 - i] = (byte) (getSSRC() >> (8 * i));
        }

        // fill the payload bitstream
        _payload = new byte[dataSize];
        _payloadSize = dataSize;

        // fill payload array of byte from data (given in parameter of the constructor)
        System.arraycopy(data, 0, _payload, 0, dataSize);
    }

    /**
     * RTP(RFC3550)协议数据包
     *
     * @param playloadType   数据报文有效载荷类型
     * @param sequenceNumber 数据报文序列号值
     * @param timestamp      数据报文采样时刻
     * @param frame          图片
     */
    public RtpPacket(RtpPayloadType playloadType, int sequenceNumber, long timestamp, Image frame) {
        // fill changing header fields
        setSequenceNumber(sequenceNumber);
        setTimestamp(timestamp);
        setPayloadType(playloadType);

        // build the header bistream
        _header = new byte[HeaderSize];

        // fill the header array of byte with RTP header fields
        _header[0] = (byte) ((getVersion() << 6) | (getPadding() << 5) | (getExtension() << 4) | getCC());
        _header[1] = (byte) ((getMarker() << 7) | getPayloadType().ordinal());
        _header[2] = (byte) (getSequenceNumber() >> 8);
        _header[3] = (byte) (getSequenceNumber());
        for (int i = 0; i < 4; i++) {
            _header[7 - i] = (byte) (getTimestamp() >> (8 * i));
        }
        for (int i = 0; i < 4; i++) {
            _header[11 - i] = (byte) (getSSRC() >> (8 * i));
        }

        // fill the payload bitstream
//C# TO JAVA CONVERTER NOTE: The following 'using' block is replaced by its Java equivalent:
//	  using (MemoryStream ms = new MemoryStream())
//        MemoryStream ms = new MemoryStream();
//        try
//        {
//            frame.Save(ms, ImageFormat.Jpeg);
//            _payload = ms.toArray();
//            _payloadSize = _payload.length;
//        }
//        finally
//        {
//            ms.dispose();
//        }
    }

    /**
     * RTP(RFC3550)协议数据包
     *
     * @param packet     数据包
     * @param packetSize 数据包长度
     */
    public RtpPacket(byte[] packet, int packetSize) {
        //check if total packet size is lower than the header size
        if (packetSize >= HeaderSize) {
            //get the header bitsream
            _header = new byte[HeaderSize];
            for (int i = 0; i < HeaderSize; i++) {
                _header[i] = packet[i];
            }

            //get the payload bitstream
            _payloadSize = packetSize - HeaderSize;
            _payload = new byte[_payloadSize];
            for (int i = HeaderSize; i < packetSize; i++) {
                _payload[i - HeaderSize] = packet[i];
            }

            //interpret the changing fields of the header
            setPayloadType(RtpPayloadType.GetValue(_header[1] & 127));
            setSequenceNumber(UnsignedInt(_header[3]) + 256 * UnsignedInt(_header[2]));
            setTimestamp(UnsignedInt(_header[7]) + 256 * UnsignedInt(_header[6]) + 65536 * UnsignedInt(_header[5]) + 16777216 * UnsignedInt(_header[4]));
        }
    }

    /**
     * 将图片转换成消息
     *
     * @param playloadType   数据报文有效载荷类型
     * @param sequenceNumber 数据报文序列号值
     * @param timestamp      数据报文采样时刻
     * @param frame          图片帧
     * @return RTP消息
     */
    public static RtpPacket FromImage(RtpPayloadType playloadType, int sequenceNumber, long timestamp, Image frame) {
        return new RtpPacket(playloadType, sequenceNumber, timestamp, frame);
    }

    /**
     * return the unsigned value of 8-bit integer nb
     *
     * @param nb
     * @return
     */
    private static int UnsignedInt(int nb) {
        if (nb >= 0) {
            return (nb);
        } else {
            return (256 + nb);
        }
    }

    /**
     * version (V): 2 bits
     * RTP版本标识，当前规范定义值为2.
     * This field identifies the version of RTP. The version defined by this specification is two (2).
     * (The value 1 is used by the first draft version of RTP and the value 0 is used by the protocol
     * initially implemented in the \vat" audio tool.)
     */
    public final int getVersion() {
        return 2;
    }

    /**
     * padding (P)：1 bit
     * 如果设定padding，在报文的末端就会包含一个或者多个padding 字节，这不属于payload。
     * 最后一个字节的padding 有一个计数器，标识需要忽略多少个padding 字节(包括自己)。
     * 一些加密算法可能需要固定块长度的padding，或者是为了在更低层数据单元中携带一些RTP 报文。
     * If the padding bit is set, the packet contains one or more additional padding octets at the
     * end which are not part of the payload. The last octet of the padding contains a count of
     * how many padding octets should be ignored, including itself. Padding may be needed by
     * some encryption algorithms with fixed block sizes or for carrying several RTP packets in a
     * lower-layer protocol data unit.
     */
    public final int getPadding() {
        return 0;
    }

    /**
     * extension (X)：1 bit
     * 如果设定了extension 位，定长头字段后面会有一个头扩展。
     * If the extension bit is set, the fixed header must be followed by exactly one header extensio.
     */
    public final int getExtension() {
        return 0;
    }

    /**
     * CSRC count (CC)：4 bits
     * CSRC count 标识了定长头字段中包含的CSRC identifier 的数量。
     * The CSRC count contains the number of CSRC identifiers that follow the fixed header.
     */
    public final int getCC() {
        return 0;
    }

    /**
     * marker (M)：1 bit
     * marker 是由一个profile 定义的。用来允许标识在像报文流中界定帧界等的事件。
     * 一个profile 可能定义了附加的标识位或者通过修改payload type 域中的位数量来指定没有标识位.
     * The interpretation of the marker is defined by a profile. It is intended to allow significant
     * events such as frame boundaries to be marked in the packet stream. A profile may define
     * additional marker bits or specify that there is no marker bit by changing the number of bits
     * in the payload type field.
     */
    public final int getMarker() {
        return 0;
    }

    public final RtpPayloadType getPayloadType() {
        return privatePayloadType;
    }

    private void setPayloadType(RtpPayloadType value) {
        privatePayloadType = value;
    }

    public final int getSequenceNumber() {
        return privateSequenceNumber;
    }

    private void setSequenceNumber(int value) {
        privateSequenceNumber = value;
    }

    public final long getTimestamp() {
        return privateTimestamp;
    }

    private void setTimestamp(long value) {
        privateTimestamp = value;
    }

    /**
     * SSRC：32 bits
     * SSRC 域识别同步源。为了防止在一个会话中有相同的同步源有相同的SSRC identifier， 这个identifier 必须随机选取。
     * 生成随机 identifier 的算法见目录A.6 。虽然选择相同的identifier 概率很小，但是所有的RTP implementation 必须检测和解决冲突。
     * 第8 章描述了冲突的概率和解决机制和RTP 级的检测机制，根据唯一的 SSRCidentifier 前向循环。如果有源改变了它的源传输地址，
     * 就必须为它选择一个新的SSRCidentifier 来避免被识别为循环过的源(见第8.2 节)。
     * The SSRC field identifies the synchronization source. This identifier should be chosen
     * randomly, with the intent that no two synchronization sources within the same RTP session
     * will have the same SSRC identifier. An example algorithm for generating a random identifier
     * is presented in Appendix A.6. Although the probability of multiple sources choosing the same
     * identifier is low, all RTP implementations must be prepared to detect and resolve collisions.
     * Section 8 describes the probability of collision along with a mechanism for resolving collisions
     * and detecting RTP-level forwarding loops based on the uniqueness of the SSRC identifier. If
     * a source changes its source transport address, it must also choose a new SSRC identifier to
     * avoid being interpreted as a looped source (see Section 8.2).
     */
    public final int getSSRC() {
        return 0;
    }

    /**
     * RTP消息头
     */
    public final byte[] getHeader() {
        return _header;
    }

    /**
     * RTP有效载荷长度
     */
    public final int getPayloadSize() {
        return _payloadSize;
    }

    /**
     * RTP有效载荷
     */
    public final byte[] getPayload() {
        return _payload;
    }

//    /**
//     将消息体转换成图片
//
//     @return 图片
//     */
//    public final Bitmap ToBitmap()
//    {
//        return new Bitmap(new MemoryStream(_payload));
//    }
//
//    /**
//     将消息体转换成图片
//
//     @return 图片
//     */
//    public final Image ToImage()
//    {
//        return Image.FromStream(new MemoryStream(_payload));
//    }

    /**
     * RTP消息总长度，包括Header和Payload
     */
    public final int getLength() {
        return HeaderSize + getPayloadSize();
    }

    /**
     * 将消息转换成byte数组
     *
     * @return 消息byte数组
     */
    public final byte[] toArray() {
        byte[] packet = new byte[getLength()];

        System.arraycopy(_header, 0, packet, 0, HeaderSize);
        System.arraycopy(_payload, 0, packet, HeaderSize, getPayloadSize());

        return packet;
    }
}