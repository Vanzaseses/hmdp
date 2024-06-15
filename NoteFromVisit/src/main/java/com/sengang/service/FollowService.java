package com.sengang.service;

import com.sengang.dto.Result;
import com.sengang.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author SenGang
* @description 针对表【tb_follow】的数据库操作Service
* @createDate 2024-04-12 22:07:30
*/
public interface FollowService extends IService<Follow> {

    Result follow(Long followUserId, Boolean isFollow);

    Result ifFollow(Long followUserId);

    Result commonFollow(Long userId);
}
