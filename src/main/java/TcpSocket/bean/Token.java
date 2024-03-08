package TcpSocket.bean;

import TcpSocket.HmacSHA256Util;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Liangpeng Yan(Nickname:Mr.Independent)
 * @date 2022/10/31 - 20:48
 */
public class Token {
    private static final long VAID_TIME=60*1000*60*6;//6小时
    private static final String SECRET="mymo";


    /**
     * 检查令牌是否有效
     * @param token
     * @return 令牌有效返回UID，令牌无效返回false
     */
    public static Object checkToken(String token){
        {
            //token 解码过程
            Base64.Decoder decoder=Base64.getDecoder();
            System.out.println(token);
//            当以.(点号)作为String.split()的分割符时，表达式不应该写成String.split('.')，因为点号在正则表达式中由特殊含义，所以此处应该用转义字符String.split('\\.')。
            String[] split = token.split("\\.");
            System.out.println(split.length);
            String header_base64=split[0];
            String body_base64=split[1];
            String body=new String(decoder.decode(body_base64), StandardCharsets.UTF_8);
            String[] bodySplit=body.split("&");
            int UID=Integer.parseInt(bodySplit[0]);
            long createTime=Long.parseLong(bodySplit[1]);
            long exp=Long.parseLong( bodySplit[2]);
            long curTime=System.currentTimeMillis();
            if(exp<curTime) {
                return false;//令牌过期
            }

            String s_hs256=split[2];
            Base64.Encoder encoder=Base64.getEncoder();
            StringBuilder sb=new StringBuilder();
            sb.append(header_base64);
            sb.append(".");
            sb.append(body_base64);
            String s_hs256_temp="";
            try {
                s_hs256_temp=HmacSHA256Util.hmacSHA256(SECRET,sb.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
            sb.append(".");
            sb.append(s_hs256_temp);

            if(sb.toString().equals(token)){
                //令牌有效
                System.out.println("令牌有效");
                return UID;
            }else{
                //令牌被篡改，无效
                System.out.println("令牌被篡改");
                return false;
            }

        }
    }



    //header
    String alg="HS256";
    //body
    private int UID;
    private long createDate;
    private long expiredDate;
    private String cipher;//密文 /ˈsaɪfə(r)/


    public Token(int UID) {
        this.UID=UID;
        this.createDate = System.currentTimeMillis();
//            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//            df.format(date)
//        this.expiredDate = addHour(createDate,6);//增加6个小时
        this.expiredDate=createDate+VAID_TIME;
        this.cipher = encrypt();
    }

    public String getCipher(){
        return cipher;
    }


    private Date addHour(Date date,int i){
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.HOUR_OF_DAY, i);
        Date newDate = c.getTime();
        return newDate;
    }

    /**
     * 加密
     * @return
     */
    private String encrypt(){
        //header.body.(二者的加密)
        Base64.Encoder encoder=Base64.getEncoder();
        StringBuilder sb=new StringBuilder();
        String header=this.alg;
        String body=this.UID+"&"+this.createDate+"&"+this.expiredDate;

        String header_base64=encoder.encodeToString(header.getBytes(StandardCharsets.UTF_8));
        String body_base64=encoder.encodeToString(body.getBytes(StandardCharsets.UTF_8));

        sb.append(header_base64);
        sb.append(".");
        sb.append(body_base64);
        final String SECRET="mymo";
        String s_hs256="";
        try {
            s_hs256= HmacSHA256Util.hmacSHA256(SECRET,sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        sb.append(".");
        sb.append(s_hs256);
        return sb.toString();
    }
}
