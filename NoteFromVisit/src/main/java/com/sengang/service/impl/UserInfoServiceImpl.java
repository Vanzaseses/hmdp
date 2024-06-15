package com.sengang.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sengang.entity.UserInfo;
import com.sengang.service.UserInfoService;
import com.sengang.mapper.UserInfoMapper;
import org.springframework.stereotype.Service;

/**
* @author SenGang
* @description 针对表【tb_user_info】的数据库操作Service实现
* @createDate 2024-04-07 22:51:17
*/
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo>
    implements UserInfoService{

}




