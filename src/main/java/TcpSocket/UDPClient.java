package TcpSocket;

import TcpSocket.bean.Card;
import TcpSocket.bean.CardUtil;
import TcpSocket.bean.Player;
import TcpSocket.bean.Room;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import static TcpSocket.UDPClient.decode;
import static TcpSocket.UDPSever.getPacket;

/**
 * @author Mr.Independent(严良鹏 20H034160215)
 * @date 2022/6/7 - 19:44
 */
public class UDPClient {
    public static final int PORT=6667;
    public static DatagramSocket socket;
    public static boolean socket_state = false;
    public static long msgId = 0;
    public static long token=213;//下次发包的开始,TCP接收原始token,UDP续上
    //玩家属性
    public static int rank;//rank分
    //卡牌记录outIndex

    static HashMap<Long,Long> msgQueue;//等待确认的封包消息队列
    public static void init() {
        //创建UDP套接字
        //1、不管有木有出现异常，finally块中代码都会执行;
        initMessageId();//初始化发包ID
        msgQueue=new HashMap<>();
        try {
            socket = new DatagramSocket();
            new Thread(new Client_listen_UDP()).start();
            socket_state = true;
        } catch (SocketException e) {
            e.printStackTrace();
            socket_state = false;
        }
    }

    private static void initMessageId() {
        msgId = (int) (Math.random() * 64);
    }

    /**
     * 上层协议
     *
     * @param body
     */
    public static Object protocol(JSONObject body) {
        JSONObject header = new JSONObject();
        header.put("msgId", msgId);
        header.put("ack", 0);//0为没有应答
        header.put("token", token);//发包的验证标识
        header.put("uid", Client.uid);//此uid 由服务器通过 TCP发放的
        JSONObject packet = new JSONObject();
        packet.put("header", header);
        packet.put("body", body);
        //将等待确认的封包添加到消息队列中
        msgQueue.put(msgId,System.currentTimeMillis());
        System.out.printf("msgId%d:已发送\n",msgId++);
        return packet;
    }


    public static void decode(JSONObject packet) {
        //输出所有收到的包
//        System.out.printf("[%s]recv:%s\n","UDP",packet);
        JSONObject header= (JSONObject) packet.get("header");
        //有header需要分析
        if(header!=null){
            token= (long) header.get("token");
            if((long)(header.get("ack"))==1){
                //确认包,消息无须经过解码回显
                long msgId= (long) header.get("msgId");
                long delay=System.currentTimeMillis()-msgQueue.get(msgId);
                System.out.printf("msgId%d:已接收确认包 延迟%dms\n",msgId,delay);
                msgQueue.remove(msgId);
                return;
            }else{

            }
        }
        //非确认包，需要解码回显
        JSONObject body= (JSONObject) packet.get("body");
        String type= (String) body.get("type");

        switch (type){
            case "room":
                decodeRoom(body);
                break;
            case "game":
                decodeGame(body);
                break;
            case "personal":
                decodePersonal(body);
                break;
        }

    }

    private static void decodePersonal(JSONObject body) {
        String cmd=(String) body.get("cmd");
        String rst= (String) body.get("rst");
        String wrong=(String) body.get("wrong");
        String name_from=(String) body.get("name_from");
        switch (cmd){
            case "msgAll":
                //回显大喇叭消息
                int from= (int) (long)body.get("uid_from");
                String content_= (String) body.get("content");
                System.out.printf("[%s]%s(%s):%s\n","All",name_from,from,content_);
                break;
            case "msgRoom":
                //回显房间消息
                int _from_uid= (int) (long)body.get("uid_from");
                String _content= (String) body.get("content");
                System.out.printf("[%s]%s(%s):%s\n","Room",name_from,_from_uid,_content);
                break;
            case "msg":
                //回显个人消息
                int from_uid= (int) (long)body.get("uid_from");
                String content= (String) body.get("content");
                System.out.printf("[%s]%s(%s):%s\n","Personal",name_from,from_uid,content);
                break;
            case "setName":
                if(rst.equals("successful")){
                    System.out.printf("[%s]%s\n","Personal","设置名称成功");
                    //设置本地新名称
                    Client.name=(String) body.get("name");
                }else{
                    System.out.printf("[%s]%s:%s\n","Personal","设置名称失败",wrong);
                }
                break;
            case "getRank":
                rank=(int)(long)body.get("rank");
                //打印 rank 分
                System.out.printf("[%s]%s:%s\n","Personal","您的Rank分",rank);
                break;
        }
    }

    public static synchronized void decodeGame(JSONObject body){
        JSONObject body_= (JSONObject) body.clone();
        String cmd=(String) body_.get("cmd");
        String rst=(String) body_.get("rst");
        switch (cmd){
            case "start":
                if(rst.equals("successful")){
                    //服务器通知游戏开始，回显游戏界面
                    System.out.printf("[%s]%s\n","Game","游戏已开始");
                    System.out.printf("[%s]%s\n","Game","记牌器已初始化");
                }else if(rst.equals("unsuccessful")){
                    //服务器通知房间未准备好
                    System.out.printf("[%s]%s\n","Game","房间内玩家未准备好，不能开始游戏");
                }
                break;
            case "deal":
                //回显收到的卡牌信息
                JSONArray cards= (JSONArray) body.get("cards");
                System.out.println("Your cards show as followed");
                System.out.println("--------------------------------");
                ArrayList<CardUtil> cardUtils=new ArrayList<>();
                for (Object o : cards) {
                    CardUtil cardUtil=CardUtil.toCardUtil((JSONObject) o);
                    System.out.println(cardUtil);//打印
                    //显示不在记牌器中的牌
                    //通知GameActivity
                    //封装消息
                    cardUtils.add(cardUtil);
                }
                System.out.println("--------------------------------");
                System.out.println("您可以通过发送/cast指令来出牌");
                break;
            case "cast":
                //回显押牌是否成功
                if(rst.equals("successful")){
                    CardUtil cardUtil= CardUtil.toCardUtil((JSONObject) body.get("cardUtil"));
                    System.out.printf("[%s](%s)%s\n","Game",cardUtil.index,"出牌成功");
                }else if(rst.equals("failed")){
                    System.out.printf("[%s]%s\n","Game","出牌失败");
                }
                break;
            case "ok":
                //回显对方已打出牌
                System.out.printf("[%s]对方已出牌\n","Game");
                break;
            case "show":
                //回显开牌数据
                //打印对手卡牌
                JSONObject card= (JSONObject) (body_.get("peer's card"));
                Card peer=Card.toCard(card);
                System.out.printf("[%s]%s%s\n","Game","对方的卡牌为",peer.toString());
                //打印本回合胜负
                int winnerUid=(int)(long)body_.get("winnerUid");
                if(winnerUid==-1){
                    System.out.printf("[%s]%s\n","Game","本回合平局");
                }else if(winnerUid==Client.uid){
                    System.out.printf("[%s]%s\n","Game","你赢了");
                }else{
                    System.out.printf("[%s]%s\n","Game","你输了");
                }

                //打印双方血量
                int hp_A=(int)(long)body_.get("my_hp");
                System.out.printf("[%s]My.Hp=%s\n","Game",hp_A);
                int hp_B=(int)(long)body_.get("your_hp");
                System.out.printf("[%s]Your.Hp=%s\n","Game",hp_B);
                break;
            case "end"://服务器通知游戏结束，显示游戏结果，回显房间信息
                System.out.printf("[%s]%s\n","Game","本场游戏已结束");
                rank= (int)(long) body.get("rank");
                System.out.printf("[%s]%s:%s\n","Game","您的rank已更新",rank);
                //回显房间信息
                Room thisRoom= Room.toRoom((JSONObject) body_.get("room"));
                printMyRoomInfo(thisRoom);
                break;
            default:
                System.out.printf("未知指令:%s\n",cmd);
        }
    }

    public static void decodeRoom(JSONObject body){
        JSONObject body_= (JSONObject) body.clone();
        String cmd= (String) body_.get("cmd");

        String rst= (String) body_.get("rst");
        switch (cmd){
            case "enter":
                if(rst.equals("successful")){
                    //进入成功
                    Room thisRoom= Room.toRoom((JSONObject) body_.get("room"));
                    System.out.printf("[%s]%s %s %s\n","Room",cmd,thisRoom.getRoomId(),"successfully");
//                    printMyRoomInfo(thisRoom);
                }else{
                    //进入失败
                    String wrong= (String) body_.get("wrong");
                    System.out.printf("[%s]%s %s:%s\n","Room",cmd,"unsuccessfully",wrong);
                }
                break;
            case "offerAllRoomsInfo":
                ArrayList<Room> roomsTable=new ArrayList<>();
                //解JSON Object
                JSONArray roomsArray= (JSONArray) body_.get("rooms");
                for (int i = 0; i < roomsArray.size(); i++) {
                    JSONObject obj= (JSONObject) roomsArray.get(i);
                    roomsTable.add(Room.toRoom(obj));
                }
                System.out.println(roomsTable);
                printRoomsInfo(roomsTable);
                break;
            case "exit":
                if(rst.equals("successful")){
                    //进入成功
                    System.out.println("退出房间成功");
                }else{
                    //进入失败
                    String wrong= (String) body.get("wrong");
                    System.out.printf("退出失败:%s\n",wrong);
                }
                break;
            case "update room info":
                Room myRoom= Room.toRoom((JSONObject) body_.get("room"));
                printMyRoomInfo(myRoom);
                break;
            default:


        }
    }


    private static void printMyRoomInfo(Room thisRoom) {
        System.out.printf("%s\n","--------------YOUR ROOM--------------");
        System.out.printf("%s %s %s %s %s %s\n","房间号",thisRoom.getRoomId(),"状态",thisRoom.getState(),"是否为满",thisRoom.isFull());
        System.out.println("-------------------------------------");
        System.out.printf("%8s%10s%10s\n","玩家id","玩家名称","状态");
        thisRoom.getAllPlayer().forEach((p)->{
            System.out.printf("%8s%14s%14s\n",p.getUid(),p.getName(),p.getState());
        });
        System.out.println("-------------------------------------");
    }

    private static void printRoomsInfo(ArrayList<Room> roomsTable) {
        System.out.printf("%s\n","-------LIST OF ROOMS-------");
        System.out.printf("%6s%8s%8s%12s%12s%12s%12s\n","房间号","状态","是否为满","玩家","玩家状态","玩家","玩家状态");
        roomsTable.forEach((room)->{
            System.out.printf("%8s%10s%10s",room.getRoomId(),room.getState(),room.isFull());
            for (Player player : room.getAllPlayer()) {

                if(player!=null)//判断是否为空玩家
                    System.out.printf("%10s%s(%s)%13s","",player.getName(),player.getUid(),player.getState());
            }
            System.out.printf("\n");
        });
        System.out.println("--------------------------");
    }

    /**
     * 打牌方法
     */
    public static void msg(){
        System.out.print("请输入对方uid:");
        Scanner scanner=new Scanner(System.in);
        int uid=Integer.parseInt(scanner.nextLine());
        System.out.print("请输入聊天内容:");
        String content=scanner.nextLine();

        JSONObject packet=new JSONObject();
        packet.put("type","personal");
        packet.put("cmd","msg");
        packet.put("uid_to",uid);
//        packet.put("name_from",Client.name); 由服务器来查询
        packet.put("content",content);
        send(packet);
    }
    public static void cast(){
        System.out.println("请输入Card Id:");
        Scanner scanner=new Scanner(System.in);
        int index=scanner.nextInt();

        JSONObject packet=new JSONObject();
        packet.put("type","game");
        packet.put("cmd","cast");
        packet.put("index",index);
        send(packet);
    }
    public static void enterRoom(){
        JSONObject enterPacket=new JSONObject();
        enterPacket.put("type","room");
        enterPacket.put("cmd","enter");
        System.out.println("请输入你要加入的房间:");
        Scanner scanner=new Scanner(System.in);
        int roomId=scanner.nextInt();
        enterPacket.put("roomId",roomId);
        send(enterPacket);
    }

    public static void newRoom(){
        JSONObject newRoomPacket=new JSONObject();
        newRoomPacket.put("type","room");
        newRoomPacket.put("cmd","new");
        send(newRoomPacket);
    }
    public static void showAllRooms(){
        JSONObject newRoomPacket=new JSONObject();
        newRoomPacket.put("type","room");
        newRoomPacket.put("cmd","showAllRooms");
        send(newRoomPacket);
    }
    public static void exitRoom(){
        JSONObject exitRoomPacket=new JSONObject();
        exitRoomPacket.put("type","room");
        exitRoomPacket.put("cmd","exit");
        send(exitRoomPacket);
    }

    public static void switchPlayerState(){
        JSONObject packet=new JSONObject();
        packet.put("type","room");
        packet.put("cmd","switch player state");
        send(packet);
    }

    public static void send(JSONObject body){
        new Thread(new Client_send_UDP(body)).start();
    }


    public static void setName() {
        System.out.printf("请输入你要设置的名称:");
        Scanner scanner=new Scanner(System.in);
        String name=scanner.nextLine();
        JSONObject packet=new JSONObject();
        packet.put("type","personal");
        packet.put("cmd","setName");
        packet.put("name",name);
        send(packet);
    }

    public static void getRank() {
        JSONObject packet=new JSONObject();
        packet.put("type","personal");
        packet.put("cmd","getRank");
        send(packet);
    }

    public static void msgAll() {
        Scanner scanner=new Scanner(System.in);
        System.out.print("请输入聊天内容:");
        String content=scanner.nextLine();

        JSONObject packet=new JSONObject();
        packet.put("type","personal");
        packet.put("cmd","msgAll");
//        packet.put("name_from",Client.name);由服务器来查询
        packet.put("content",content);
        send(packet);
    }

    public static void msgRoom() {
        Scanner scanner=new Scanner(System.in);
        System.out.print("请输入聊天内容:");
        String content=scanner.nextLine();

        JSONObject packet=new JSONObject();
        packet.put("type","personal");
        packet.put("cmd","msgRoom");
        packet.put("content",content);
        send(packet);
    }

    /**
     * 结束本回合方法
     */
    public static void finishTurn(){
        JSONObject packet=getPacket("game","finish","","");
        send(packet);
    }
}

class Client_listen_UDP implements Runnable {
    @Override
    public void run() {
        try {
            while (true) {
                //转化数据格式为JSONObject
                DatagramPacket mDatagramPacket = new DatagramPacket(new byte[1024], 1024);
                UDPClient.socket.receive(mDatagramPacket);
                String s = new String(mDatagramPacket.getData(), 0, mDatagramPacket.getLength());

                JSONObject packet = (JSONObject) new JSONParser().parse(s);

                System.out.println(packet);
                //解析数据
                decode(packet);

            }
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            UDPClient.socket.close();
        }

    }
}

class Client_send_UDP implements Runnable{
    private JSONObject body;

    Client_send_UDP(JSONObject body) {
        this.body = body;
    }

    @Override
    public void run() {
            if (!UDPClient.socket_state)
                return;
            try {
//                System.out.printf("[%s]send:%s\n","UDP",body);
                byte[] data = UDPClient.protocol(body).toString().getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(new byte[0], 0, InetAddress.getByName(Client.IP), UDPClient.PORT);

                packet.setData(data);
                UDPClient.socket.send(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //卢本伟

    }
}