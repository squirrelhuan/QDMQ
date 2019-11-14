package cn.demomaster.qdsocket;
import com.alibaba.fastjson.JSON;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


public class QDTcpClient {

    private static QDTcpClient instance;
    private String serverIP;
    private int serverPort;

    public static QDTcpClient getInstance() {
        if (instance == null) {
            instance = new QDTcpClient();
        }
        return instance;
    }

    private QDTcpClient() {
    }

    private String userName = "admin";
    private String passWord = "admin";

    public synchronized void connect(final String serverIP, final int serverPort) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!isServerClose(client)) {
                        throw new Exception("client已连接，请勿重新初始化");
                    }
                    client = new Socket(serverIP, serverPort);
                    System.out.println("socket连接成功");
                    client.setKeepAlive(true);//开启保持活动状态的套接字
                    client.setSoTimeout(5 * 60 * 1000);//设置超时时间
                    waitMessage();//开启消息接收
                    qdlogin();//用户登录
                } catch (IOException e) {
                    System.err.println("socket连接失败");
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    boolean flag = true;

    private void waitMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (flag) {
                    try {
                        InputStream is = null;
                        is = client.getInputStream();
                        BufferedReader br = new BufferedReader(new InputStreamReader(is));
                        //接收服务器的相应
                        String reply = null;
                        while (!((reply = br.readLine()) == null)) {
                            System.out.println("接收服务器的信息：" + reply);
                            QDMessage qdMessage = JSON.parseObject(reply, QDMessage.class);
                            if (qdMessage != null) {
                                isConnected = true;
                                if (receiveListenerMap.containsKey(qdMessage.getTime())) {
                                    OnMessageReceiveListener onMessageReceiveListener = receiveListenerMap.get(qdMessage.getTime());
                                    onMessageReceiveListener.onReceived((String) qdMessage.getMsg());
                                    receiveListenerMap.remove(qdMessage.getTime());
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private boolean isConnected;
    private void qdlogin() {
        QDMessage qdMessage = new QDMessage();
        QDUserInfo userInfo = new QDUserInfo();
        userInfo.setUserName(userName);
        userInfo.setPassWord(passWord);
        qdMessage.setData(userInfo);
        qdMessage.setTime(System.currentTimeMillis());
        send(qdMessage, new OnMessageReceiveListener() {
            @Override
            public void onReceived(String response) {
                System.out.println("收到登录回复" + response);
            }
        });
    }

    private Socket client;
    private OutputStream out;
    public static final char END_CHAR = '\n';

    /**
     * @param data
     */
    public synchronized void send(String data) {
        QDMessage qdMessage = new QDMessage();
        qdMessage.setData(data);
        qdMessage.setTime(System.currentTimeMillis());
        send(qdMessage, null);
    }

    Map<Long, OnMessageReceiveListener> receiveListenerMap = new HashMap<>();
    public synchronized void send(final QDMessage qdMessage, OnMessageReceiveListener listener) {
        if (listener != null) {
            receiveListenerMap.put(qdMessage.getTime(), listener);
        }
        //这里比较重要，需要给请求信息添加终止符，否则服务端会在解析数据时，一直等待
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String msg1 = JSON.toJSONString(qdMessage) + END_CHAR;
                    out = client.getOutputStream();
                    out.write(msg1.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    public static interface OnMessageReceiveListener {
        void onReceived(String response);
    }

    /**
     * 判断是否断开连接，断开返回true,没有返回false
     *
     * @param socket
     * @return
     */
    public Boolean isServerClose(Socket socket) {
        try {
            if (socket == null) return true;
            socket.sendUrgentData(0xFF);//发送1个字节的紧急数据，默认情况下，服务器端没有开启紧急数据处理，不影响正常通信
            return false;
        } catch (Exception se) {
            return true;
        }
    }

}
