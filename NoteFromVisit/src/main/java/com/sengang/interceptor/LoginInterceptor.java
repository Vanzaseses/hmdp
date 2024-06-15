package com.sengang.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.sengang.dto.UserDTO;
import com.sengang.entity.User;
import com.sengang.utils.SystemConstants;
import com.sengang.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.sengang.utils.RedisConstants.LOGIN_USER_KEY_PREFIX;
import static com.sengang.utils.RedisConstants.LOGIN_USER_TTL;

/***
 * @description: TODO ：登录拦截器，如果ThreadLocal中有用户信息且用户存在则放行，否则拦截
 * @params:
 * @return:
 * @author: SenGang
 */

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(UserHolder.getUser()==null){//取出用户
            response.setStatus(401);
            return false;//不存在就拦截
        }

        return true;
    }
}
