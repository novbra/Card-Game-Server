package TcpSocket.bean;

/**
 * @author Mr.Independent
 * @date 2022/6/24 - 21:40
 */
public class PokemonCard {
    private int id;
    private int type;//0 水 1 火 2 木 3 无视属性 4无敌
    private int func;//特防 物防 特攻 攻击 全能盾 血包
    private int weight;
    private int resourceId;
    private boolean available;

    public PokemonCard(int id) {
        this.id = id;
        initCard();
    }

    private void initCard() {
    }
}
