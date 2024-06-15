package com.sengang.utils;

import cn.hutool.core.util.StrUtil;
import io.jsonwebtoken.*;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.Date;

@Data
@Component

@ConfigurationProperties(prefix = "jwt.token")//批量读取application.xml中的jwt.token前缀的内容,属性自动对应赋值
//@PropertySource("classpath:application.yaml")
public class JwtHelper {

//    @Value(value = "jwt.token.tokenExpiration")
    private  long tokenExpiration; //有效时间,单位毫秒 1000毫秒 == 1秒
//    @Value("jwt.token.tokenSignKey")
    private  String tokenSignKey;  //当前程序签名秘钥

    //生成token字符串
    public  String createToken(String phone) {
        System.out.println("tokenExpiration = " + tokenExpiration);
        System.out.println("tokenSignKey = " + tokenSignKey);
        String token = Jwts.builder()

                .setSubject("YYGH-USER")
                .setExpiration(new Date(System.currentTimeMillis() + tokenExpiration*1000*60)) //单位分钟
                .claim("phone", phone)
                .signWith(SignatureAlgorithm.HS512, tokenSignKey)
                .compressWith(CompressionCodecs.GZIP)
                .compact();
        return token;
    }

    //从token字符串获取phone
    public  String getPhone(String token) {
        if(StrUtil.isEmpty(token)) return null;
        Jws<Claims> claimsJws = Jwts.parser().setSigningKey(tokenSignKey).parseClaimsJws(token);
        Claims claims = claimsJws.getBody();
        String phone = (String)claims.get("phone");
        return phone;
    }



    /***
     * @description: TODO :判断token是否有效
     * @params: [token]
     * @return: boolean
     * @author: SenGang
     */

    public  boolean isExpiration(String token){
        try {
            boolean isExpire = Jwts.parser()
                    .setSigningKey(tokenSignKey)
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration().before(new Date());
            //没有过期，有效，返回false
            return isExpire;
        }catch(Exception e) {
            //过期出现异常，返回true
            return true;
        }
    }
}
