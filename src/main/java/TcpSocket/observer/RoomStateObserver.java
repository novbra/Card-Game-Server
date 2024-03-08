package TcpSocket.observer;

import TcpSocket.bean.MyPacket;
import TcpSocket.bean.Player;

/**
 * @author Liangpeng Yan(Nickname:Mr.Independent)
 * @date 2022/11/1 - 17:39
 */
public class RoomStateObserver {
    private Player player;

    public RoomStateObserver(Player player) {
        this.player = player;
    }

    public void roomStateChange(MyPacket myPacket){
        //通知玩家
        if(Player.isOnline(player.getUid())) {
            player.send(myPacket);
        }
    }
}
