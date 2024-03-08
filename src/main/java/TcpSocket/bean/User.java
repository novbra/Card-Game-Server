package TcpSocket.bean;

import TcpSocket.LoggerUtil;
import TcpSocket.MySQLDBUtil;
import TcpSocket.TcpSocketSever;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;

/**
 * @author Liangpeng Yan(Nickname:Mr.Independent)
 * @date 2022/10/31 - 14:37
 */
public class User extends Thread{

    private static Logger logger = LoggerUtil.getLogger(TcpSocketSever.class);
    private static HashMap<Socket,User> table=new HashMap<>();
    private static int count=0;//连接总数
    public static User getUser(Socket socket){
        return table.get(socket);
    }

    private int UID;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean wantDrop;//断连需求

    /**
     * 处理并被动发送
     */
    @Override
    public void run() {
        try {
            while(!socket.isClosed()){
                //解析封包
                handle((JSONObject) in.readObject());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            logger.info(UID+" has been off line");
            close();
        }
    }

    /**
     * 主动发送
     */
    public class Messenger extends Thread{
        private JSONObject packet;
        public Messenger(JSONObject object) {
            this.packet=object;
        }
        @Override
        public void run() {
            try {
//            ObjectOutputStream oos= new ObjectOutputStream(cSocket.getOutputStream());
//            需要确保每个socket 用同一个oos，不能new，否则接收端会报错
                out.writeObject(packet);
                out.flush();//发送之后必须要刷新
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        public void post(){
            this.start();
        }
    }

    /**
     * 被动发送
     * @param packet
     * @return
     */
    public synchronized boolean sendPacket(JSONObject packet){
        try {
            out.writeObject(packet);
            out.flush();//发送之后必须要刷新
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void handle(JSONObject recvObject){
        System.out.println(recvObject);

        JSONObject object=new JSONObject();//待发送的JSONObj

        String type= (String) recvObject.get("type");
        String cIp=socket.getInetAddress().getHostAddress();

        switch (type){
            case "goodbye":
                logger.info(UID+"'s heart is beating");
                close();
                break;
            case "heart":
                logger.info(UID+"'s heart is beating");
                break;
            case "version":
                object.put("type","version");
                object.put("version_number",TcpSocketSever.CLIENT_VERSION);
                sendPacket(object);
                break;
            case "register":
                String registerUser,registerKey;
                registerUser= (String) recvObject.get("user");
                registerKey= (String) recvObject.get("key");
                if(MySQLDBUtil.register(registerUser,registerKey,cIp)){
                    logger.info(UID+"("+registerUser+") 注册成功");
                    //注册成功封包
                    object.put("type","register");
                    object.put("result","successful");
                    //预留两个参数
                    object.put("arg0","");
                    object.put("arg1","");
                }else{
                    logger.info(UID+"("+registerUser+") 注册失败");
                    //注册失败封包
                    object.put("type","register");
                    object.put("result","failed");
                    //预留两个参数
                    object.put("wrong","本功能未开放");
                    object.put("arg0","");
                }
                sendPacket(object);
                break;
            case "login":
                String loginUser,loginKey;
                loginUser= (String) recvObject.get("user");
                loginKey= (String) recvObject.get("key");
                login(loginUser,loginKey,socket);
                break;
            default:
                System.out.printf("未知type:%s\n",type);
        }
    }


    public User(Socket socket) {
        this.socket = socket;
        this.UID=count++;
        try {
            this.out=new ObjectOutputStream(socket.getOutputStream());
            this.in=new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            table.put(socket,this);
        }

    }


    private void login(String loginUser, String loginKey, Socket socket) {
        JSONObject loginPacket=new JSONObject();
        loginPacket.put("type","login");
        //ip
        String ip=socket.getInetAddress().getHostAddress();
        //uid
        Integer uid_database=MySQLDBUtil.getUid(loginUser);

        if(MySQLDBUtil.login(loginUser,loginKey,ip)){
            //密码校验正确
            //绑定套接字
            int loginCode;

            Player checkPlayer = Player.getPlayer(uid_database);
            if(checkPlayer==null){
                loginCode=0;
            }else{
                //挤线
                loginCode=2;
            }

            switch (loginCode){
                case 0:
                    //登录成功 (非挤线登录)
                    logger.info(uid_database+" 登录成功");
                    //token 令牌的发放
                    String token=new Token(uid_database).getCipher();//密文
                    //封包
                    loginPacket.put("result","successful");
                    loginPacket.put("token",token);//后期可以换成 token 临时许可
                    break;
                case 1:
                    //同个套接字重复登录相同账号
                    break;
                case 2:
                    //不同套接字登录相同账号
                    //打印
                    logger.info(uid_database+" 发生挤线");
                    try {
                        //向当前在线用户发出断开要求
                        Player.getPlayer(uid_database).disconnect(Player.FORCE_DISCONNECTED);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //向挤线的用户发出异常提示 封包
                    loginPacket.put("result", "failed");
                    loginPacket.put("wrong", "您登录的账号已在线上，已向当前在线用户发送断开请求，请再次尝试登录");
                    loginPacket.put("arg0", "");
                    break;
                case 3:
                    //非法
                    //打印
                    logger.info(uid_database+" 登录异常:非法操作");
                    //封包
                    loginPacket.put("result", "failed");
                    loginPacket.put("wrong", "非法登录");
                    loginPacket.put("arg0", "");
                    break;
                case 4:
                    //该套接字已经正常登录但还是执意想登录，限号
                    //打印
                    logger.info(uid_database+"("+loginUser+")"+"该用户已登录，不允许其执行登录操作");
                    //封包
                    loginPacket.put("result", "failed");
                    loginPacket.put("wrong", "您已登录，不允许执行登录操作");
                    loginPacket.put("arg0", "");
                    break;
            }

        }else{
            //打印
            logger.info(uid_database+"("+loginUser+")"+"登陆失败:账号不存在或密码错误");
            //登录失败封包
            loginPacket.put("result","failed");
            //预留两个参数
            loginPacket.put("wrong","账号不存在或密码错误");
            loginPacket.put("arg0","");
        }
        sendPacket(loginPacket);
    }

    public void close(){
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket !=null){
                socket.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            table.remove(this.socket,this);
        }
    }

    /**
     * 登出
     * @param reason
     */
    public void logout(String reason){
        JSONObject packet=new JSONObject();
        packet.put("type","logout");
        packet.put("reason",reason);
        sendPacket(packet);
    }


    public int getUID() {
        return UID;
    }
}
