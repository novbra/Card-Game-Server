package TcpSocket.bean;

/**
 * @author Mr.Independent
 * @date 2022/6/24 - 21:54
 */
public class Pokemon {
    private short id;
    private String type;//属性
    private short[] ss;//种族值
    private short[] bp;//努力值
    private int nature;//性格
    private short[] stat;//能力值

    public Pokemon(short id, int nature) {
        this.id = id;//设置全国图鉴id
        this.nature = nature;//设置性格
        init();//设置属性，种族值，努力值
        calcStat();//计算能力值
    }

    private void calcStat() {
    }

    private void init() {
        ss=new short[6];
        stat=new short[6];
        //初始化努力值
        bp= new short[]{0, 0, 0, 0, 0, 0};
    }
}
