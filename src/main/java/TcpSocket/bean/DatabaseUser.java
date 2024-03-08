package TcpSocket.bean;

import java.util.Date;

/**
 * @author Liangpeng Yan(Nickname:Mr.Independent)
 * @date 2022/11/1 - 18:51
 */

public class DatabaseUser {
    private int id;
    private String phoneNumber;
    private String passwordMd5;
    private String name;
    private int status;
    private int role;
    private Date registerTime;
    private String registerIp;
    private Date loginTime;
    private String loginIp;

    public void setRegisterIp(String registerIp) {
        this.registerIp = registerIp;
    }

    public int getId() {
        return id;
    }

    public int getStatus() {
        return status;
    }

    public int getRole() {
        return role;
    }

    public Date getRegisterTime() {
        return registerTime;
    }

    public String getRegisterIp() {
        return registerIp;
    }

    public Date getLoginTime() {
        return loginTime;
    }

    public String getLoginIp() {
        return loginIp;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getPasswordMd5() {
        return passwordMd5;
    }

    public String getName() {
        return name;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setPasswordMd5(String passwordMd5) {
        this.passwordMd5 = passwordMd5;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", passwordMd5='" + passwordMd5 + '\'' +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", role=" + role +
                ", registerTime=" + registerTime +
                ", registerIp='" + registerIp + '\'' +
                ", loginTime=" + loginTime +
                ", loginIp='" + loginIp + '\'' +
                '}';
    }
}


