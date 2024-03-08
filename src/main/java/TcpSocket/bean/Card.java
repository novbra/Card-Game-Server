package TcpSocket.bean;

import TcpSocket.MySQLDBUtil;
import org.json.simple.JSONObject;
import org.omg.CORBA.PRIVATE_MEMBER;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

/**
 * @author Mr.Independent
 * @date 2022/6/9 - 20:30
 */
public class Card implements  Cloneable{
    static HashMap<Integer,Card> map=new HashMap<>();//卡牌册
    private int id;//
    private int type;//一般:0 火：1 水：2 电：3 草：4 冰：5 超能:10
    public int func;//特防 物防 特攻 攻击 全能盾 血包
    private int weight;//点数
    public int consumption;
    public String description;
    public String name;

    /**
     * 更新卡牌册
     */
    public static void updateCards(){
        ResultSet rs=MySQLDBUtil.getCards();
        try{
            do{
                int index=1;
                int id=rs.getInt(index++);
                int type=rs.getInt(index++);
                int func=rs.getInt(index++);
                int weight=rs.getInt(index++);
                int consumption=rs.getInt(index++);
                boolean isAvailable=rs.getBoolean(index++);//后续使用
                String name=rs.getString(index++);
                String description=rs.getString(index);
                map.put(id,new Card(id,type,func,weight,consumption,name,description));
            }while(rs.next());
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return (Card)super.clone();
    }

    public Card(int id, int type, int func, int weight, int consumption,String name, String description) {
        this.id = id;
        this.type = type;
        this.func = func;
        this.weight = weight;
        this.consumption = consumption;
        this.name=name;
        this.description = description;
    }

    public int getType() {
        return type;
    }

    public int getWeight() {
        return weight;
    }

    public static Card toCard(JSONObject o){
        int id= (int)(long) o.get("id");
        int type=(int)(long)o.get("type");
        int func=(int)(long)o.get("func");
        int weight=(int)(long)o.get("weight");
        int consumption=(int)(long)o.get("consumption");
        String name=(String)o.get("name");
        String description= (String) o.get("description");
        return new Card(id,type,func,weight,consumption,name,description);
    }
    public JSONObject toJSONObject(){
        JSONObject object=new JSONObject();
        object.put("id",id);
        object.put("type",type);
        object.put("func",func);
        object.put("weight",weight);
        object.put("consumption",consumption);
        object.put("name",name);
        object.put("description",description);
        return object;
    }
    public Card compare(Card that){
        //无敌属性

        if(this.type==4&&that.type==4){
            //双方都是无敌卡
            return null;//null 平手
        } else if(this.type==4||that.type==4){
            //一方是无敌卡
            return this.type==4?this:that;
        }
        //排出掉无敌卡的情况，即没有4的情况下 对3的判断
        if(this.type!=3&&that.type!=3) //可以访问到，不需要写get
        {
            //两个属性卡的判断
           if((this.type+1)%3==that.type){
               //前者克制后者
               return this;
           }else if(this.type==that.type){
               //前者与后者属性等同
               if(this.weight>that.weight){
                   //前者点数大于后者
                   return this;
               }else if (this.weight==that.weight){
                   //前者与后者点数相同
                   return null;
               }else{
                   //后者点数大于前者
                   return that;
               }
           }else{
               //后者克制前者
               return that;
           }
        }else{
            //属性卡和一般卡的比较，一般卡和一般卡的比较
            if(this.weight>that.weight){
                //前者点数大于后者
                return this;
            }else if (this.weight==that.weight){
                //前者与后者点数相同
                return null;
            }else{
                //后者点数大于前者
                return that;
            }
        }
        //大小比较
    }


    public Card(int type, int weight) {
        this.type = type;
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "Card{" +
                "id=" + id +
                ", weight=" + weight +
                ", consumption=" + consumption +
                ", description='" + description + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
