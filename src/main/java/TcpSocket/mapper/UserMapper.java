package TcpSocket.mapper;
import TcpSocket.bean.DatabaseUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * @author Liangpeng Yan(Nickname:Mr.Independent)
 * @date 2022/10/5 - 0:19
 */
public interface UserMapper {
    /**
     * 查询所有
     * @return
     */
    List<DatabaseUser> selectAll();

    /**
     * 通过id查询User对象
     * @param id
     * @return
     */
    @Select("SELECT * FROM user WHERE id=#{id}")
    DatabaseUser selectedById(int id);

    /**
     * 根据手机号码和密码查询用户对象
     * @param user
     * @param pwd
     * @return
     */
    @Select("SELECT * FROM user WHERE phone_number =#{user} AND password_md5=MD5(#{pwd})")
    DatabaseUser checkPwd(@Param("user") String user,@Param("pwd") String pwd);

    /**
     * 根据手机号码查询用户对象
     * @param user
     * @return
     */
    @Select("SELECT * FROM user WHERE phone_number =#{user}")
    DatabaseUser getUser(String user);

    /**
     * 注册
     * @param user
     * @return
     */
    @Insert("INSERT user (id,phone_number, password_md5, name,register_ip) values (null,#{phoneNumber},MD5(#{passwordMd5}),#{name},#{registerIp})")
    void register(DatabaseUser user);
    /**
     * 更新信息
     * @param name
     * @param status
     */
    @Update("update user set name=#{name},status=#{status} where id=#{id}")
    void update(@Param("name") String name,@Param("status") int status,@Param("id") int id);


    /**
     * 更新信息
     * @param status
     */
    @Update("update user set status=#{status} where id=#{id}")
    void update2(@Param("status") int status,@Param("id") int id);
}
