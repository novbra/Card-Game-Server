package TcpSocket.bean;

import org.json.simple.JSONObject;

import java.util.HashMap;

/**
 * @author Mr.Independent
 * @date 2022/6/9 - 21:44
 */
public class CardUtil{
    public int index;//手牌索引
    public Card card;
    private boolean isOut;
    public Card getCard() {
        return card;
    }

    @Override
    public String toString() {
        return "CardUtil{" +
                "index=" + index +
                ", card.name=" + card.name +
                ", card.consumption="+card.consumption+
                ", card.description="+card.description+
                '}';
    }

    public CardUtil(int index,Card card) {
        this.index=index;
        this.card=card;
        this.isOut=false;
    }
    public static CardUtil toCardUtil(JSONObject o){
        int index= (int) (long)o.get("index");
        Card card= Card.toCard((JSONObject) o.get("card"));
        return new CardUtil(index,card);
    }

    /**
     * 打出牌,将牌设置成打出状态,不可再用
     */
    public void out(){
        this.isOut=true;
    }
    public boolean isAvailable(){
        return !isOut;
    }

    public Object toJSONObject() {
        JSONObject o=new JSONObject();
        o.put("index",index);//手牌index
        o.put("card",this.getCard().toJSONObject());
        return o;
    }
}
