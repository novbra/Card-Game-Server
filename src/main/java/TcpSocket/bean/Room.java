package TcpSocket.bean;

import TcpSocket.LoggerUtil;
import TcpSocket.TcpSocketSever;
import TcpSocket.UDPSever;
import TcpSocket.observer.RoomStateObserver;
import TcpSocket.services.RoomService;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * @author Mr.Independent
 * @date 2022/6/8 - 13:02
 */
public class Room {
    private static Logger logger = LoggerUtil.getLogger(TcpSocketSever.class);
    private static HashMap<Integer, Room> table = new HashMap<>();

    public static boolean contain(int roomId) {
        return table.containsKey(roomId);
    }

    public static Room getRoom(int roomId) {
        return table.get(roomId);
    }

    public static HashMap<Integer, Room> getAllRooms() {
        return table;
    }

    public static void remove(int id) {
        table.remove(id);
        System.out.printf("[%s]roomId%d%s\n", "ROOM", id, "已被删除");
    }

    public static Room toRoom(JSONObject object) {
        int roomId = (int) (long) object.get("roomId");
        JSONObject playerA = (JSONObject) object.get("playerA");
        JSONObject playerB = (JSONObject) object.get("playerB");
        boolean isFull = (boolean) object.get("isFull");
        String s = (String) object.get("state");
        GameState state = GameState.NOT_READY;
        if (s.equals(GameState.READY.toString()))
            state = GameState.READY;
        else if (s.equals(GameState.NOT_READY.toString()))
            state = GameState.NOT_READY;
        else if (s.equals(GameState.PLAYING.toString()))
            state = GameState.PLAYING;
        Room roomForRead = new Room(roomId, null, null, isFull, state);

        if (playerA != null) {
            roomForRead.setPlayerA(Player.toPlayer(playerA));
        }
        if (playerB != null) {
            roomForRead.setPlayerB(Player.toPlayer(playerB));
        }
        return roomForRead;
    }

    public static Room newInstance() {
        return new Room();
    }

    enum GameState {READY, NOT_READY, PLAYING}

    private int roomId;//唯一
    private Player playerA;
    private Player playerB;
    private boolean isFull;//房间是否已满信号量
    private LinkedList<RoomStateObserver> observers = new LinkedList<>();//观察者对象列表
    private GameState state;
    public Game runningGame = null;

    /**
     * 服务端唯一构造方法
     */
    public Room() {
        init();
    }

    public Room(int roomId, Player playerA, Player playerB, boolean isFull, GameState state) {
        this.roomId = roomId;
        this.playerA = playerA;
        this.playerB = playerB;
        this.isFull = isFull;
        this.state = state;
    }

    public boolean isFull() {
        return isFull;
    }

    public boolean contain(Player player) {
        if (player == playerA || player == playerB) {
            return true;
        } else
            return false;
    }

    public GameState getState() {
        return state;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setPlayerA(Player playerA) {
        this.playerA = playerA;
    }

    public void setPlayerB(Player playerB) {
        this.playerB = playerB;
    }

    public JSONObject toJSONObject() {
        JSONObject temp = new JSONObject();
        temp.put("roomId", roomId);
        if (playerA != null)
            temp.put("playerA", playerA.toJSONObject());
        if (playerB != null)
            temp.put("playerB", playerB.toJSONObject());
        temp.put("isFull", isFull);
        temp.put("state", state.toString());
        return temp;

    }

    @Override
    public String toString() {
        return "Room{" +
                "roomId=" + roomId +
                ", playerA=" + playerA +
                ", playerB=" + playerB +
                ", isFull=" + isFull +
                ", state=" + state +
                '}';
    }

    void init() {
        int id;
        for (id = 10000; table.containsKey(id); id++) {
        }
        table.put(id, this);
        roomId = id;
        isFull = false;
        state = GameState.NOT_READY;
    }

    public boolean include(Player player) {
        if (playerA == null) {
            playerA = player;
            player.setRoomId(roomId);//设置房间号
            player.initState();//初始化为未准备
            addObserver(player.getRoomStateObserver());
            renewRoomState();
            return true;
        }
        if (playerB == null) {
            playerB = player;
            player.setRoomId(roomId);//设置房间号
            player.initState();//初始化为未准备
            addObserver(player.getRoomStateObserver());
            renewRoomState();
            return true;
        }
        return false;
    }

    public boolean exclude(Player player) {
        if (playerA == player) {
            playerA = null;
            isFull = false;
            player.setRoomId(0);
            removeObserver(player.getRoomStateObserver());
            this.isRemovable();
            renewRoomState();
            //正在游戏中，有玩家强退
            if (runningGame != null) {
                runningGame.close(player);
            }
            return true;
        }
        if (playerB == player) {
            playerB = null;
            isFull = false;
            player.setRoomId(0);
            removeObserver(player.getRoomStateObserver());
            this.isRemovable();
            renewRoomState();

            //正在游戏中，有玩家强退
            if (runningGame != null) {
                runningGame.close(player);
            }
            return true;
        }
        return false;
    }

    /**
     * isRemovable 如果可以删除的就会删除并返回真，不能删除的话返回假
     * 在Room表中删除，但没有立即删除对象
     * @return
     */
    public boolean isRemovable() {
        if (playerA == null && playerB == null) {
            Room.remove(this.roomId);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 更新房间状态, 调用情景(玩家准备/取消,玩家进入，玩家退出，玩家准备状态初始化）
     *
     * @return
     */
    public void renewRoomState() {
        //更新是否满员
        if (playerA != null && playerB != null) {
            this.isFull = true;
        } else {
            this.isFull = false;
            state = GameState.NOT_READY;

        }
        //更新能不能开始游戏
        if (isFull) {
            if (playerA.isReady() && playerB.isReady()) {
                //转换房间状态为READY
                state = GameState.READY;
                //双人准备即直接游戏模式
                try {
                    startGame();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("无法开始游戏");
                } finally {

                }
            } else {
                //转换房间状态为NOT_READY
                state = GameState.NOT_READY;
            }
        }
        //通知所有观察者，房间状态更新
        for (RoomStateObserver observer : observers) {
            try {
                observer.roomStateChange(RoomService.updateRoomInfo(this));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 初始化玩家准备状态
     */
    public void initPlayerState() {
        //初始化房间内所有玩家的状态
        //中途可能有人会退游戏,所以玩家不一定是齐的 判断当前玩家是否为空
        if (playerA != null) {
            playerA.initPlayerState();
        }
        if (playerB != null) {
            playerB.initPlayerState();
        }
        renewRoomState();
    }

    /**
     * 启动游戏
     *
     * @throws Exception
     */
    public void startGame() throws Exception {

        if (getRunningGame() != null) {
            //该房间游戏已经开始
        } else {
            Game game = initGame();
            if (game != null) {
                //向玩家发送开始游戏包
                sendRoomers(RoomService.informGameStarting(this));
                //开始游戏
                game.start();
            } else {
                //玩家尚未准备
                sendRoomers(RoomService.informPlayersUnready(this));
            }
        }
    }

    public Game initGame() {
        if (state == GameState.READY) {
            //设置房间状态为游戏中
            state = GameState.PLAYING;
            runningGame = new Game(this);
            return runningGame;
        } else
            return null;//正在游戏中和房间未准备都返回null
    }

    public void destroyGame() {
        //设置原先房间runningGame为null
        this.runningGame = null;
        //设置原先房间中所有玩家状态为未准备
        this.initPlayerState();
    }

    public Game getRunningGame() {
        return runningGame;
    }

    public ArrayList<Player> getAllPlayer() {
        ArrayList<Player> playersList = new ArrayList<>();
        if (playerA != null) {
            playersList.add(playerA);
        }
        if (playerB != null) {
            playersList.add(playerB);
        }
        return playersList;
    }


    /**
     * 添加房间观察者
     *
     * @param o
     */
    public void addObserver(RoomStateObserver o) {
        this.observers.add(o);
    }

    /**
     * 删除房间观察者
     *
     * @param o
     */
    public void removeObserver(RoomStateObserver o) {
        this.observers.remove(o);
        System.out.println("删除了");
    }

    /**
     * 向房间内所有玩家发送特定封包
     *
     * @param packet
     */
    public void sendRoomers(MyPacket packet) {
        for (Player thisPlayer : this.getAllPlayer()) {
            thisPlayer.send(packet);
        }
    }
}
