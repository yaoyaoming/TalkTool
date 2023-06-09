package com.kaicheng.talktool.talktool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerTaskCache {

    public static Map<String, ServerTask> clientAddress2ServerTask = new ConcurrentHashMap<>();

}