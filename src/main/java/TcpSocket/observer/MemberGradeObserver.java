package TcpSocket.observer;

/**
 * @author Liangpeng Yan(Nickname:Mr.Independent)
 * @date 2022/10/7 - 21:11
 */
public class MemberGradeObserver implements Observer {
    @Override
    public void endTask() {
        System.out.println("玩家升级");
    }
}
