package TcpSocket.services;


import TcpSocket.bean.MyPacket;
import TcpSocket.bean.MyPacketBuilder;
import com.sun.xml.internal.ws.api.message.Packet;

/**
 * @author Liangpeng Yan(Nickname:Mr.Independent)
 * @date 2022/11/1 - 18:35
 */
public class MsgSystem {
    private static  MsgSystem.Encoder encoder=new MsgSystem.Encoder();
    private static final String TYPE="msg";

    public static MsgSystem.Encoder getEncoder(){
        return encoder;
    }

    public static class Encoder{
        private Encoder() {
        }

        public MyPacket encode(String cmd,int UIDFrom, String nameFrom, String msg) throws Exception {
            return new MyPacketBuilder()
                    .buildType(TYPE)
                    .buildCmd(cmd)
                    .buildExtra("UIDFrom",UIDFrom)
                    .buildExtra("nameFrom",nameFrom)
                    .buildExtra("content",msg)
                    .build();
        }
    }
    public static class PacketService{
        public static MyPacket wrongInfo(String wrong) throws Exception {
            return new MyPacketBuilder().buildType(TYPE)
                    .buildCmd("state")
                    .buildState(MyPacket.STATE_FAILED)
                    .buildWrong(wrong)
                    .build();
        }
        public static MyPacket positiveInfo() throws Exception {
            return new MyPacketBuilder().buildType(TYPE)
                    .buildCmd("state")
                    .buildState(MyPacket.STATE_SUCCESSFUL)
                    .build();
        }
    }
}
