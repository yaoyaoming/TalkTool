package com.kaicheng.talktool.talktool;

public class CommandRegister {
    public String command;
    public String id;
    public String authentication;
    public String session;
    public String auth_dir;
    public int result;
    public int systick;//状态上传频率，用于检测音频板是否在线
    public String mode;//设备模式
}

