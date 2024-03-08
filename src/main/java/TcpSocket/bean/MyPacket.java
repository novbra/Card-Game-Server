package TcpSocket.bean;

import org.json.simple.JSONObject;


/**
 * @author Liangpeng Yan(Nickname:Mr.Independent)
 * @date 2022/11/1 - 12:38
 */
public class MyPacket extends JSONObject {
    private JSONObject header;
    private int func;//必须设置，所以要给个默认，如果不设置 默认为FUNC类型
    private String token;
    private int seq;//必须设置，如果不设置 默认为0
    private int ack;//必须设置，如果不设置 默认为 HAS_SENT
    private int UID;//发出去必须要写UID
    private JSONObject body;
    private String type;//FUNC类型必须设置，没有默认值
    private String cmd;//
    private int state;
    private Object result;
    private String wrong;
    private String reason;
    public static MyPacket parse(JSONObject json) throws Exception {
        if(json!=null) {
            MyPacketBuilder pb=new MyPacketBuilder();
            JSONObject header = (JSONObject) json.get("header");
            JSONObject body = (JSONObject) json.get("body");
            if(header==null){
                pb.buildHeader();
            }else{
                pb.setHeader(header);
                int func=header.get("func")==null?0:(int)(long)header.get("func");
                String token=header.get("token")==null?null:(String)header.get("token");
                int ack=header.get("ack")==null?0:(int)(long)header.get("ack");
                int seq=header.get("seq")==null?0:(int)(long)header.get("seq");
                int UID=header.get("UID")==null?0:(int)(long)header.get("UID");
                pb.buildFunc(func)
                        .buildToken(token)
                        .buildAck(ack)
                        .buildSeq(seq)
                        .buildUID(UID);
            }

            if(body==null){
                pb.buildBody();
            }else{
                pb.setBody(body);
                String type=body.get("type")==null?"null": (String) body.get("type");
                String cmd=body.get("cmd")==null?null: (String) body.get("cmd");;
                int state=body.get("state")==null?-2: (int)(long) body.get("state");;
                Object result=body.get("result")==null?null: (Object) body.get("result");;
                String wrong=body.get("wrong")==null?null: (String) body.get("wrong");;
                String reason=body.get("reason")==null?null: (String) body.get("reason");;
                pb.buildType(type)
                        .buildCmd(cmd)
                        .buildState(state)
                        .buildResult(result)
                        .buildWrong(wrong)
                        .buildReason(reason);
            }
            return pb.build();
        }else{
            //为空抛出异常
            throw new Exception("参数为空");
        }
    }

    public void createHeader(){
        if(this.header==null){
            setHeader(new JSONObject());
        }
    }
    public void setHeader(JSONObject header) {
        this.header = header;
        this.put("header",header);
    }
    public static final int FUNC_LOGIN=1;
    public static final int FUNC_HEART=2;
    public static final int FUNC_CHECK=3;
    public static final int FUNC_FUNC=4;
    public static final int FUNC_DISCONNECT=5;
    public void setFunc(int func){
        this.func=func;
        createHeader();
        header.put("func",func);
    }

    public void setToken(String token) {
        this.token = token;
        createHeader();
        header.put("token",token);
    }

    public static final int HAS_RECEIVED=1;
    public static final int HAS_SENT=0;
    public void setAck(int ack) {
        this.ack = ack;
        createHeader();
        header.put("ack",ack);
    }

    public void setSeq(int seq) {
        this.seq = seq;
        createHeader();
        header.put("seq",seq);
    }

    public void setUID(int UID) {
        this.UID = UID;
        createHeader();
        header.put("UID",UID);
    }
    public void createBody(){
        if(this.body==null){
            setBody(new JSONObject());
        }
    }
    public void setBody(JSONObject body) {
        this.body = body;
        this.put("body",body);
    }


    public void setType(String type) {
        this.type = type;
        createBody();
        body.put("type",type);
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
        createBody();
        body.put("cmd",cmd);
    }

    public static final int STATE_SUCCESSFUL=0;
    public static final int STATE_FAILED=-1;
    public void setState(int state){
        this.state=state;
        createBody();
        body.put("state",state);
    }

    public void setResult(Object result) {
        this.result = result;
        createBody();
        body.put("result",result);
    }

    public void setWrong(String wrong) {
        this.wrong = wrong;
        createBody();
        body.put("wrong",wrong);
    }

    public void setReason(String reason) {
        this.reason = reason;
        createBody();
        body.put("reason",reason);
    }

    public void putExtra(String name,Object value){
        createBody();
        body.put(name,value);
    }

    public JSONObject getHeader() {
        return header;
    }

    public JSONObject getBody() {
        return body;
    }

    public int getFunc() {
        return func;
    }

    public String getToken() {
        return token;
    }

    public int getAck() {
        return ack;
    }

    public int getSeq() {
        return seq;
    }

    public int getUID() {
        return UID;
    }

    public String getType() {
        return type;
    }

    public String getCmd() {
        return cmd;
    }

    public int getState() {
        return state;
    }

    public Object getResult() {
        return result;
    }

    public String getWrong() {
        return wrong;
    }

    public String getReason() {
        return reason;
    }

    public Object getExtra(String key){
        return body.get(key);
    }
}
