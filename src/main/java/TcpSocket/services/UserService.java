package TcpSocket.services;


import TcpSocket.bean.DatabaseUser;
import TcpSocket.mapper.UserMapper;
import TcpSocket.util.SqlSessionFactoryUtils;
import org.apache.ibatis.session.SqlSession;

import java.io.IOException;
import java.util.List;

/**
 * @author Liangpeng Yan(Nickname:Mr.Independent)
 * @date 2022/10/7 - 1:50
 */
public class UserService {
    /**
     * 通过id查询User
     * @param id
     * @return
     * @throws IOException
     */
    public DatabaseUser selectedById(int id) throws IOException{
        SqlSession sqlSession = SqlSessionFactoryUtils.getSqlSessionFactory().openSession();
        UserMapper mapper = sqlSession.getMapper(UserMapper.class);
        DatabaseUser user= mapper.selectedById(id);
        sqlSession.close();
        return user;
    }

    /**
     * 查询所有
     * @return
     * @throws IOException
     */
    public List<DatabaseUser> selectAll() throws IOException {
        SqlSession sqlSession = SqlSessionFactoryUtils.getSqlSessionFactory().openSession();
        UserMapper mapper = sqlSession.getMapper(UserMapper.class);
        List<DatabaseUser> users= mapper.selectAll();
        sqlSession.close();
        return users;
    }

    /**
     * 检查账号与密码
     * @param user
     * @param pwd
     * @return
     * @throws IOException
     */
    public DatabaseUser checkPwd(String user,String pwd) throws IOException{
        SqlSession sqlSession = SqlSessionFactoryUtils.getSqlSessionFactory().openSession();
        UserMapper mapper = sqlSession.getMapper(UserMapper.class);
        DatabaseUser user_= mapper.checkPwd(user,pwd);
        sqlSession.close();
        return user_;
    }

    /**
     * 通过手机号码获取User对象
     */
    public DatabaseUser getUser(String phoneNumber) throws IOException{
        SqlSession sqlSession = SqlSessionFactoryUtils.getSqlSessionFactory().openSession();
        UserMapper mapper = sqlSession.getMapper(UserMapper.class);
        DatabaseUser user= mapper.getUser(phoneNumber);
        sqlSession.close();
        return user;
    }

    /**
     * 添加User对象
     * @param user
     * @throws IOException
     */
    public void register(DatabaseUser user) throws IOException{
        SqlSession sqlSession = SqlSessionFactoryUtils.getSqlSessionFactory().openSession();
        UserMapper mapper = sqlSession.getMapper(UserMapper.class);
        mapper.register(user);
        sqlSession.commit();
        sqlSession.close();
    }

    public void update(String name,int status,int id) throws IOException{
        SqlSession sqlSession = SqlSessionFactoryUtils.getSqlSessionFactory().openSession();
        UserMapper mapper = sqlSession.getMapper(UserMapper.class);
        mapper.update(name,status,id);
        sqlSession.commit();
        sqlSession.close();
    }

    public void update(int status,int id) throws IOException{
        SqlSession sqlSession = SqlSessionFactoryUtils.getSqlSessionFactory().openSession();
        UserMapper mapper = sqlSession.getMapper(UserMapper.class);
        mapper.update2(status,id);
        sqlSession.commit();
        sqlSession.close();
    }



}
