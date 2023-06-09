package com.kaicheng.talktool.talktool;


import android.util.Log;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkServer {

    public ServerTask serverTask;


    public String devip;
    public int devport;
    public Socket socket;

    public NetworkServer() {
    }

    private static final String TAG = "NetworkServer";
    /**
     * 启动服务端
     */
    public void init() {
        serverTask = new ServerTask();
        ServerThread readTask = new ServerThread();
        readTask.start();
        Log.i(TAG, "**audioUtil***socket server is start ..");
    }

    public class ServerThread extends Thread {

        @Override
        public void run() {

            ExecutorService executorService = Executors.newCachedThreadPool();
            int connectionCount = 0;//连接客户端计数，没啥大用，统计客户端数量
            try (ServerSocket serverSocket = new ServerSocket(ConfigUtil.getServerPort())) {
                while (true) {
                    Log.i(TAG, "**audioUtil***audioUtil ServerThread socket server listen on port: " + serverSocket.getLocalPort());


                    socket = serverSocket.accept ();
                    ++connectionCount;

                    String clientAddress = socket.getInetAddress().getHostAddress();
                    Log.i(TAG, "**audioUtil ServerThread receive a connect request from " + socket.getRemoteSocketAddress());
                    //将clientConnect填充
                    devip = clientAddress;
                    devport = socket.getPort();

                    serverTask.setSocket(socket);
                    //加入到线程池中
                    executorService.submit(serverTask);

                    ServerTaskCache.clientAddress2ServerTask.put(clientAddress, serverTask);
                    Log.i(TAG, "**audioUtil ServerThread receive client connection count: " + connectionCount);
                }
            } catch (Exception e) {
                Log.i(TAG, "**audioUtil ServerThread socket server error !!"+ e.toString());
            }
        }

    }

}

