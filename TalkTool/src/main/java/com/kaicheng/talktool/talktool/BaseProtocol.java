package com.kaicheng.talktool.talktool;

import org.eclipse.paho.client.mqttv3.util.Strings;

public class BaseProtocol {


    protected String msgContent;
    private String message;

    public BaseProtocol() {

    }

    public BaseProtocol(String message) {
        this.message = message;
    }

    /**
     * 获取消息的类型
     *
     * @param message
     * @return
     */
    public static String getMessageType(String message) {
        if (!Strings.isEmpty(message)) {

            return message.split("\\|\\|")[0].trim();
        }

        return "";
    }

    public void analysisMessage(String message) {
        this.message = message;

        if (!Strings.isEmpty(message)) {
            String[] data = message.split("\\|\\|");
            if (data.length == 2) {
                msgContent = data[1];
            }
        }
    }

    public String getMessage() {
        return message;
    }

    public String getMsgContent() {
        return msgContent;
    }
}