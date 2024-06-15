package com.sengang.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sengang.dto.Result;
import com.sengang.dto.ScrollResult;
import com.sengang.dto.UserDTO;
import com.sengang.entity.Blog;
import com.sengang.entity.Follow;
import com.sengang.service.FollowService;
import com.sengang.mapper.FollowMapper;
import com.sengang.service.UserService;
import com.sengang.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.sengang.utils.RedisConstants.FEED_KEY;
import static com.sengang.utils.RedisConstants.FOLLOW_KEY;

/**
* @author SenGang
* @description 针对表【tb_follow】的数据库操作Service实现
* @createDate 2024-04-12 22:07:30
*/
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow>
    implements FollowService{

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private UserService userService;
    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        //判断isFollow,为true则添加follow表,为false则删除follow表对应的记录
        UserDTO user = UserHolder.getUser();
        Long userId = user.getId();
        if(isFollow){
            //关注
            Follow follow = new Follow();
            follow.setFollowUserId(followUserId);
            follow.setUserId(userId);
            boolean isSave = save(follow);//保存到数据库
            //保存到redis
            if(isSave)
                stringRedisTemplate.opsForSet().add(FOLLOW_KEY+userId,followUserId.toString());
        }else {
            //取关
            remove(new QueryWrapper<Follow>().eq("user_id",userId).eq("follow_user_id",followUserId));
            stringRedisTemplate.opsForSet().remove(FOLLOW_KEY+userId,followUserId.toString());
        }
        return Result.ok();
    }

    @Override
    public Result ifFollow(Long followUserId) {
        Long userId = UserHolder.getUser().getId();
        //查询follow表是否有这条记录就行
        Integer count = query().eq("follow_user_id", followUserId).eq("user_id", userId).count();
        //有说明关注了,没有说明没关注
        return Result.ok(count>0);
    }

    @Override
    public Result commonFollow(Long userId) {
        Long id = UserHolder.getUser().getId();
        //求交集
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(FOLLOW_KEY+userId, FOLLOW_KEY+id);
        //转成存id的list
        if (intersect==null|| intersect.isEmpty())
            return Result.ok(Collections.emptyList());
        List<Long> list = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        //通过id查user的list并转成userDTO
        List<UserDTO> userDTOS = userService.listByIds(list).stream().map(user -> BeanUtil.copyProperties(user, UserDTO.class)).collect(Collectors.toList());
        return Result.ok(userDTOS);
    }


}




