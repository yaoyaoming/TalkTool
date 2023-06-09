package com.kaicheng.talktool.talktool;


import java.util.Properties;

public class ConfigUtil {


    private static Properties prop = new Properties();

    public static int getServerPort() {

        return 8877;
    }
    public static String getServerHost() {
        try {
            return prop.getProperty("server.host");
        } catch (Exception e) {
        }

        return "127.0.0.1";
    }



    public static int getHeartbeatInterval() {

        return 30;
    }

    public static int getHeartLimitTimes() {

        return 2;
    }

    public static int getLoginTimeout() {

        return -1;
    }

}