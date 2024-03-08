package TcpSocket.bean;

import TcpSocket.*;
import TcpSocket.services.GameService;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Stack;

/**
 * @author Mr.Independent
 * @date 2022/6/9 - 20:15
 */
public class Game {
    private static Logger logger = LoggerUtil.getLogger(TcpSocketSever.class);

    public static final int GAME_ID_BEGINING=20000;
    public static HashMap<Integer,Game> gameList=new HashMap<>();//正在进行中的游戏总表
    public static Stack<Card> cardStack;//卡牌堆
    public static final int FIRST_DEAL_TIMES=5;//首轮发牌3张
    public static final int RESTORE_ENERGY_AMOUNT_DEFAULT=8;
    public static final int MAXIMUM_ROUND_TIME=20;//20s
    public static final int INCREMENT=20;//一把加20分或一把扣20分
    /**
     * 连接数据库获取卡牌册,new新对象之前必须初始化卡堆
     */
    public static void initCardPack(){
        //初始化卡牌堆
        ArrayList<Card> cardPack=new ArrayList<>();
        //一副牌模式
        Card.map.forEach((i,c)->{
            cardPack.add(c);
        });
        for(int i=1;i<cardPack.size();i++){
            int index=randInt(1,cardPack.size()-1);
            Card temp=cardPack.get(index);
            cardPack.set(index,cardPack.get(i));
            cardPack.set(i,temp);
        }
        cardStack=new Stack<>();
        for (Card card : cardPack) {
            cardStack.push(card);
        }
    }
    /**
     * 置随机数方法
     * @param min
     * @param max
     * @return
     */
    static int randInt(int min,int max){
        int offset=max-min;
        return min+(int)(Math.random()*(offset+1));
    }

    private Room room;//原先房间
    private int gameId;
    private int roundCount;//游戏回合
    private int totalTurn;
    public Gamer[] gamers;

    public Room getRoom() {
        return room;
    }
    public Game(Room room) {
        setId();//设置游戏Id
        this.room=room;
        gameList.put(this.gameId,this);
        ArrayList players=room.getAllPlayer();
        gamers=new Gamer[players.size()];
        for (int i = 0; i < players.size(); i++) {
            Player player= (Player) players.get(i);
            Gamer gamer=new Gamer(player,this);
            gamers[i]=gamer;
            player.gamer=gamer;
        }
        this.roundCount = 0;//初始化为第0回合 即未开始
        this.totalTurn=0;
        initCardPack();//初始化卡牌堆
        for (Gamer gamer : gamers) {
            notifyAllPlayer(gamer);
        }
    }

    /**
     * Gamers属性通知
     */
    public void notifyAllPlayer(Gamer target){
        for (Gamer gamer : gamers) {
            MyPacket packet = null;
            try {
                packet = target.getPropertyPacket();
                gamer.player.send(packet);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("notifyAllPlayer(Gamer):报错");
            }
        }
    }

    /**
     * 发牌
     */
    private void deal(){
        Card card;
        if(roundCount==0){
            //首轮发牌
            for(int i=0;i<FIRST_DEAL_TIMES;i++){
                try {
                    for (Gamer gamer : gamers) {
                        if(cardStack.isEmpty()){//重置卡牌堆
                            initCardPack();
                        }
                        card=cardStack.pop();
                        gamer.hand.put(i,new CardUtil(i,card));
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            //通知玩家手中各自卡牌
            for (Gamer gamer : gamers) {
                try {
                    gamer.deliverCardsInformation();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            roundCount++;//第一回合开始
        }
    }
    private void deal(Gamer gamer){
            //正常发牌
            int index=FIRST_DEAL_TIMES+roundCount-1;
            try {
                if(cardStack.isEmpty()){//重置卡牌堆
                    initCardPack();
                }
                Card card=cardStack.pop();
                gamer.hand.put(index,new CardUtil(index,card));
            }catch (Exception e){
                e.printStackTrace();
            }
            totalTurn++;
            roundCount=1+totalTurn/2;
            //通知
        try {
            gamer.deliverCardsInformation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void setId(){
        int id=GAME_ID_BEGINING;
        for(;gameList.containsKey(id);id++){}
        this.gameId=id;
    }

    /**
     * 游戏开始方法
     */
    public void start(){
        deal();//发牌
        giveTurn(gamers[randInt(0,gamers.length-1)]);//随机给予回合
    }

    private void giveTurn(Gamer gamer) {
        deal(gamer);//常规发牌
        gamer.is_turn=true;
        //回合开始恢复该玩家部分能量值
        gamer.restoreEnergy(RESTORE_ENERGY_AMOUNT_DEFAULT);
        //通知该名玩家这是你的回合
        try {
            gamer.player.send(GameService.informThisIsYourTurn());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("giveTurn():报错");
        }
    }

    public void hasFinished(Gamer gamer){
        gamer.is_turn=false;
        //通知该名玩家你的回合已结束
        try {
            gamer.player.send(GameService.informTurnIsFinished());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("informTurnIsFinished():报错");
        }
        //给予另外一名玩家回合
        giveTurn(gamer.getPeer());
    }

    /**
     * 一方打出牌,成功返回对应的卡牌，失败不返回
     */
//    public Card cast(Player player,int cardId){
//        if(player==playerA){
//            CardUtil cardUtil=pr.cardsA.get(cardId);
//
//            if(pr.ok_A==true)
//                return null;//已经出牌
//
//            if(cardUtil==null){
//                //该玩家卡牌堆中没有这个牌
//                return null;
//            }else{
//                //该玩家卡牌堆中有这个牌
//                if(cardUtil.isAvailable()){
//                    //该玩家卡牌堆中这个牌还未打出
//
//                    //将被打出的卡牌放置在展示区
//                    pr.card_A=cardUtil.getCard();
//                    //将卡牌设置成out状态
//                    cardUtil.out();
//                    System.out.println("A出的牌:"+cardUtil);
//                    //设置玩家A已经打出状态
//                    pr.ok_A=true;
//                    //通知B，Aok了
//                    UDPSever.informOk(playerB);
//
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            try {
//                                //延迟开牌
//                                Thread.sleep(1500);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                            show();
//                        }
//                    }).start();
//                    return cardUtil.getCard();//打牌成功返回对应的卡牌
//                }else{
//                    //当前卡牌已经弃置到卡牌堆中，无法出牌
//                    System.out.println("A当前卡牌已经弃置到卡牌堆中，无法出牌");
//                    return null;
//                }
//            }
//
//        }else if(player==playerB){
//            CardUtil cardUtil=pr.cardsB.get(cardId);
//            if(pr.ok_B==true)
//                return null;//已经出牌
//            if(cardUtil==null){
//                //该玩家卡牌堆中没有这个牌
//                return null;
//            }else{
//                //该玩家卡牌堆中有这个牌
//                if(cardUtil.isAvailable()){
//                    //该玩家卡牌堆中这个牌还未打出
//
//                    //将被打出的卡牌放置在展示区
//                    pr.card_B=cardUtil.getCard();
//                    //将卡牌设置成out状态
//                    cardUtil.out();
//                    System.out.println("B出的牌:"+cardUtil);
//                    //设置玩家B已经打出状态
//                    pr.ok_B=true;
//                    //通知A，Bok了
//                    UDPSever.informOk(playerA);
//                    show();
//                    return cardUtil.getCard();
//                }else{
//                    //当前卡牌已经弃置到卡牌堆中，无法出牌
//                    System.out.println("B当前卡牌已经弃置到卡牌堆中，无法出牌");
//                    return null;
//
//                }
//            }
//        }else{
//            //当前游戏中没有该玩家
//            System.out.println("开挂行为");
//            return null;
//
//        }
//    }

    /**
     * 亮牌
     */
//    public void show(){
//
//        if(pr.ok_A&&pr.ok_B)
//        {
//            //判断本回合结果
//            Card rst=judge();
//            int code;
//            if(rst==pr.card_A){
//                code= (int) playerA.getUID();
//            }else if(rst==pr.card_B){
//                code=(int) playerB.getUID();
//            }else{
//                code=-1;//平局
//            }
//            //向双方发送牌的数据和本回合的结果
//            UDPSever.showCard(this,code);
//
//            //判断是场上是否有玩家血量为0
//            if(pr.hpA==0||pr.hpB==0){
//                //场上已有玩家血量为0
//                //结束本场游戏
//                close();
//            }else{
//                //继续游戏
//                //更新下一回合
//                next();
//            }
//        }
//    }
//    private Card judge(){
//        Card winner=pr.card_A.compare(pr.card_B);
//        if(winner== pr.card_A){
//            //A卡胜出
//            //B玩家掉血
//            pr.hpB--;
//        }else if(winner==null){
//            //平局
//            //不做掉血处理
//        }
//        else{
//            //B卡胜出
//            //A玩家掉血
//            pr.hpA--;
//        }
//        return winner;
//    }
    /**
     * 通知游戏结束
     */
    public void endGame(Player winner) throws Exception {
        for (Gamer gamer : this.gamers) {
            MyPacket packet;
            if (gamer.player == winner) {
                packet=new MyPacketBuilder().buildType("game")
                        .buildCmd("end")
                        .buildResult("win")
                        .build();
            } else {
                packet=new MyPacketBuilder().buildType("game")
                        .buildCmd("end")
                        .buildResult("lose")
                        .build();
            }
            packet.put("room", this.getRoom().toJSONObject());
            packet.put("rank", gamer.player.getRank());
            gamer.player.send(packet);
        }
    }


    /**
     * 游戏正常结束
     */
    public void close(Player loser){
        Player winner=loser.gamer.getPeer().player;
        //在数据库中更新双方rank分
        MySQLDBUtil.updateRank((int)loser.getUid(),-INCREMENT);//失败方
        MySQLDBUtil.updateRank((int)winner.getUid(),+INCREMENT);//胜利方
        //在程序中更新双方的rank分
        loser.setRank(MySQLDBUtil.getRank((int)loser.getUid()));
        winner.setRank(MySQLDBUtil.getRank((int)winner.getUid()));
        //将自己从游戏列表中移除
        gameList.remove(this.gameId);
        //通知Room销毁游戏
        this.room.destroyGame();
        //通知玩家本场游戏结果
        try {
            endGame(winner);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Game.endGame(winner):关闭游戏异常");
        }
    }

}
