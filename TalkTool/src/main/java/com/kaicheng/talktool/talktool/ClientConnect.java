package com.kaicheng.talktool.talktool;

import java.net.Socket;

public class ClientConnect {

    public int status; //0=init;1=registed;
    public int postion;
    public String devip;
    public int devport;
    public String id;
    public Socket socket;

    @Override
    public String toString() {
        //return devip + ":" + devport.ToString() +"/" + id.ToString("X8");
        return devip + ":" + (new Integer(devport)) + "/" + id;
    }
}
