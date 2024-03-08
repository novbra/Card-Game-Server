package TcpSocket;

import TcpSocket.bean.Currencies;
import TcpSocket.mapper.CurrenciesMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.List;

/**
 * @author Mr.Independent
 * @date 2022/6/6 - 12:38
 */
public class MySQLDBUtil {
    private static  String driver="com.mysql.jdbc.Driver";
    private static String DbUser="sa";
    private static String DbPassword="jibazhenda";
    //autoReconnect=true
    //useServerPrepStmts=true 开启预编译功能，mysql 默认是关闭的
    private static String dbUtil="jdbc:mysql://124.223.75.186/mymo?useUnicode=true&characterEncoding=utf-8&useServerPrepStmts=true";
    public static Connection connection=null;
    private static boolean connection_state=false;

    private static final int RANK_INIT=1000;

    public static boolean connect(){
//        判断连接是否已经建立
        if (!connection_state) {
            try {
                Class.forName(driver);//动态加载类 可以不写
                //尝试建立到个定数据库URL的连接
                System.out.printf("[%s]%s\n","Databases","Linking...");
                connection = DriverManager.getConnection(dbUtil, DbUser, DbPassword);
                connection_state=true;
            } catch (Exception e) {
                e.printStackTrace();
                connection_state=false;
            }
        }
        return connection_state;
    }

    public static void disConnect() {
        if (!connection_state) {
            System.out.println("关闭数据库失败,因为与数据库没有建立连接");
            return;
        } else {
            try {
                connection.close();
                connection_state=false;
                System.out.println("关闭数据库");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 开发代理方式 重点掌握
     * @throws IOException
     */
    public static void getAllCurrencies() throws IOException {
        //1.加载mybatis的核心配置文件，获取SqlSessionFactory
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        //2.获取SqlSession对象，用它来执行sql
        SqlSession sqlSession = sqlSessionFactory.openSession();
        //3.执行sql selectOne 查一个,selectList 查所有
//        List<Currencies> currenciesList = sqlSession.selectList("test.selectAll");
        //3.1 获取CurrenciesMapper接口的代理对象
        CurrenciesMapper currenciesMapper = sqlSession.getMapper(CurrenciesMapper.class);
        List<Currencies> currenciesList = currenciesMapper.selectAll();
        System.out.println(currenciesList);
        //4.释放资源
        sqlSession.close();
    }
    /**
     * 销号 ctrl+alt+T 弹出surround
     * @param id
     * @return
     */
    public static boolean cancelAccount(int id){
        if(connection_state){
            try {
                String sql="delete from `user` where id=?";
                PreparedStatement pstmt=connection.prepareStatement(sql);
                pstmt.setInt(1,id);
                int count=pstmt.executeUpdate();
                pstmt.close();
                if(count>0){
                    return true;
                }else{
                    return false;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }
    /**
     * 数据库操作
     */
    public static boolean register(String user,String key,String ip){
        if (connection_state) {

            String sql = "INSERT INTO `user`" +
                    "(`phone_number`,`password_md5`,`status`,`role`,`register_ip`)" +
                    "VALUES(" +
                    "'" + user +
                    "',MD5('" + key + //MD5加密
                    "'),0,'" +
                    "'+'" +
                    "','" + ip + "')";
            if(getUid(user)==-1){
                try {
                    Statement statement = connection.createStatement();
                    statement.execute(sql);
                    initRank(user,RANK_INIT);//初始化玩家的rank分
                    return true;
                } catch (SQLException e) {
                    e.printStackTrace();
                    return false;
                } finally {

                }
            }else{
                //该账号已存在，不可以注册
                return false;
            }
        }else {
            return false;
        }
    }

    /**
     * 登录,注意防止SQL注入 ' or '1'='1
     * @param user
     * @param key
     * @param ip
     * @return
     */
    public static boolean login(String user,String key,String ip){
        if (connection_state) {
//            String sql = "SELECT * FROM `user` WHERE phone_number ='" + user + "' AND password_md5=MD5('" + key + "')";
            String sql = "SELECT * FROM `user` WHERE phone_number =? AND password_md5=MD5(?)";
            try {
                PreparedStatement pstmt=connection.prepareStatement(sql);
                pstmt.setString(1,user);
                pstmt.setString(2,key);
                ResultSet rs=pstmt.executeQuery();
                if (rs.next()) {
                    sql = "UPDATE `user` SET login_ip=? WHERE phone_number =?";
                    pstmt=connection.prepareStatement(sql);
                    pstmt.setString(1,ip);
                    pstmt.setString(2,user);
                    pstmt.execute();
                    return true;
                } else {
                    return false;

                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        else{
            return false;
        }
    }

    public static boolean createUserTable(){
        if (connection_state) {
            String sql = "CREATE TABLE `user`" +
                    "(" +
                    "`id` int UNSIGNED AUTO_INCREMENT PRIMARY KEY," +
                    "`phone_number` VARCHAR(11) UNIQUE," +
                    "`password_md5` VARCHAR(50)," +
                    "`name` VARCHAR(16) NOT NULL DEFAULT '未命名的玩家',"+
                    "`status` boolean," +
                    "`role` VARCHAR(16)," +
                    "`register_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "`register_ip` VARCHAR(15)," +
                    "`login_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP," +
                    "`login_ip` VARCHAR(15)" +
                    ")CHARACTER SET utf8 COLLATE utf8_general_ci";
            try {
                Statement statement = connection.createStatement();
                statement.execute(sql);
                System.out.printf("[%s]%s\n","Databases","建立玩家表成功");

            } catch (SQLException e) {
                e.printStackTrace();
                System.out.printf("[%s]%s\n","Databases","建立玩家表失败");
                return false;
            } finally {
                return true;
            }
        }else {
            System.out.printf("[%s]s\n","Databases","数据库未连接");
            return false;
        }
    }
    public static boolean createRankTable(){
        if (connection_state) {
            String sql = "CREATE TABLE `user_rank`(`uid` INT  UNSIGNED UNIQUE PRIMARY KEY,`rank` INT)";
            try {
                Statement statement = connection.createStatement();
                statement.execute(sql);

            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            } finally {
                return true;
            }
        }else
            System.out.printf("[%s]创建rank表失败\n","Databases");
            return false;
    }

    public static boolean createChatTable(){
        if (connection_state) {
            String sql = "CREATE TABLE `user_msg`(id int UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,`from_uid` int UNSIGNED NOT NULL,`to_uid` int UNSIGNED NOT NULL,`send_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,`is_read` boolean DEFAULT 0,`is_withdraw` boolean DEFAULT 0,`content` VARCHAR(256))";
            try {
                Statement statement = connection.createStatement();
                statement.execute(sql);

            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            } finally {
                return true;
            }
        }else
            return false;
    }

    /**
     * 聊天记录储存到数据库中
     * content限制在256Byte
     * @param chatData
     * @return
     */
    public static boolean addChatRecord(JSONObject chatData){
        String from_uid= (String) chatData.get("from_uid");
        String to_uid= (String) chatData.get("to_uid");
        String content= (String) chatData.get("msg");

        if (connection_state) {
            String sql ="INSERT INTO `user_msg (from_uid,to_uid,content)" +
                    "VALUES ('"+from_uid+"','"+to_uid+"','"+content+"')";
            try {
                Statement statement = connection.createStatement();
                statement.execute(sql);
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        else
            return false;

    }

    /**
     * Get uid by phone number
     * @param phone_number
     * @return uid
     */
    public static int getUid(String phone_number){
        if (connection_state) {
            String sql ="SELECT id FROM user WHERE phone_number='"+phone_number+"'";
            try {
                Statement statement = connection.createStatement();
                statement.execute(sql);
                ResultSet rs=statement.getResultSet();
                if(rs.next()){
                    return rs.getInt("id");
                }else{
                    return -1;//未查找到相关uid
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return -2;//连接异常
            }
        }
        else
            return -3;//连接异常
    }

    /**
     * Get
     * @param uid
     * @param columnName
     * @return
     */
    public static String getFromUser(int uid,String columnName){
        if (connection_state) {
            String sql ="SELECT "+columnName+" FROM `user` WHERE `id`="+uid;
            try {
                Statement statement = connection.createStatement();
                statement.execute(sql);
                ResultSet rs=statement.getResultSet();
                if(rs.next()){
                    String name=rs.getString(columnName);
                    return name;
                }else{
                    System.out.printf("[%s]%s\n","Databases","未找到对应名称");
                    return "匿名";
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return "匿名";//连接异常
            }
        }
        else
            return "匿名";//连接异常
    }
    /**
     * Get rank
     * @param uid
     * @return rank
     */
    public static int getRank(int uid){
        if (connection_state) {
            String sql ="SELECT `rank` FROM `user_rank` WHERE `uid`='"+uid+"'";
            try {
                Statement statement = connection.createStatement();
                statement.execute(sql);
                ResultSet rs=statement.getResultSet();
                if(rs.next()){
                    return rs.getInt("rank");
                }else{
                    return -1;//未查找到rank
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return -1;//连接异常
            }
        }
        else
            return -1;//连接异常
    }

    public static ResultSet getCards(){
        if (connection_state) {
            String sql ="SELECT * FROM `cards` ";
            try {
                Statement statement = connection.createStatement();
                statement.execute(sql);
                ResultSet rs=statement.getResultSet();
                if(rs.next()){
                    return rs;
                }else{
                    return null;//
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return null;//连接异常
            }
        }
        else
            return null;//连接异常

    }

    /**
     * 初始化rank分
     * @param phone_number
     * @param rank
     * @return
     */
    public static boolean initRank(String phone_number,int rank){
        if (connection_state) {
            String sql ="INSERT INTO `user_rank`(`uid`,`rank`) VALUES((SELECT `id` FROM `user`WHERE phone_number='"+phone_number+"'),"+rank+")";
            try {
                Statement statement = connection.createStatement();
                statement.execute(sql);
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        else
            return false;
    }
    /**
     * 更新user表
     * @param uid
     * @param columnName
     * @return
     */
    public static boolean updateFromUser(int uid,String columnName,String name){
        if (connection_state) {

            String sql ="UPDATE `user` SET "+columnName+" = '"+name+ "' WHERE id='"+uid+"'";
            try {
                Statement statement = connection.createStatement();
                statement.execute(sql);
                //通知Server 对对应的玩家更改名称
                UDPSever.getPlayer(uid).setName(name);
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        else
            return false;
    }
    /**
     * 更新rank分
     * @param uid
     * @param increment
     * @return
     */
    public static boolean updateRank(int uid,int increment){
        if (connection_state) {
            String sql ="UPDATE user_rank SET rank=rank+"+increment+ " WHERE uid="+uid;
            try {
                Statement statement = connection.createStatement();
                statement.execute(sql);
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        else
            return false;
    }

    public static boolean checkNameUnique(String name) {
        if (connection_state) {
            String sql ="SELECT `name` FROM `user` WHERE `name`='"+name+"'";
            try {
                Statement statement = connection.createStatement();
                statement.execute(sql);
                ResultSet rs=statement.getResultSet();
                if(rs.next()){
                    return false;
                }else{
                    return true;//未查找到该名称
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        else
            return false;

    }
}
