package TcpSocket;

import TcpSocket.bean.*;
import TcpSocket.observer.SendStateObserver;
import TcpSocket.services.UserService;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static TcpSocket.UDPSever.*;

/**
 * @author Mr.Independent(严良鹏 20H034160215)
 * @date 2022/6/7 - 19:44
 */
public class UDPSever {
    private static Logger logger = LoggerUtil.getLogger(TcpSocketSever.class);
    private static final int PORT = 6667;
    public static DatagramSocket mDatagramSocket;
    public static UserService userService = new UserService();

    public static void startUDPServer() {
        try {
//            System.out.printf("[%s]%s\n","Game","initiating game resource...");
//            Game.initCardPack();

            System.out.printf("[%s]%s\n", "UDP", "Server start...");
            mDatagramSocket = new DatagramSocket(PORT);

            System.out.printf("[%s]%s\n", "UDP", "Start Listening...");
            new Thread(new Server_listen_UDP()).start();
        } catch (SocketException e) {
            e.printStackTrace();
            mDatagramSocket.close();
            System.out.printf("[%s]%s\n", "UDP", "Can not start Server");
        }
    }


    public static Player getPlayer(int uid) {
        return Player.table.get(uid);
    }

    /**
     * 简化包
     *
     * @param type
     * @param cmd
     * @return
     */
    public static JSONObject getPacket(String type, String cmd, String rst, String wrong) {
        JSONObject packet = new JSONObject();
        packet.put("type", type);
        packet.put("cmd", cmd);
        packet.put("rst", rst);
        packet.put("wrong", wrong);
        return packet;
    }


    public static class Server_send_UDP implements Runnable {
        private InetAddress ip;
        private int port;
        private MyPacket packet;
        private SendStateObserver observer;//记录发送的情况
        public Server_send_UDP(InetAddress ip, int port, MyPacket packet) {
            this.ip = ip;
            this.port = port;
            this.packet = packet;
        }
        public Server_send_UDP(InetAddress ip, int port, MyPacket packet,SendStateObserver o) {
            this.ip = ip;
            this.port = port;
            this.packet = packet;
            this.observer=o;
        }

        @Override
        public void run() {
            byte[] data = packet.toString().getBytes(StandardCharsets.UTF_8);
            DatagramPacket dPacket = new DatagramPacket(new byte[0], 0, ip, port);
            dPacket.setData(data);
            try {
                mDatagramSocket.send(dPacket);
                System.out.println("send:"+packet);
                //发送成功执行PostiveTask
                if(observer!=null) {
                    observer.positiveTask();
                }
            } catch (IOException e) {
                e.printStackTrace();
                //发送失败执行PassiveTask
                if(observer!=null)
                    observer.passiveTask();
            }
        }
    }

    static class Server_listen_UDP implements Runnable {
        @Override
        public void run() {
            while (true) {

                try {
                    DatagramPacket mDatagramPacket = new DatagramPacket(new byte[1024], 1024);
                    mDatagramSocket.receive(mDatagramPacket);
                    //获取数据中对方IP地址和端口号
                    InetAddress ip = mDatagramPacket.getAddress();
                    int port = mDatagramPacket.getPort();
                    //解析数据
                    String s = new String(mDatagramPacket.getData(), 0, mDatagramPacket.getLength(), StandardCharsets.UTF_8);
                    JSONObject packet = (JSONObject) new JSONParser().parse(s);
                    //解析为MyPacket对象
                    MyPacket myPacket = MyPacket.parse(packet);
                    System.out.println("recv:"+myPacket);
                    //处理登录事件
                    if (myPacket.getFunc() == MyPacket.FUNC_LOGIN) {
                        //login处理
                        String token = myPacket.getToken();
                        String UID = String.valueOf(Token.checkToken(token));
                        System.out.println(UID);
                        if (!UID.equals("false")) {
                            //验证成功
                            Player player = Player.newInstance(Integer.parseInt(UID));
                            //定位player
                            player.login(ip, port);

                            MyPacket loginPacket = new MyPacketBuilder().buildFunc(MyPacket.FUNC_LOGIN)
                                    .buildState(MyPacket.STATE_SUCCESSFUL)
                                    .buildResult(player.getUid() + "&" + player.getName())
                                    .build();
                            player.send(loginPacket);
                        } else {
                            //验证失败 过期或篡改
                            MyPacket loginPacket = new MyPacketBuilder().buildFunc(MyPacket.FUNC_LOGIN)
                                    .buildState(MyPacket.STATE_FAILED)
                                    .buildWrong("令牌过期或被篡改")
                                    .build();
                            new Thread(new UDPSever.Server_send_UDP(ip, port, loginPacket)).start();
                        }
                    } else {
                        //处理非登录事件
                        Player player = Player.getPlayer(myPacket.getUID());//问题所在
                        switch (myPacket.getFunc()) {
                            case MyPacket.FUNC_HEART:
                                //对心跳的处理
                                break;
                            case MyPacket.FUNC_CHECK:
                                //处理确认接受包，交给玩家对象处理，删除一部分已接受的包
                                player.recv(myPacket);
                                break;
                            case MyPacket.FUNC_FUNC:
                                //对功能性的封包，回发确认包
                                MyPacket checkPacket = new MyPacketBuilder().buildSeq(myPacket.getSeq())
                                        .buildFunc(MyPacket.FUNC_CHECK)
                                        .buildAck(MyPacket.HAS_RECEIVED)
                                        .buildBody()
                                        .build();
                                player.send(checkPacket);
                                //玩家对象对功能性封包进行处理
                                player.recv(myPacket);
                                break;
                            case MyPacket.FUNC_DISCONNECT:
                                switch (myPacket.getCmd()){
                                    case "goodbye":
                                        player.disconnect(Player.GOODBYE_DISCONNECTED);
                                        break;
                                }
                                break;

                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                }


            }

        }
    }
}



