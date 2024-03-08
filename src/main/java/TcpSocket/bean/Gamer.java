package TcpSocket.bean;

import TcpSocket.LoggerUtil;
import TcpSocket.TcpSocketSever;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Properties;

import static TcpSocket.UDPSever.*;

/**
 * @author Liangpeng Yan(Nickname:Mr.Independent)
 * @date 2022/9/25 - 15:35
 */
public class Gamer {
    private static Logger logger = LoggerUtil.getLogger(TcpSocketSever.class);
    //固定参数
    private static Properties props = null;
    private static final String LOCAL_PATH = LoggerUtil.dir;//系统路径
    public static String SYSPROPFILE = LOCAL_PATH + "properties\\gamer.properties"; //系统配置文件位置

    //配置属性
    public static int DISCARD_RESTORE_AMOUNT_ENERGY = 2;
    private static int MAX_CARD_AMOUNT = 5;
    private static int MAX_HP = 100;
    private static int MAX_ENERGY = 20;
    private static int SHIELD = 0;

    static {
        initParams();
    }

    private static void initParams() {
        props = new Properties();
        Reader reader = null;
        try {
            logger.info("——————————————————————————————【Gamer参数配置信息】——————————————————————————————");
            logger.info("SYSPROPFILE:" + SYSPROPFILE);
            reader = new InputStreamReader(new FileInputStream(SYSPROPFILE), StandardCharsets.UTF_8);
            props.load(reader);
            DISCARD_RESTORE_AMOUNT_ENERGY = Integer.parseInt(props.getProperty("DISCARD_RESTORE_AMOUNT_ENERGY", "2"));
            MAX_CARD_AMOUNT = Integer.parseInt(props.getProperty("MAX_CARD_AMOUNT", "5"));
            MAX_HP = Integer.parseInt(props.getProperty("MAX_HP", "100"));
            MAX_ENERGY = Integer.parseInt(props.getProperty("MAX_ENERGY", "20"));
            SHIELD = Integer.parseInt(props.getProperty("SHIELD", "0"));
            //日志打印配置信息
            logger.info("DISCARD_RESTORE_AMOUNT_ENERGY:" + DISCARD_RESTORE_AMOUNT_ENERGY);
            logger.info("MAX_CARD_AMOUNT:" + MAX_CARD_AMOUNT);
            logger.info("MAX_HP:" + MAX_HP);
            logger.info("MAX_ENERGY:" + MAX_ENERGY);
            logger.info("SHIELD:" + SHIELD);

        } catch (Exception e) {
            logger.info("Gamer参数配置异常，采用默认配置：" + e.getMessage(), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        logger.info("——————————————————————————————（Gamer配置结束）——————————————————————————————");
    }

    Game game;
    public Player player;
    int hp;//生命值
    int shield;//全能盾
    int energy;//能量值
    HashMap<Integer, CardUtil> hand;//手牌
    private int cardCount;//手牌数
    boolean is_turn;//是否回合

    public Gamer(Player player, Game game) {
        this.player = player;
        this.game = game;

        init();
    }

    void init() {
        this.hp = MAX_HP;
        this.shield = SHIELD;
        this.energy = MAX_ENERGY;
        hand = new HashMap<>();
        is_turn = false;
    }

    public void activate(int cardIndex) {
        try {
            activate_inner(hand.get(cardIndex));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cardOut(CardUtil cardUtil) {
        cardUtil.out();
        try {
            //更新自身卡牌
            player.send(deliverCardsInformation());
            //展示卡牌给对手
            player.send(showOpponentCard(cardUtil.card));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 激活卡牌
     *
     * @param cardUtil
     */
    private void activate_inner(CardUtil cardUtil) throws Exception {
        if (!is_turn) {
            //不是你的回合
            deliverCastInformation(MyPacket.STATE_FAILED, "不是你的回合，无法激活任何卡牌");
            System.out.println("不是你的回合,无法激活任何卡牌");
            return;
        }
        Card card = cardUtil.card;
        if (this.energy >= card.consumption) {
            //消耗能量值
            this.energy -= card.consumption;
            this.game.notifyAllPlayer(this);
        } else {
            //能量值不够
            deliverCastInformation(MyPacket.STATE_FAILED, "能量值不够");
            System.out.println("能量值不够");
            return;
        }
        int amount = card.getWeight();
        //判断func
        switch (card.func) {
            case 0:
                //血包
                restoreHp(amount);
                break;
            case 1:
                //护盾
                getShield(amount);
                break;
            case 2:
                //攻击
                attack(amount, getPeer());
                break;
        }
        //卡牌设置为不可用
        cardOut(cardUtil);
        //通知当前回合玩家卡牌已激活
        deliverCastSuccessfulInformation(cardUtil);//后期删除card类
    }

    /**
     * 弃牌
     *
     * @param index
     */
    public void discard(int index) {
        restoreEnergy(DISCARD_RESTORE_AMOUNT_ENERGY);
        cardOut(hand.get(index));
        System.out.println("完成弃牌操作");
    }

    /**
     * 结束回合
     */
    public void finish() {
        //判断手牌数
        if (getCardCount() > MAX_CARD_AMOUNT) {
            //无法结束回合，必须进行弃牌操作
            try {
                deliverPleaseDiscard();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        ;
        game.hasFinished(this);
    }

    /**
     * 恢复血量
     */
    private void restoreHp(int amount) {
        this.hp += amount;
        if (this.hp > MAX_HP) {
            this.hp = MAX_HP;
        }
        this.game.notifyAllPlayer(this);
    }

    public void restoreEnergy(int amount) {
        this.energy += amount;
        if (this.energy > MAX_ENERGY) {
            this.energy = MAX_ENERGY;
        }
        this.game.notifyAllPlayer(this);
    }

    /**
     * 攻击
     *
     * @param amount
     */
    private void attack(int amount, Gamer gamer) {
        gamer.getHurt(amount);
    }

    /**
     * 受到伤害
     */
    private void getHurt(int amount) {
        //护盾优先扣除
        if (shield > 0) {
            int leftover = shield - amount;
            if (leftover < 0) {
                //护盾全部扣除
                this.shield = 0;
                //生命值扣除
                this.hp -= Math.abs(leftover);
            } else {
                //扣除部分护盾
                this.shield = leftover;
            }
        } else
            this.hp -= amount;//生命值扣除

        if (hp <= 0) {
            this.hp = 0;
            //玩家阵亡事件通知结束游戏
            game.close(player);
        }

        this.game.notifyAllPlayer(this);
    }

    /**
     * 获得护盾
     */
    private void getShield(int amount) {
        this.shield += amount;
        this.game.notifyAllPlayer(this);
    }

    public int getCardCount() {
        cardCount = 0;
        hand.forEach((i, cardUtil) -> {
            if (cardUtil.isAvailable()) {
                cardCount++;
            }
        });
        return cardCount;
    }

    public Gamer getPeer() {
        for (Gamer gamer : game.gamers) {
            if (gamer == this) {

            } else {
                return gamer;
            }
        }
        return null;
    }


    //信息服务
    public MyPacket deliverCardsInformation() throws Exception {
        MyPacket packet = new MyPacketBuilder().buildType("game")
                .buildCmd("deal")
                .build();
        //获取卡牌数组
        JSONArray cardUtils = new JSONArray();
        hand.forEach((index, cardUtil) -> {
            //只提供给客户端有效卡牌
            if (cardUtil.isAvailable()) {
                cardUtils.add(cardUtil.toJSONObject());
            }
        });

        packet.putExtra("cards", cardUtils);
        return packet;
    }

    public MyPacket deliverPleaseDiscard() throws Exception {
        MyPacket packet = new MyPacketBuilder().buildType("game")
                .buildCmd("pleaseDiscard")
                .build();
        return packet;
    }

    public MyPacket deliverCastInformation(int state, String wrong) throws Exception {
        MyPacket packet = new MyPacketBuilder().buildType("game")
                .buildCmd("cast")
                .buildState(state)
                .buildWrong(wrong)
                .build();
        return packet;
    }

    private MyPacket deliverCastSuccessfulInformation(CardUtil cardUtil) throws Exception {
        MyPacket packet = new MyPacketBuilder().buildType("game")
                .buildCmd("cast")
                .buildState(MyPacket.STATE_SUCCESSFUL)
                .build();
        packet.putExtra("cardUtil", cardUtil.toJSONObject());
        return packet;
    }

    public MyPacket showOpponentCard(Card card) throws Exception {
        MyPacket packet = new MyPacketBuilder().buildType("game")
                .buildCmd("showOpponentCard")
                .build();
        packet.putExtra("peer's card", card.toJSONObject());
        return packet;
    }

    public MyPacket getPropertyPacket() throws Exception {
        return new MyPacketBuilder().buildType("game")
                .buildCmd("updateProperty")
                .buildExtra("UID", player.getUid())
                .buildExtra("hp", hp)
                .buildExtra("MAX_HP", MAX_HP)
                .buildExtra("shield", this.shield)
                .buildExtra("INITIAL_SHIELD", SHIELD)
                .buildExtra("energy", this.energy)
                .buildExtra("MAX_ENERGY", MAX_ENERGY)
                .buildExtra("cardCount", cardCount)
                .build()
                ;
    }

}
