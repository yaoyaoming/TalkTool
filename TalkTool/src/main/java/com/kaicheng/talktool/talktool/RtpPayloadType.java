package com.kaicheng.talktool.talktool;


public enum RtpPayloadType {

    G711_uLaw(0),
    /// <summary>
    /// G721
    /// </summary>
    G721(2),

    G711_aLaw(8),
    /// <summary>
    /// G722
    /// </summary>
    G722(9),
    /// <summary>
    /// G728
    /// </summary>
    G728(15),
    /// <summary>
    /// G729
    /// </summary>
    G729(18),
    /// <summary>
    /// JPEG
    /// </summary>
    JPEG(26),
    /// <summary>
    /// H261
    /// </summary>
    H261(31),
    /// <summary>
    /// H263
    /// </summary>
    H263(34);

    int id;

    RtpPayloadType(int i) {
        id = i;
    }

    public static RtpPayloadType GetValue(int _id) {
        RtpPayloadType[] As = RtpPayloadType.values();
        for (int i = 0; i < As.length; i++) {
            if (As[i].Compare(_id))
                return As[i];
        }
        return RtpPayloadType.G711_uLaw;
    }

    public int GetId() {
        return id;
    }

    public boolean IsEmpty() {
        return this.equals(RtpPayloadType.G711_uLaw);
    }

    public boolean Compare(int i) {
        return id == i;
    }
}
