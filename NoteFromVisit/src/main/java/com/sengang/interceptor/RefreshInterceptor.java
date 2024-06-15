package com.sengang.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.sengang.dto.UserDTO;
import com.sengang.utils.JwtHelper;
import com.sengang.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.sengang.utils.RedisConstants.LOGIN_USER_KEY_PREFIX;
import static com.sengang.utils.RedisConstants.LOGIN_USER_TTL;
/***
 * @description: TODO :用于对所有的访问都刷新token,并且判断user是否存在，存在就放进ThreadLocal中
 * @params:
 * @return:
 * @author: SenGang
 */
public class RefreshInterceptor implements HandlerInterceptor {
    private StringRedisTemplate stringRedisTemplate;//无法将这个放进IOC容器，因为拦截器不是spring自带的bean
    @Resource
    private JwtHelper jwtHelper;
    public RefreshInterceptor(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate=stringRedisTemplate;
    }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获得该会话对应的session域
//        HttpSession session = request.getSession();
        //获取token
        String token = request.getHeader("authorization");
        //判断token是否存在
        if(StrUtil.isBlank(token)){
            return true;//不存在就放行,存在就刷新token
        }

        //TODO : JWT解密,未完成,完成了也没用,数据都在redis里
//        String phone = jwtHelper.getPhone(token);

        //获得该会话对应的user
//        UserDTO userDTO = (UserDTO)session.getAttribute(SystemConstants.USER);
        //获取token对应的map
        Map<Object, Object> userDTOMap = stringRedisTemplate.opsForHash().entries(LOGIN_USER_KEY_PREFIX + token);
        //判断map是否存在
        if(userDTOMap.isEmpty()){
            return true;//不存在就放行,存在就将user保存到ThreadLocal
        }
        //将map转换成userDTO
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userDTOMap, new UserDTO(), false);
        //刷新token持续时间
        stringRedisTemplate.expire(LOGIN_USER_KEY_PREFIX + token,LOGIN_USER_TTL, TimeUnit.SECONDS);
        //保存用户到ThreadLocal
        UserHolder.saveUser(BeanUtil.copyProperties(userDTO, UserDTO.class));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
