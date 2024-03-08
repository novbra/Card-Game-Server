package TcpSocket;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerFactory;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * @author Liangpeng Yan(Nickname:Mr.Independent)
 * @date 2022/9/28 - 17:40
 */
public class LoggerUtil {
    public static String dir;
    static {
        dir=System.getProperty("user.dir")+"\\";
        System.out.println(dir);
        DOMConfigurator.configure(dir+"log4j.xml"); //"/"号表示父的文件夹
    }

    public static Logger getLogger(Class<?> clazz) {
        return Logger.getLogger(clazz);
    }

    public static Logger getLogger(String name) {
        return Logger.getLogger(name);
    }

    public static Logger getLogger(String name, LoggerFactory factory) {
        return Logger.getLogger(name, factory);
    }

    public static Logger getRootLogger() {
        return Logger.getRootLogger();
    }

}
