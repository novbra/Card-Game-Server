package TcpSocket.bean;

import java.util.HashMap;

/**
 * @author Mr.Independent
 * @date 2022/6/9 - 22:22
 */
public class PlayerRecord {
    private static final int MAX_HP=5;
    public int hpA;
    public int hpB;
    public boolean ok_A;
    public boolean ok_B;
    public HashMap<Integer,CardUtil> cardsA;//A手中的卡牌
    public HashMap<Integer,CardUtil> cardsB;//B手中的卡牌
    public Card card_A;//引用
    public Card card_B;//引用

    public PlayerRecord() {
        //初始化双方血量、
        this.hpA = MAX_HP;
        this.hpB = MAX_HP;
        this.ok_A = false;
        this.ok_B = false;
        this.cardsA = new HashMap<>();
        this.cardsB = new HashMap<>();
    }

    /**
     * 准备下一回数据
     */
    public void nextRound(){
        this.ok_A=false;
        this.ok_B=false;

    }
}
