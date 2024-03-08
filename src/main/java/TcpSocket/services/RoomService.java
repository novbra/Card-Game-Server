package TcpSocket.services;

import TcpSocket.bean.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * @author Liangpeng Yan(Nickname:Mr.Independent)
 * @date 2022/11/1 - 16:21
 */
public class RoomService {
    private static final String TYPE="room";


    /**
     * 通知房间内所有玩家更新数据
     *
     * @param room 房间
     */
    public static MyPacket updateRoomInfo(Room room) throws Exception {

        MyPacket packet= new MyPacketBuilder()
                .buildType(TYPE)//由原先改动为room
                .buildCmd("update room info")
                .buildExtra("room",room.toJSONObject())
                .build();
        return packet;
    }

    public static MyPacket enterRoom (int state,String wrong) throws Exception {
        MyPacket packet= new MyPacketBuilder()
                .buildType(TYPE)//由原先改动为room
                .buildCmd("enter")
                .buildState(state)
                .buildWrong(wrong)
                .build();
        return packet;
    }



    public static MyPacket informGameStarting(Room room) throws Exception {
        return new MyPacketBuilder()
                .buildType(TYPE)//由原先改动为room
                .buildCmd("start")
                .buildState(MyPacket.STATE_SUCCESSFUL)
                .buildExtra("room",room.toJSONObject())
                .build();
    }
    public static MyPacket informPlayersUnready(Room room) throws Exception {
        MyPacket packet= new MyPacketBuilder()
                .buildType(TYPE)//由原先改动为room
                .buildCmd("start")
                .buildState(MyPacket.STATE_FAILED)
                .buildWrong("玩家尚未准备")
                .build();
        return packet;
    }



    public static MyPacket offerAllRoomsInfo() throws Exception {

        //获取房间数组
        JSONArray roomArray = new JSONArray();
        Room.getAllRooms().forEach((id, room) -> {
            roomArray.add(room.toJSONObject());
        });

        return new MyPacketBuilder()
                .buildType(TYPE)//由原先改动为room
                .buildCmd("offerAllRoomsInfo")
                .buildExtra("rooms",roomArray)
                .build();

    }

    //ROOM
    public static MyPacket exitRoom(int state,String wrong) throws Exception {
        MyPacket packet= new MyPacketBuilder()
                .buildType(TYPE)//由原先改动为room
                .buildCmd("exit")
                .buildState(state)
                .buildWrong(wrong)
                .build();
        return packet;
    }


    public static MyPacket exitFailed(String wrong) throws Exception {
        MyPacket packet= new MyPacketBuilder()
                .buildType(TYPE)//由原先改动为room
                .buildCmd("exit")
                .buildState(MyPacket.STATE_FAILED)
                .buildWrong(wrong)
                .build();
        return packet;
    }

    public static MyPacket exitSuccessful() throws Exception {
        MyPacket packet= new MyPacketBuilder()
                .buildType(TYPE)//由原先改动为room
                .buildCmd("exit")
                .buildState(MyPacket.STATE_SUCCESSFUL)
                .build();
        return packet;
    }
}
