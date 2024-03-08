package TcpSocket.services;

import TcpSocket.MySQLDBUtil;
import TcpSocket.bean.MyPacket;
import TcpSocket.bean.MyPacketBuilder;
import TcpSocket.bean.Player;

/**
 * @author Liangpeng Yan(Nickname:Mr.Independent)
 * @date 2022/11/1 - 16:25
 */
public class PersonService {
    private static final String TYPE="personal";

    public static MyPacket getName(Player player) throws Exception {
        MyPacket packet= new MyPacketBuilder()
                .buildType(TYPE)
                .buildCmd("getName")
                .buildExtra("name",player.getName())
                .build();
        return packet;
    }
    public static MyPacket setName(int state, String wrong) throws Exception {
        MyPacket packet= new MyPacketBuilder()
                .buildType(TYPE)
                .buildCmd("setName")
                .buildState(state)
                .buildWrong(wrong)
                .build();
        return packet;
    }

    public static MyPacket deliverRank(Player p) throws Exception {
        if (p!= null) {
            int rank = MySQLDBUtil.getRank((int) p.getUid());
            MyPacket packet= new MyPacketBuilder()
                    .buildType(TYPE)
                    .buildCmd("getRank")
                    .buildExtra("rank",rank)
                    .build();
            //获取数据库中rank分
            return packet;
        } else {
            //玩家不存在
            throw new Exception("传入的玩家参数为null");
        }

    }
}
