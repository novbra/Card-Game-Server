package TcpSocket.bean;


import org.json.simple.JSONObject;

/**
 * @author Liangpeng Yan(Nickname:Mr.Independent)
 * @date 2022/11/1 - 12:45
 */
public class MyPacketBuilder {
    private MyPacket myPacket=new MyPacket();
    public MyPacketBuilder setHeader(JSONObject header){
        myPacket.setHeader(header);
        return this;
    }
    public MyPacketBuilder buildHeader(){
        myPacket.createHeader();
        return this;
    }
    public MyPacketBuilder buildFunc(int func){
        myPacket.setFunc(func);
        return this;
    }
    public MyPacketBuilder buildToken(String token) {
        myPacket.setToken(token);
        return this;
    }
    public MyPacketBuilder buildAck(int ack){
        myPacket.setSeq(ack);
        return this;
    }

    public MyPacketBuilder buildSeq(int seq){
        myPacket.setSeq(seq);
        return this;
    }
    public MyPacketBuilder buildUID(int UID){
        myPacket.setUID(UID);
        return this;
    }

    public MyPacketBuilder buildBody(){
        myPacket.createBody();
        return this;
    }
    public MyPacketBuilder setBody(JSONObject body){
        myPacket.setBody(body);
        return this;
    }

    public MyPacketBuilder buildType(String type){
        myPacket.setType(type);
        return this;
    }
    public MyPacketBuilder buildCmd(String cmd){
        myPacket.setCmd(cmd);
        return this;
    }
    public MyPacketBuilder buildState(int state){
        myPacket.setState(state);
        return this;
    }
    public MyPacketBuilder buildResult(Object result){
        myPacket.setResult(result);
        return this;
    }
    public MyPacketBuilder buildWrong(String wrong){
        myPacket.setWrong(wrong);
        return this;

    }
    public MyPacketBuilder buildReason(String reason){
        myPacket.setReason(reason);
        return this;
    }
    public MyPacketBuilder buildExtra(String name,Object value){
        myPacket.putExtra(name,value);
        return this;
    }

    public MyPacket build() throws Exception {
        if(myPacket.getAck()==0){
            this.buildAck(MyPacket.HAS_SENT);
        }
        if(myPacket.getFunc()==0){
            this.buildFunc(MyPacket.FUNC_FUNC);
        }
        if(myPacket.getSeq()==0){
            this.buildSeq(0);
        }
        if(myPacket.getFunc()==MyPacket.FUNC_FUNC&&myPacket.getType()==null){
            throw new Exception("功能型封包，其type值不能为null");
        }
        return myPacket;
    }


}
