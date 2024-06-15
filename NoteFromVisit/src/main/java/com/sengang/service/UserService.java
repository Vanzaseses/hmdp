package com.sengang.service;

import com.sengang.dto.LoginFormDTO;
import com.sengang.dto.Result;
import com.sengang.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpSession;

/**
* @author SenGang
* @description 针对表【tb_user】的数据库操作Service
* @createDate 2024-04-07 21:58:25
*/
public interface UserService extends IService<User> {

    Result senCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginFormDTO,HttpSession session);

    Result sign();

    Result signCount();
}
