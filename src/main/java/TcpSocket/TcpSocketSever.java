package TcpSocket;
import TcpSocket.bean.*;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.net.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author Mr.Independent(严良鹏 20H034160215)
 * @date 2022/6/5 - 23:35
 */
public class TcpSocketSever {
    private static Logger logger = LoggerUtil.getLogger(TcpSocketSever.class);
    //log.info();
    //log.error();
    //log.warn();
    //宁可多写也不要少写

    //固定参数
    private static Properties props=null;
    private static final String LOCAL_PATH = LoggerUtil.dir;//系统路径
    public static String SYSPROPFILE = LOCAL_PATH + "properties\\server.properties"; //系统配置文件位置
    public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    public static final String Server_VERSION="1.0.1 build-1";

    public static ServerSocket mServerSocket;
    //配置参数
    private static int PORT=6666;//默认端口号
    public static String CLIENT_VERSION="默认版本号";//默认版本号
    public static String txt_noticeBoard="默认公告文本";//默认公告文本
    //初始化配置参数
    static {
        initParams();
    }

    private static void initParams() {
        props = new Properties();
        Reader reader = null;
        try {
            logger.info("——————————————————————————————【TCP服务器参数配置信息】——————————————————————————————");
            logger.info("SYSPROPFILE:"+SYSPROPFILE);
            reader = new InputStreamReader(new FileInputStream(SYSPROPFILE), UTF8_CHARSET);
            props.load(reader);
            PORT = Integer.parseInt(props.getProperty("server.port", "6666"));
            CLIENT_VERSION=props.getProperty("server.clientVersion","默认版本号");
            txt_noticeBoard=props.getProperty("server.txtNoticeBoard","默认公告文本");
            //日志打印配置信息
            logger.info("PORT:"+PORT);
            logger.info("CLIENT_VERSION:"+CLIENT_VERSION);
            logger.info("txt_noticeBoard:"+txt_noticeBoard);
        } catch (Exception e) {
            logger.info("系统参数配置异常，采用默认配置：" + e.getMessage(), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        logger.info("——————————————————————————————（TCP服务器配置结束）——————————————————————————————");
    }

    public static void main(String[] args) {
        UDPSever.startUDPServer();
        startTCPServer();
    }

    private static void startTCPServer() {
        try {
            //连接数据库
            if(MySQLDBUtil.connect()){
                logger.info("connected to MySQL Databases successfully");
                //数据库读取块
                Card.updateCards();
            }else{
                logger.info("failed to connect MySQL Databases");
            }
            logger.info("TCP Sever start...");
            //建立套接字
            mServerSocket=new ServerSocket(PORT);
            logger.info("TCP start listening...");
            //启动监听线程
            new Thread(new Server_listening()).start();
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("failed to start TCP server");
            try {
                mServerSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }


    /**
     * 广播消息
     * @param msg
     */
    public static void broadcastMsg(String msg){
        JSONObject object=new JSONObject();
        object.put("type","broadcast");
        object.put("msg",msg);
//        socketMap.forEach((key,cSocket)->{
//            new Thread(new Server_send(cSocket,object)).start();
//        });
    }

    public static void noticeBoard(User user,String txt){
        JSONObject object=new JSONObject();
        object.put("type","noticeboard");
        object.put("txt",txt);
        object.put("version_number",CLIENT_VERSION);
        user.sendPacket(object);
    }

    public static void sendVersion(User user){
        JSONObject object=new JSONObject();
        object.put("type","version");
        object.put("version_number",CLIENT_VERSION);
        user.sendPacket(object);
    }

    /**
     * 用户离线处理
     * @param cSocket
     */
    public static void close(Socket cSocket){
        //离线必须调用
        User user =User.getUser(cSocket);
        if(user!=null){
            user.close();
        }
    }

    static class Server_listening implements Runnable{
        @Override
        public void run() {
            while (true){
                Socket cSocket=null;
                try{
                    cSocket=mServerSocket.accept();

                    User user =new User(cSocket);
                    logger.info(user.getUID()+" has linked");
                    //对用户开启监听
                    user.start();
                    //对每个接收到的用户发送公告栏文本
                    noticeBoard(user,TcpSocketSever.txt_noticeBoard);
                }catch (Exception e){
                    e.printStackTrace();
                    if(cSocket!=null) {
                        try {
                            close(cSocket);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }

//            以下为备份
//        //关闭IO流，Socket
//        try {
//            mServerSocket.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }finally {
//            System.out.printf("[%s]%s\n","TCP","Server socket has closed");
//        }
//
//        //断开数据库
//        MySQLDBUtil.disConnect();

        }
    }

}




