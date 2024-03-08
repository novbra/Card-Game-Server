package TcpSocket.observer;

/**
 * @author Liangpeng Yan(Nickname:Mr.Independent)
 * @date 2022/10/7 - 21:12
 */
public class MainTaskObserver implements Observer {
    @Override
    public void endTask() {
        System.out.println("完成主线任务");
    }
}
