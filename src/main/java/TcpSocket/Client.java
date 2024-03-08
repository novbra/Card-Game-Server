package TcpSocket;

import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

/**
 * @author Mr.Independent(严良鹏 20H034160215)
 * @date 2022/6/6 - 0:35
 */

/*
一般客户端 都没有公网ip，所以服务端不主动与客户端重连，而是客户端主动要求重连
 */
public class Client {
    public static final String CLIENT_VERSION="1.0.0 build-5";
    public static final String IP="124.223.75.186";
//    public static final String IP="127.0.0.1";
    private static final int PORT=6666;
    private static Socket mTcpSocket;
    public static boolean login_sate=false;
    public static boolean connection_state=false;
    private static ObjectOutputStream oos;
    private static ObjectInputStream ois;

    public static int uid;//账户
    public static String name;//玩家名称


    public static void setUid(int uid) {
        Client.uid = uid;
    }

    public static void main(String[] args) {
        System.out.printf("%-64s","LIST OF ROOMS\n");
        System.out.printf("%-16s","房间号");
        System.out.printf("%-16s","状态");
        System.out.printf("%-16s","是否为满");
        System.out.printf("%-16s","玩家");



        while(!connection_state){
            if(!connect()){
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        UDPClient.init();
        test();

    }
    private static void test(){
        Scanner scanner=new Scanner(System.in);
        while(true){
            System.out.println("请输入命令 /help 获取所有指令");
            String cmd=scanner.nextLine();
            switch (cmd){
                case "/help":
                    help();
                    break;
                case "/reconnect":
                    System.out.println("偷懒未实现");
                    break;
                case "/register":
                    System.out.println("phone number:");
                    String phoneNumber_register=scanner.nextLine();
                    System.out.println("password:");
                    String password_register=scanner.nextLine();
                    register(phoneNumber_register,password_register);
                    break;
                case "/login":
                    System.out.println("phone number:");
                    String phoneNumber=scanner.nextLine();
                    System.out.println("password:");
                    String password=scanner.nextLine();
                    login(phoneNumber,password);
                    break;
                case "/setName":
                    if(!login_sate){
                        System.out.println("此操作需要登录");
                        continue;
                    }
                    UDPClient.setName();
                    break;
                case "/rank":
                    if(!login_sate){
                        System.out.println("此操作需要登录");
                        continue;
                    }
                    UDPClient.getRank();
                    break;
                case "/all":
                    if(!login_sate){
                        System.out.println("此操作需要登录");
                        continue;
                    }
                    UDPClient.msgAll();
                    break;
                case "/msgRoom":
                    if(!login_sate){
                        System.out.println("此操作需要登录");
                        continue;
                    }
                    UDPClient.msgRoom();
                    break;
                case "/msg":
                    if(!login_sate){
                        System.out.println("此操作需要登录");
                        continue;
                    }
                    UDPClient.msg();
                    break;
                case "/new":
                case "/new room":
                    if(!login_sate){
                        System.out.println("此操作需要登录");
                        continue;
                    }
                    UDPClient.newRoom();
                    break;
                case "/show":
                case "/showAllRooms":
                    if(!login_sate){
                        System.out.println("此操作需要登录");
                        continue;
                    }
                    UDPClient.showAllRooms();
                    break;
                case "/enter":
                case "/enter room":
                    if(!login_sate){
                        System.out.println("此操作需要登录");
                        continue;
                    }
                    UDPClient.enterRoom();
                    break;
                case "/drop":
                case "/exit from room":
                    if(!login_sate){
                        System.out.println("此操作需要登录");
                        continue;
                    }
                    UDPClient.exitRoom();
                    break;
                case "/switch":
                case "/switch player state":
                    if(!login_sate){
                        System.out.println("此操作需要登录");
                        continue;
                    }
                    UDPClient.switchPlayerState();
                    break;
                case "/start":
                    if(!login_sate){
                        System.out.println("此操作需要登录");
                        continue;
                    }
                    break;
                case "/cast":
                    if(!login_sate){
                        System.out.println("此操作需要登录");
                        continue;
                    }
                    UDPClient.cast();
                    break;
                case "/finish":
                    if(!login_sate){
                        System.out.println("此操作需要登录");
                        continue;
                    }
                    UDPClient.finishTurn();
                    break;

            }

        }
    }

    private static void login(String phoneNumber,String password) {
        new Thread(new Client_login(mTcpSocket,oos,phoneNumber,password)).start();
    }

    private static void register(String phoneNumber,String password) {
        new Thread(new Client_register(mTcpSocket,oos,phoneNumber,password)).start();
    }

    private static void help(){
        System.out.println("/help:               提供全部命令");
        System.out.println("/reconnect:          重连(未实现)");
        System.out.println("/register:           注册");
        System.out.println("/login:              登录");
        System.out.println("/setName:            设置名称");
        System.out.println("/all:                大喇叭发送消息给全服玩家");
        System.out.println("/msgRoom:            房内聊天");
        System.out.println("/msg:                私聊");
        System.out.println("/rank:               查询rank分");
        System.out.println("/show:               显示所有房间");
        System.out.println("/showAllRooms:       显示所有房间");
        System.out.println("/new:                创建房间");
        System.out.println("/new room:           创建房间");
        System.out.println("/enter:              进入房间");
        System.out.println("/enter room:         进入房间");
        System.out.println("/drop:               离开当前房间");
        System.out.println("/exit from room:     离开当前房间");
        System.out.println("/switch:             改变玩家状态(准备就绪,未准备就绪)");
        System.out.println("/switch player state:改变玩家状态(准备就绪,未准备就绪)");
        System.out.println("/start:              房主准备开始(本功能仅供内部测试)");
        System.out.println("/cast:               打牌(本功能已开放)");
    }

    private static boolean connect() {
        try{
            mTcpSocket=new Socket(IP,PORT);
            connection_state=true;
            oos=new ObjectOutputStream(mTcpSocket.getOutputStream());
            ois=new ObjectInputStream(mTcpSocket.getInputStream());

            new Thread(new Client_listen(mTcpSocket,ois)).start();
//            new Thread(new Client_send(mSocket,oos)).start();
            //心跳包
            new Thread(new Client_heart(mTcpSocket,oos)).start();
            return true;
        }
        catch (Exception e){
            e.printStackTrace();
            connection_state=false;
            return false;
        }
    }
    public static void reconnect(){
        while(!connection_state){
            System.out.println("尝试重新连接");
            connect();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取版本号
     */
    private static void getVersion(){
        JSONObject object=new JSONObject();
        object.put("type","version");
        new Thread(new Client_send(mTcpSocket,oos,object)).start();
    }

    /**
     * 私人消息发送
     */
    private static void sendPersonalMsg(String to_uid,String msg){
        JSONObject object=new JSONObject();
        object.put("type","personal_msg");
        object.put("to_uid",to_uid);
        object.put("msg",msg);
        //预留两个参数
        object.put("arg0","");
        object.put("arg1","");
        new Thread(new Client_send(mTcpSocket,oos,object)).start();
    }
}

class Client_listen implements Runnable{
    private Socket mSocket;//想想为什么这里要写
    private ObjectInputStream ois;

    public Client_listen(Socket mSocket, ObjectInputStream ois) {
        this.mSocket = mSocket;
        this.ois = ois;
    }


    @Override
    public void run() {
        try {
            while(true){
                decodePackage((JSONObject) ois.readObject());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                mSocket.close();
                Client.login_sate=false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void decodePackage(JSONObject recvObject){
        JSONObject object=new JSONObject();//待发送的JSONObj
        String type= (String) recvObject.get("type");
        switch (type){
            case "noticeboard":
                String versionNumber= (String) recvObject.get("version_number");
                System.out.printf("Local version:%s | Latest version:%s\n",Client.CLIENT_VERSION,versionNumber);
                String txt_noticeboard= (String) recvObject.get("txt");
                System.out.printf("**********NOTICE BOARD**********\n%s\n",txt_noticeboard);
                //弹出dialog，并将公告栏文本显示在上
                System.out.printf("********************************\n");
                break;
            case "version":
//                String versionNumber= (String) recvObject.get("version_number");
//                System.out.printf("Last version:%s\n",versionNumber);
                //自动更新判断
                break;
            case "broadcast":
                String msg= (String) object.get("msg");
                //弹出dialog,并将广播消息显示在上
                break;
            case "register":
                if(recvObject.get("result").equals("successful")){
                    //注册成功
                    System.out.println("注册成功");
                }else{
                    //注册失败
                    String wrong= (String) recvObject.get("wrong");
                    System.out.println("注册失败|"+wrong);
                }
                break;
            case "login":
                String result= (String) recvObject.get("result");

                if(result.equals("successful")){
                    //登录成功
                    System.out.println("登录成功");
                    //设置登录状态
                    Client.login_sate=true;
                    //设置uid
                    int uid=(Integer) recvObject.get("uid");
                    Client.setUid(uid);
                    //设置名称
                    String name= (String) recvObject.get("name");
                    System.out.printf("[%s]Hello %s(%s)\n","TCP",name,uid);
                    //向服务器查询Rank信息
                    UDPClient.getRank();
                }else if(result.equals("failed")){
                    //登录失败
                    String wrong= (String) recvObject.get("wrong");
                    System.out.println("登录失败|"+wrong);
                }else{
                    //其他情况
                }
                break;
            case "personal_msg":
                int from_uid= (int) recvObject.get("from_uid");
                String content= (String) recvObject.get("msg");
                System.out.println("["+from_uid+"]:" + content);
                break;
            case "disconnect":
                //弹出挤线提示，并返回登录界面
                //重置uid
                //打印
                System.out.println("该账户正在被其他人登录，您已被迫下线");
                Client.setUid(0);
                break;
            default:
                System.out.println(recvObject);
                break;
        }
    }
}

class Client_send implements Runnable{
    private Socket mSocket;
    private ObjectOutputStream oos;
    private JSONObject object;

    public Client_send(Socket mSocket,ObjectOutputStream oos,JSONObject object) {
        this.mSocket = mSocket;
        this.oos=oos;
        this.object=object;
    }

    @Override
    public void run() {
        try {
            while(true){
                oos.writeObject(object);
                oos.flush();//必须刷新
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

class Client_heart implements Runnable{
    private Socket mSocket;
    private ObjectOutputStream oos;

    public Client_heart(Socket mSocket, ObjectOutputStream oos) {
        this.mSocket = mSocket;
        this.oos = oos;
    }

    @Override
    public void run() {
        try {
            System.out.println("心跳包线程已启动...");
            //多个oos会报错，接收端会检查传来的oos是不是同一个东西，不一致报错
            while(true){
                //每60秒发送心跳包
                Thread.sleep(60000);
                JSONObject object=new JSONObject();
                object.put("type","heart");
                object.put("msg","心跳包");
                oos.writeObject(object);
                oos.flush();//必须刷新
            }
        } catch (Exception e) {
            e.printStackTrace();
            //关闭套接字
            try {
                mSocket.close();
                Client.connection_state=false;
                Client.login_sate=false;
                Client.reconnect();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}



class Client_login implements Runnable{
    private Socket mSocket;
    private ObjectOutputStream oos;
    private String phoneNumber;
    private String password;

    public Client_login(Socket mSocket, ObjectOutputStream oos, String phoneNumber, String password) {
        this.mSocket = mSocket;
        this.oos = oos;
        this.phoneNumber = phoneNumber;
        this.password = password;
    }

    @Override
    public void run() {
        try {
            System.out.println("登录...");
            //登录数据

            JSONObject object=new JSONObject();
            object.put("type","login");
            object.put("user",phoneNumber);
            object.put("key",password);
            oos.writeObject(object);
            oos.flush();//必须刷新
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

class Client_register implements Runnable{
    private Socket mSocket;
    private ObjectOutputStream oos;
    private String phoneNumber;
    private String password;

    public Client_register(Socket mSocket, ObjectOutputStream oos, String phoneNumber, String password) {
        this.mSocket = mSocket;
        this.oos = oos;
        this.phoneNumber = phoneNumber;
        this.password = password;
    }

    @Override
    public void run() {
        try {
            System.out.println("注册中...");
            //注册数据
            JSONObject object=new JSONObject();
            object.put("type","register");
            object.put("user",phoneNumber);
            object.put("key",password);
            oos.writeObject(object);
            oos.flush();//必须刷新
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

