package TcpSocket.observer;

/**
 * @author Liangpeng Yan(Nickname:Mr.Independent)
 * @date 2022/11/1 - 21:39
 */
public abstract class SendStateObserver {
    public abstract void positiveTask();
    public abstract void passiveTask();
}
