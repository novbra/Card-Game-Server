package TcpSocket.mapper;

import TcpSocket.bean.Currencies;

import java.util.List;

/**
 * @author Liangpeng Yan(Nickname:Mr.Independent)
 * @date 2022/10/3 - 21:58
 */
public interface CurrenciesMapper {
    List<Currencies> selectAll();
}
