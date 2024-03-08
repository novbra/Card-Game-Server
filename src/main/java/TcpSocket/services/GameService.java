package TcpSocket.services;

import TcpSocket.bean.*;
import org.json.simple.JSONObject;

/**
 * @author Liangpeng Yan(Nickname:Mr.Independent)
 * @date 2022/11/1 - 16:20
 */
public class GameService {
    private final static String TYPE="game";
    /**
     * 通知回合结束
     */
    public static MyPacket informTurnIsFinished() throws Exception {
        return new MyPacketBuilder().buildType(TYPE)
                .buildCmd("finish")
                .buildState(MyPacket.STATE_SUCCESSFUL)
                .build();
    }

    public static MyPacket informThisIsYourTurn() throws Exception {
        return new MyPacketBuilder().buildType(TYPE)
                .buildCmd("turn")
                .build();
    }




    public static MyPacket informOk() throws Exception {
        return new MyPacketBuilder().buildType(TYPE)
                .buildCmd("ok")
                .build();
    }
}
