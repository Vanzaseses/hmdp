package com.sengang.controller;

import com.sengang.dto.Result;
import com.sengang.service.FollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/follow")
public class FollowController {
    @Resource
    private FollowService followService;

    /***
     * @description: TODO :根据isFollow值改变数据库，是否关注对应userid
     * @params: [followUserId, isFollow]
     * @return: com.sengang.dto.Result
     * @author: SenGang
     */
    @PutMapping("{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followUserId, @PathVariable("isFollow")Boolean isFollow){
        return followService.follow(followUserId,isFollow);
    }

    /***
     * @description: TODO :查询用户是否被关注
     * @params: [followUserId]
     * @return: com.sengang.dto.Result
     * @author: SenGang
     */
    @GetMapping("or/not/{id}")
    public Result isFollow(@PathVariable("id") Long followUserId){
        return followService.ifFollow(followUserId);
    }

    /***
     * @description: TODO :求共同关注的id
     * @params: [userId]
     * @return: com.sengang.dto.Result
     * @author: SenGang
     */
    @GetMapping("common/{id}")
    public Result commonFollow(@PathVariable("id")Long userId){
        return followService.commonFollow(userId);
    }

}
