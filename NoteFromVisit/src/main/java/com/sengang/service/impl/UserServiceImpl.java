package com.sengang.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sengang.dto.LoginFormDTO;
import com.sengang.dto.Result;
import com.sengang.dto.UserDTO;
import com.sengang.entity.User;
import com.sengang.service.UserService;
import com.sengang.mapper.UserMapper;
import com.sengang.utils.JwtHelper;
import com.sengang.utils.MD5Util;
import com.sengang.utils.RegexUtils;
import com.sengang.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.mapper.Mapper;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.sengang.utils.RedisConstants.*;
import static com.sengang.utils.SystemConstants.*;

/**
* @author SenGang
* @description 针对表【tb_user】的数据库操作Service实现
* @createDate 2024-04-07 21:58:25
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{
    @Resource
    private StringRedisTemplate stringRedisTemplate;//Redis操作,ops
    @Resource
    private JwtHelper jwtHelper;

    @Override
    public Result senCode(String phone, HttpSession session) {
        //检验手机是否正确
        if(RegexUtils.isPhoneInvalid(phone))
            return Result.fail("手机格式错误");
        //手机号码正确则生成验证码6位数
        String code = RandomUtil.randomNumbers(6);
        //保存验证码到session
//        session.setAttribute(USER_LOGIN_CODE,code);
        //保存验证码到redis,key为手机号,15分钟有效期
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY_PREFIX+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);//加一个统一前缀,失效时间5分钟

        //发送验证码
        log.info("发送短信验证码，验证码为:{}",code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginFormDTO,HttpSession session) {
        String phone = loginFormDTO.getPhone();
        if(RegexUtils.isPhoneInvalid(phone))
            return Result.fail("手机号格式错误");
        //获得存储在session中的验证码
//        String code = (String)session.getAttribute(USER_LOGIN_CODE);
        //获得存储在redis中的验证码
        String code = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY_PREFIX + phone);
        //与传入的验证码对比
        if(!loginFormDTO.getCode().equals(code)||loginFormDTO.getCode()==null)//错误直接返回
            return Result.fail("验证码错误");
        //正确则判断用户是否存在，不存在则添加
        //根据phone获得user
        User user = query().eq("phone", phone).one();
        //判断user是否存在
        if(user==null){//不存在则新建
            user = createByPhone(phone);
        }
        //存在就将用户简略信息保存到session中以便后续用户访问,TODO:不需要保存在cookie中，因为用户会携带唯一的sessionId访问
//        session.setAttribute(USER, BeanUtil.copyProperties(user, UserDTO.class));//自动拷贝对应的user属性到UserDTO
        //TODO:存在就生成一个唯一的token给前端作为浏览器标识符和作为redis对应对象的key，将用户简略信息保存到redis中以便后续用户访问
        //随机生成token，用UUID
//        String token = UUID.randomUUID().toString(true);
        //TODO:jwt
        String token = jwtHelper.createToken(MD5Util.encrypt(phone));//TODO:MD5加密
        //保存到redis中
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);//获得对象简略信息
        Map<String, Object> userDTOMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),//将对象转换为map以便存进redis的hash中
                CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((fieldName,fieldValue)->{
                    if(fieldValue!=null)
                        return fieldValue.toString();
                    return null;
                }));//将userDTO所有属性转为string才能存进redis的string结构
        //TODO: setFieldValueEditor传入三个值分别是参数名、参数值、返回值,函数体自定义
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY_PREFIX+token,userDTOMap);
        stringRedisTemplate.expire(LOGIN_USER_KEY_PREFIX+token,LOGIN_USER_TTL,TimeUnit.SECONDS);//设置token有效期3600s

        return Result.ok(token);
    }

    @Override
    public Result sign() {
        UserDTO user = UserHolder.getUser();
        //获取年月,以用户id+年月为key
        LocalDateTime now = LocalDateTime.now();
        String timeSuffix = now.format(DateTimeFormatter.ofPattern(":yyyy/MM"));
        String key = USER_SIGN_KEY+user.getId()+timeSuffix;
        //获取天
        int day = now.getDayOfMonth();
        //设置签到 TODO:一个bitmap 32位，一位当一天
        stringRedisTemplate.opsForValue().setBit(key,day-1,true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        UserDTO user = UserHolder.getUser();
        LocalDateTime now = LocalDateTime.now();
        String timeSuffix = now.format(DateTimeFormatter.ofPattern(":yyyy/MM"));
        String key = USER_SIGN_KEY+user.getId()+timeSuffix;
        int today = now.getDayOfMonth();
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(today)).valueAt(0));
        if (result==null||result.isEmpty())
            return Result.ok();
        Long num = result.get(0);
        if(num==null||num==0)
            return Result.ok();
        int count = 0;
        //TODO:按位与1,判断最后一天是否签到,然后右移判断前一天是否也签到
        while(true){
            if ((num & 1)==0){
                //代表该日未签到
                break;
            }else{
                //该日签到
                count++;
            }
            //num右移
            num = num>>>1;
        }
        return Result.ok(count);
    }

    private User createByPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX +RandomUtil.randomString(10));
        //保存用户
        save(user);
        return user;
    }
}




