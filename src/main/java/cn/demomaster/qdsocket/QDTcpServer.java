package cn.demomaster.qdsocket;

import com.alibaba.fastjson.JSON;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QDTcpServer {

    public static final String SERVICE_IP = "127.0.0.1";
    public static final int SERVICE_PORT = 10101;
    private String serverIP = SERVICE_IP;
    private int serverPort = SERVICE_PORT;
    private ServerSocket server;
    Map<Long, Socket> socketMap = new HashMap<>();
    private static QDTcpServer instance;

    public static QDTcpServer getInstance() {
        if (instance == null) {
            instance = new QDTcpServer();
        }
        return instance;
    }

    private QDTcpServer() {

    }

    /**
     * 创建socket server
     * @param serverIP
     * @param serverPort
     */
    public void creat(String serverIP, int serverPort) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        try {
            if (server == null) {
                //封装服务端地址
                InetAddress serverAddress = InetAddress.getByName(serverIP);
                server = new ServerSocket(serverPort, 10, serverAddress);
                System.out.println("QDTcpServer 初始化成功");
            }else {
                throw new Exception("服务已经创建，请勿重复创建");
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() {
        startService();
    }

    private void startService() {
        try {
            if(server==null||server.isClosed()){
                throw new Exception("服务未创建");
            }
            while (true) {
                System.out.println("QDTcpServer 等待连接");
                // 阻塞式的等待连接
                Socket client = server.accept();
                addClient(client);
            }
            //server.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    int i = 0;
    private String userName = "admin";
    private String passWord = "admin";

    private void addClient(Socket client) {
        System.out.println("客户端" + i + "连接");
        //3.获得输入流
        InputStream is = null;
        try {
            is = client.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            //4.读取用户输入信息
            String info = null;
            while (!((info = br.readLine()) == null)) {
                StringBuffer stringBuffer = new StringBuffer();
                stringBuffer.append(info);

                QDMessage qdMessage = JSON.parseObject(info, QDMessage.class);

                if (!socketMap.containsValue(client)) {
                    System.out.println("用户信息为：" + info);

                    QDUserInfo userInfo;
                    try {
                        userInfo = JSON.parseObject(qdMessage.getData().toString(), QDUserInfo.class);
                    } catch (Exception e) {
                        System.err.println("用户登录失败,参数有误");
                        replyLogin(client, false, qdMessage.getTime());
                        return;
                    }
                    if (userName != null || passWord != null) {
                        long clentId = System.currentTimeMillis() + i;
                        socketMap.put(clentId, client);
                        System.out.println("连接登录成功");
                        replyLogin(client, true, qdMessage.getTime());
                    } else {
                        if (userInfo != null && userInfo.getUserName() != null && userInfo.getPassWord() != null) {
                            if (userInfo.getUserName().equals(userName) && userInfo.getPassWord().equals(passWord)) {
                                long clentId = System.currentTimeMillis() + i;
                                socketMap.put(clentId, client);
                                System.out.println("连接登录成功");
                                replyLogin(client, true, qdMessage.getTime());
                            } else {
                                System.err.println("用户登录失败");
                                replyLogin(client, false, qdMessage.getTime());
                                return;
                            }
                        } else {
                            System.err.println("用户登录失败");
                            replyLogin(client, false, qdMessage.getTime());
                            return;
                        }
                    }
                } else {
                    if (info == null || info.trim().equals("")) {
                        //过滤
                    } else {
                        System.out.println("用户消息：" + info);
                        if (onReceiveMessageListener != null) {
                            onReceiveMessageListener.onReceiveMessage(getSocketId(client), qdMessage);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private long getSocketId(Socket client) {
        for (Map.Entry entry : socketMap.entrySet()) {
            if (entry.getValue() == client) {
                return (long) entry.getKey();
            }
        }
        return -1;
    }

    OnReceiveMessageListener onReceiveMessageListener;

    public void setOnReceiveMessageListener(OnReceiveMessageListener onReceiveMessageListener) {
        this.onReceiveMessageListener = onReceiveMessageListener;
    }

    public static interface OnReceiveMessageListener {
        void onReceiveMessage(long clientId, QDMessage qdMessage);
    }

    public static final char END_CHAR = '\n';

    private void replyLogin(Socket client, boolean isSuccess, long time) {
        if(isSuccess){
            if(i>65536){
                i = 0;
            }
            i++;
        }
        //获得输出流
        OutputStream os = null;
        try {
            os = client.getOutputStream();
            //给客户一个响应
            QDMessage qdMessage = new QDMessage();
            qdMessage.setStatus(isSuccess ? 1 : 0);
            qdMessage.setTime(time);
            qdMessage.setMsg(isSuccess ? "login success" : "login fail");
            String reply = JSON.toJSONString(qdMessage) + END_CHAR;
            os.write(reply.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(long clientId, long time, String msg) {
        OutputStream os = null;
        try {
            os = getSocketById(clientId).getOutputStream();
            //给客户一个响应
            QDMessage qdMessage = new QDMessage();
            qdMessage.setTime(time);
            qdMessage.setMsg(msg);
            String reply = JSON.toJSONString(qdMessage) + END_CHAR;
            os.write(reply.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Socket getSocketById(long clientId) {
        return socketMap.get(clientId);
    }

    public synchronized void checkClient() {
        List<Long> socketIds = new ArrayList<>();
        for (Map.Entry entry : socketMap.entrySet()) {
            boolean b = isServerClose((Socket) entry.getValue());
            if (b) {
                System.out.println(entry.getKey() + "离线");
                socketIds.add((Long) entry.getKey());
            }
        }
        for (Long id : socketIds) {
            socketMap.remove(id);
        }
    }

    /**
     * 判断是否断开连接，断开返回true,没有返回false
     *
     * @param socket
     * @return
     */
    public Boolean isServerClose(Socket socket) {
        try {
            socket.sendUrgentData(0xFF);//发送1个字节的紧急数据，默认情况下，服务器端没有开启紧急数据处理，不影响正常通信
            return false;
        } catch (Exception se) {
            return true;
        }
    }

}
