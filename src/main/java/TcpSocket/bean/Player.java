package TcpSocket.bean;

import TcpSocket.*;
import TcpSocket.observer.RoomStateObserver;
import TcpSocket.observer.SendStateObserver;
import com.sun.istack.internal.Nullable;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import TcpSocket.services.*;

import static TcpSocket.services.RoomService.enterRoom;

/**
 * @author Mr.Independent
 * @date 2022/6/8 - 12:30
 */
public class Player {
    private static Logger logger = LoggerUtil.getLogger(TcpSocketSever.class);

    enum playerState {
        READY, NOT_READY, LOST;
    }

    public static HashMap<Integer, Player> table=new HashMap<>();

    private Currencies currencies;
    private final static int MAX_CACHE_LENGTH = 128;
    private int uid;
    private String name;

    private int sendSeq = 0;
    private int recvSeq;
    private Hashtable<Integer, MyPacket> cacheSendTable;//存放发送的


    public int getUid() {
        return uid;
    }

    private int gameType;//保留
    public Gamer gamer;//游戏中的角色
    private playerState state;
    private int roomId;
    private RoomStateObserver roomStateObserver=new RoomStateObserver(this);
    private long rank;
    private InetAddress ip;
    private int port;

    public RoomStateObserver getRoomStateObserver() {
        return roomStateObserver;
    }

    public static boolean isOnline(int uid) {
        return table.containsKey(uid);
    }

    public static Player getPlayer(int uid) {
        return table.get(uid);
    }

    /**
     * 不带Observer的发送
     *
     * @param packet
     */
    public void send(MyPacket packet) {
        packet.setUID(this.uid);


        if (packet.getFunc() == MyPacket.FUNC_FUNC) {
            packet.setSeq(++sendSeq);
            //缓存到发送列表中
            if (cacheSendTable.size() >= MAX_CACHE_LENGTH) {
                //删除一个释放空间，后期修改
                Iterator<Integer> iterator = cacheSendTable.keySet().iterator();
                if (iterator.hasNext()) {
                    iterator.next();
                    iterator.remove();
                }

            }
            cacheSendTable.put(sendSeq, packet);
        }
        new Thread(new UDPSever.Server_send_UDP(this.ip, this.port, packet)).start();
    }

    /**
     * 带Observer的发送
     *
     * @param packet
     * @param o
     */
    public void send(MyPacket packet, SendStateObserver o) {
        if (packet.getFunc() == MyPacket.FUNC_FUNC) {
            packet.setSeq(++sendSeq);
            //缓存到发送列表中
            if (cacheSendTable.size() >= MAX_CACHE_LENGTH) {
                //删除一个释放空间，后期修改
                Iterator<Integer> iterator = cacheSendTable.keySet().iterator();
                if (iterator.hasNext()) {
                    iterator.next();
                    iterator.remove();
                }

            }
            cacheSendTable.put(sendSeq, packet);
        }
        new Thread(new UDPSever.Server_send_UDP(this.ip, this.port, packet, o)).start();
    }

    public void recv(MyPacket packet) {
        switch (packet.getFunc()) {
            case MyPacket.FUNC_CHECK:
                int sendSeq = packet.getSeq();
                if (cacheSendTable.containsKey(sendSeq))
                    cacheSendTable.remove(sendSeq);
                System.out.println(sendSeq + "已被客户成功接受，缓存区现已移除");
                break;
            case MyPacket.FUNC_FUNC:
                int seq = packet.getSeq();
                if (seq != 0) {
                    //对recvSeq 的判断 (差距过大，有封包挂风险，或者断线严重)
                    recvSeq = seq;
                }
                handle(packet);
                break;
        }
    }

    /**
     * locate功能 标记对方的ip和地址
     *
     * @param ip
     * @param port
     */
    public void login(InetAddress ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public void handle(MyPacket packet) {
        //分流
        switch (packet.getType()) {
            case "msg":
                msgCmd(packet);
                break;
            case "personal":
                personalCmd(packet);
                break;
            case "room":
                roomCmd(packet);
                break;
            case "game":
                gameCmd(packet);
                break;
        }

    }

    /**
     * 聊天消息的处理
     *
     * @param packet
     */
    private void msgCmd(MyPacket packet) {
        try {
            String content = (String) packet.getExtra("content");
            SendStateObserver observer = new SendStateObserver() {
                @Override
                public void positiveTask() {//消息发送成功
                    try {
                        send(MsgSystem.PacketService.positiveInfo());
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.error("positiveTask:send失败");
                    }
                }
                @Override
                public void passiveTask() {//消息发送失败
                    try {
                        send(MsgSystem.PacketService.wrongInfo("消息发送失败"));
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.error("passiveTask:send失败");
                    }

                }
            };
            switch (packet.getCmd()) {
                case "msg"://私人聊天
                    //对消息做转发
                    int UIDTo = (int) (long) packet.getExtra("to");
                {   //判断这个玩家存不存在
                    DatabaseUser databaseUser = null;
                    databaseUser = UDPSever.userService.selectedById(UIDTo);
                    if (databaseUser != null) {
                        //该用户存在
                        if (Player.isOnline(UIDTo)) {
                            //该用户在线
                            //Player对象
                            Player toPlayer = Player.getPlayer(UIDTo);
                            //发送消息
                            toPlayer.send(MsgSystem.getEncoder().encode(packet.getCmd(), this.uid, this.name, content), observer);
                        } else {
                            //该用户不在线
                            send(MsgSystem.PacketService.wrongInfo("该用户不在线"));
                        }

                    } else {
                        //该用户不存在
                        send(MsgSystem.PacketService.wrongInfo("该用户不存在"));
                        logger.warn("msg:用户不存在");
                    }
                    break;
                }
                case "msgRoom"://房间内聊天
                    Room room = Room.getRoom(this.roomId);
                    if (room != null) {
                        //向房内人发消息
                        room.sendRoomers(MsgSystem.getEncoder().encode(packet.getCmd(),this.uid, this.name, content));
                    } else {
                        //不做任何事情
                        logger.warn("msgRoom:房间为null");
                    }
                    break;
                case "msgAll"://大喇叭
                    Player.table.forEach((id, toPlayer) -> {
                        System.out.println(id);
                        try {
                            toPlayer.send(MsgSystem.getEncoder().encode(packet.getCmd(),this.uid, this.name, content),observer);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void personalCmd(MyPacket packet) {
        try {
            switch (packet.getCmd()) {
                case "getRank":
                    send(PersonService.deliverRank(this));
                    break;
                case "getName":
                    send(PersonService.getName(this));
                    break;
                case "setName":
                    String newName = (String) packet.getExtra("newName");
                    //查询该名字是否已被占用
                    if (MySQLDBUtil.checkNameUnique(newName)) {
                        //该名字未被占用
                        if (MySQLDBUtil.updateFromUser(uid, "name", newName)) {
                            //设置名称成功
                            send(PersonService.setName(MyPacket.STATE_SUCCESSFUL, null));
                        } else {
                            //设置名称失败
                            send(PersonService.setName(MyPacket.STATE_FAILED, "数据库更新名称失败"));
                        }
                    } else {
                        //该名称已被占用
                        send(PersonService.setName(MyPacket.STATE_FAILED, "该名称已被占用"));
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void roomCmd(MyPacket packet) {
        String cmd =packet.getCmd();

        switch (cmd) {
            case "new"://创建房间
                enter(null);
                break;
            case "showAllRooms"://提供所有房间信息
                try {
                    send(RoomService.offerAllRoomsInfo());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case "enter":
                int roomId = (int) (long) packet.getExtra("roomId");
                Room room = Room.getRoom(roomId);
                enter(room);
                break;
            case "exit":
                drop();
                break;
            case "start game"://房主权限预留
                break;
            case "switch player state":
                //改变玩家状态,随之改变房间状态
                try {
                    switchState();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                System.out.println(cmd);

        }
    }

    /**
     * 游戏类解包
     */
    public void gameCmd(MyPacket packet) {
        switch (packet.getCmd()) {
            case "cast"://打牌
                int index = (int) (long) packet.getExtra("index");
                cast(index);
                break;
            case "discard"://弃牌
                int index_discard = (int) (long) packet.getExtra("index");
                discard(index_discard);
                break;
            case "finish"://结束本轮回合
                this.gamer.finish();
                break;
        }
    }


    @Override
    public String toString() {
        return "Player{" +
                "uid=" + uid +
                ", name='" + name + '\'' +
                ", gameType=" + gameType +
                ", state=" + state +
                ", roomId=" + roomId +
                ", rank=" + rank +
                ", ip=" + ip +
                ", port=" + port +
                '}';
    }

    public InetAddress getIp() {
        return ip;
    }

    public long getRank() {
        return rank;
    }

    public int getPort() {
        return port;
    }

    public static Player newInstance(int UID) {
        int rank = MySQLDBUtil.getRank(UID);
        String name = MySQLDBUtil.getFromUser(UID, "name");
        return new Player(UID, name, rank, null, -1);
    }

    public Player(int UID, String name, long rank, InetAddress ip, int port) {
        this.uid = UID;
        this.name = name;
        this.rank = rank;
        this.ip = ip;
        this.port = port;
        this.roomId = 0;
        cacheSendTable = new Hashtable<>();
        table.put(UID, this);
    }

    public static Player toPlayer(JSONObject object) {
        long UID = (long) object.get("UID");
        String name_ = (String) object.get("name");
        int gameType = (int) (long) object.get("gameType");
        String playerState = (String) object.get("playerState");
        int roomId = (int) (long) object.get("roomId");
        long rank = (long) object.get("rank");
        InetAddress ip = null;
        try {
            ip = InetAddress.getByName((String) object.get("InetAddress"));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        int port = (int) (long) object.get("port");
        //如果不匹配则为 LOST
        Player.playerState state = Player.playerState.LOST;
        if (playerState.equals(Player.playerState.READY.toString())) {
            state = Player.playerState.READY;
        }
        if (playerState.equals(Player.playerState.NOT_READY.toString())) {
            state = Player.playerState.NOT_READY;
        }
        if (playerState.equals(Player.playerState.LOST.toString())) {
            state = Player.playerState.LOST;
        }
        return new Player(UID, name_, gameType, state, roomId, rank, ip, port);

    }

    public void setRank(long rank) {
        this.rank = rank;
    }

    public Player(long UID, String name, int gameType, playerState state, int roomId, long rank, InetAddress ip, int port) {
        this.uid = uid;
        this.name = name;
        this.gameType = gameType;
        this.state = state;
        this.roomId = roomId;
        this.rank = rank;
        this.ip = ip;
        this.port = port;
        //货币属性
        this.currencies = new Currencies();
    }

    public JSONObject toJSONObject() {
        JSONObject temp = new JSONObject();
        temp.put("UID", uid);
        temp.put("name", name);
        temp.put("gameType", gameType);
        temp.put("playerState", state.toString());
        temp.put("roomId", roomId);
        temp.put("rank", rank);
        temp.put("InetAddress", ip.getHostAddress());
        temp.put("port", port);
        return temp;
    }

    public void initPlayerForGame(int gameType, playerState state, int roomId) {
        this.gameType = gameType;
        this.state = state;
        this.roomId = roomId;
    }

    public void initPlayerState() {
        this.state = playerState.NOT_READY;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public playerState getState() {
        return state;
    }

    public boolean isReady() {
        return state == playerState.READY ? true : false;
    }

    public void initState() {
        state = playerState.NOT_READY;
    }

    /**
     * 更新玩家的状态
     *
     * @return
     * @throws Exception
     */
    public boolean switchState() throws Exception {

        if (Room.contain(this.roomId)) {//房间有效
            if (state == playerState.READY)
                state = playerState.NOT_READY;
            else if (state == playerState.NOT_READY)
                state = playerState.READY;

            Room thisRoom = Room.getRoom(roomId);
            thisRoom.renewRoomState();
            return true;
        } else {
            return false;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public final static int FORCE_DISCONNECTED = 1;
    public final static int GOODBYE_DISCONNECTED = 2;

    public void disconnect(int code) throws Exception {
        MyPacket packet = null;
        switch (code) {
            case FORCE_DISCONNECTED:
                packet = new MyPacketBuilder()
                        .buildFunc(MyPacket.FUNC_DISCONNECT)
                        .buildType("disconnect")
                        .buildCmd("forceLogOut")
                        .buildReason("您的账号在其他地方登录")
                        .build();
                break;
            case GOODBYE_DISCONNECTED:
                packet = new MyPacketBuilder()
                        .buildFunc(MyPacket.FUNC_DISCONNECT)
                        .buildType("disconnect")
                        .buildCmd("goodbye")
                        .build();
                break;
        }
        if (packet != null) {
            send(packet);
            table.remove(this.uid, this);
        }
    }

    //------------------------------Room Operation
    public void enter(@Nullable Room room) {
        try {
            MyPacket packet;
            //判断Player对象是否已在房间中
            if (this.getRoomId() != 0) {
                //已在其他房间中
                packet = enterRoom(MyPacket.STATE_FAILED, "您已在其他房间");

                System.out.printf("[%s][%d]进入房间[%d]%s\n", "UDP", this.getUid(), room.getRoomId(), "该玩家已在其他房间中");
                //检查是否通过new的方式进入的，如果是则删除这个多余的房间
                room.isRemovable();
            } else {
                //没有在任何一间房间中
                if (room == null) {//room为null,默认为创建房间
                    //新键房间
                    room = Room.newInstance();
                    logger.info(this.uid + "创建房间" + room.getRoomId());
                }

                if (room.include(this)) {
                    //成功进入
                    packet = enterRoom(MyPacket.STATE_SUCCESSFUL, null);
                    //向进入的玩家发送房间信息和房间内的所有玩家发送消息
                    System.out.printf("[%s][%d]进入房间[%d]%s\n", "UDP", this.getUid(), room.getRoomId(), "successful");
                } else {
                    //不能进入，已满
                    packet = enterRoom(MyPacket.STATE_FAILED, "full room");
                    System.out.printf("[%s][%d]进入房间[%d]%s\n", "UDP", this.getUid(), room.getRoomId(), "full room");
                }
            }
            send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 退出房间
     *
     * @return
     */
    public boolean drop() {
        //判断玩家是否加入过房间
        if (this.getRoomId() == 0) {
            //玩家未加入过房间
            return false;
        } else {
            //玩家加入过房间
            Room curRoom = Room.getRoom(getRoomId());
            //退出当前房间
            try {
                if (curRoom.exclude(this)) {
                    System.out.println("退出了");
                    //成功退出
                    send(RoomService.exitSuccessful());
                } else {
                    //未加入过任何房间
                    send(RoomService.exitFailed("您不在任何房间，无法退出"));
                    //补发同步包();
                    send(RoomService.exitSuccessful());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
    }
    //--------------------------------------Game operation

    private void cast(int index) {
        Game game = Room.getRoom(this.getRoomId()).getRunningGame();
        if (game != null) {
            //有游戏进行中
            Gamer gamer = this.gamer;
            gamer.activate(index);
        } else {
            //该玩家所在房间没有游戏进行中
            logger.warn(this.uid + "操作discard():该玩家所在房间没有游戏进行");
        }
    }


    private void discard(int index) {
        Game game = Room.getRoom(this.getRoomId()).getRunningGame();
        if (game != null) {
            //有游戏进行中
            Gamer gamer = this.gamer;
            gamer.discard(index);
        } else {
            //该玩家所在房间没有游戏进行中
            logger.warn(this.uid + "操作discard():该玩家所在房间没有游戏进行");
        }
    }

}
