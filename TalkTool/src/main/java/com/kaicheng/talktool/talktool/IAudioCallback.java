package com.kaicheng.talktool.talktool;


public interface IAudioCallback {
    /**
     * 音频数据回调
     *
     * @param data
     */
    void onAudioData(byte[] data);
}
