package com.sengang.controller;

import cn.hutool.core.bean.BeanUtil;
import com.sengang.dto.LoginFormDTO;
import com.sengang.dto.Result;
import com.sengang.dto.UserDTO;
import com.sengang.entity.User;
import com.sengang.entity.UserInfo;
import com.sengang.service.UserInfoService;
import com.sengang.service.UserService;
import com.sengang.utils.SystemConstants;
import com.sengang.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;
    @Resource
    private UserInfoService userInfoService;

    /***
     * @description: TODO ：给手机发送验证码,(并将验证码保存到session)->用redis替代，
     * @params: [phone, session]
     * @return: com.sengang.dto.Result
     * @author: SenGang
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone")String phone, HttpSession session){
        return userService.senCode(phone,session);
    }

    /***
     * @description: TODO :根据id查询用户
     * @params: [userId]
     * @return: com.sengang.dto.Result
     * @author: SenGang
     */

    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id") Long userId){
        // 查询详情
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // 返回
        return Result.ok(userDTO);
    }

    /***
     * @description: TODO :根据返回的手机号和验证码进行登录或注册
     * @params: [loginFormDTO, session]
     * @return: com.sengang.dto.Result
     * @author: SenGang
     */
    @PostMapping("login")
    public Result login(@RequestBody LoginFormDTO loginFormDTO,HttpSession session){
        return userService.login(loginFormDTO,session);
    }

    /***
     * @description: TODO :获取当前登录的用户
     * @params: []
     * @return: com.sengang.dto.Result
     * @author: SenGang
     */
    @GetMapping("me")
    public Result me(){
        //TODO:简略用户信息存在ThreadLocal中，code存在session中
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }
    /***
     * @description: TODO :根据userId返回UserInfo
     * @params: [userId]
     * @return: com.sengang.dto.Result
     * @author: SenGang
     */
    @GetMapping("info/{id}")
    public Result infoById(@PathVariable(value = "id")Long userId){
        UserInfo userInfo = userInfoService.getById(userId);
        if(userInfo==null)
            return Result.ok();
        userInfo.setCreateTime(null);
        userInfo.setUpdateTime(null);
        // 返回
        return Result.ok(userInfo);
    }

    /***
     * @description: TODO :签到功能实现，用bitmap
     * @params: []
     * @return: com.sengang.dto.Result
     * @author: SenGang
     */
    @PostMapping("sign")
    public Result sign(){
        return userService.sign();
    }

    /***
     * @description: TODO :统计连续签到天数
     * @params: []
     * @return: com.sengang.dto.Result
     * @author: SenGang
     */

    @GetMapping("sign/count")
    public Result signCount(){
        return userService.signCount();
    }
}
