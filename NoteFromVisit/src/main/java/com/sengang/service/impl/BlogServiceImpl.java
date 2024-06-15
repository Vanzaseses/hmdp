package com.sengang.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.setting.SettingUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sengang.dto.Result;
import com.sengang.dto.ScrollResult;
import com.sengang.dto.UserDTO;
import com.sengang.entity.Blog;
import com.sengang.entity.Follow;
import com.sengang.entity.User;
import com.sengang.service.BlogService;
import com.sengang.mapper.BlogMapper;
import com.sengang.service.FollowService;
import com.sengang.service.UserService;
import com.sengang.utils.SystemConstants;
import com.sengang.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.sengang.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.sengang.utils.RedisConstants.FEED_KEY;

/**
* @author SenGang
* @description 针对表【tb_blog】的数据库操作Service实现
* @createDate 2024-04-07 21:44:45
*/
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog>
    implements BlogService{
    @Resource
    private UserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private FollowService followService;

    @Override
    public Result queryBlogById(Long id) {
        Blog blog = getById(id);
        if(blog==null){
            return Result.fail("笔记不存在");
        }
        //查询blog有关用户
        queryBlogUser(blog);
        //判断用户是否点赞 TODO:对于该用户返回的所有blog都必须检查该用户是否点赞过
/*        Boolean isLiked = isBlogLiked(id);
        if(isLiked){
            blog.setIsLike(true);
        }*/
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            queryBlogUser(blog);
            isBlogLiked(blog);;
        });
        return Result.ok(records);
    }
    /***
     * @description: TODO :查询blog是否被点赞
     * @params: [blog]
     * @return: void
     * @author: SenGang
     */
    private void isBlogLiked(Blog blog) {
        //在redis中对应blog的set是否存在该用户id,存在返回true
//        blog.setIsLike(BooleanUtil.isTrue(stringRedisTemplate.opsForSet().isMember(BLOG_LIKED_KEY + blog.getId(), UserHolder.getUser().getId().toString())));
        //改成ZSET,带排序功能,score不为null就点赞
        UserDTO user = UserHolder.getUser();//判断是否为游客
        if (user!=null)
            //修改传入的blog对象对应的isLike属性，返回给前端
            blog.setIsLike(stringRedisTemplate.opsForZSet().score(BLOG_LIKED_KEY + blog.getId(), user.getId().toString())!=null);
    }
    /***
     * @description: TODO :查询blog有关用户
     * @params: [blog]
     * @return: void
     * @author: SenGang
     */

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setIcon(user.getIcon());
        blog.setName(user.getNickName());
    }
    @Override
    public Result likeBlog(Long id) {
        // 1.获取登录用户
        UserDTO user = UserHolder.getUser();
        // 2.判断当前用户是否已经点赞
        String key = BLOG_LIKED_KEY+id;
        Double score = stringRedisTemplate.opsForZSet().score(key, user.getId().toString());
        // 3.如果未点赞
        if(score==null){
            // 3.1 数据库对应点赞数量+1 TODO:先改数据库再改redis
            boolean success = update().setSql("liked = liked + 1").eq("id", id).update();
            if(success){
                // 3.2 保存用户到redis对应zset中，score是时间戳
                stringRedisTemplate.opsForZSet().add(key,user.getId().toString(), System.currentTimeMillis());
            }
        }else{
            // 4.如果已经点赞,取消点赞
            // 4.1 数据库对应点赞数量-1
            boolean success = update().setSql("liked = liked - 1").eq("id", id).update();
            if(success) {
                // 4.2 redis对应set中删除对应用户
                stringRedisTemplate.opsForZSet().remove(key,user.getId().toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result likesBlog(Long id) {
        //获取对应blog,前5名用户点赞数
        Set<String> userIds = stringRedisTemplate.opsForZSet().range(BLOG_LIKED_KEY + id, 0l, 4L);
        if(userIds==null||userIds.isEmpty())
            return Result.ok(Collections.emptyList());
        //获取id集合
        ArrayList<String> list = ListUtil.toList(userIds);
        //拼接成id1,id2,id3的str
        String idStr = StrUtil.join(",", userIds);
        //获取user集合转成userDTO-> select  ... where id in(xx,xx,xx) order by (id,xx,xx,xx),TODO:自定义根据传入的id排序,sql默认排序是乱序
        List<UserDTO> userDTOS = userService.query().in("id",userIds).last("order by field(id,"+idStr+")")//自定义sql语句
                .list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))//转换
                .collect(Collectors.toList());//收集
        return Result.ok(userDTOS);
    }

    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        boolean isSave = save(blog);
        if (!isSave)
            return Result.fail("笔记保存失败");
        //查询作者的所有粉丝
        List<Follow> followList = followService.query().eq("follow_user_id", blog.getUserId()).list();
        //推送id给所有粉丝
        for (Follow follow :followList) {
            //获取粉丝id
            Long userId = follow.getUserId();
            String key = FEED_KEY+ userId;
            //添加blogId到粉丝收件箱，zset
            stringRedisTemplate.opsForZSet().add(key,blog.getId().toString(),System.currentTimeMillis());
        }

        // 返回id
        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        Long id = UserHolder.getUser().getId();
        //TODO:滚动分页,根据传入的max锁定上次最小的数据在zset的位置,根据offset确认遍历开始位置 max+offset,避免最小的值重复导致下标定义到了最上面
        //取出zset对应元组 TODO：Value是BlogId Score是时间戳
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(FEED_KEY + id, 0, max, offset, 3);
        if (typedTuples.isEmpty()||typedTuples==null)
            return Result.ok();

        //创建blogId对应的list结果集
        ArrayList<Long> blogIds = new ArrayList<>(typedTuples.size());
        //最小的时间戳0
        long minTime = 0;
        //默认偏移1,即下次遍历开始的位置
        offset=1;
        //构建结果集
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            //获取时间戳
            long tmp = tuple.getScore().longValue();
            if (tmp ==minTime) {//结果相等，偏移+1
                ++offset;
            }else{//不相等偏移重置为1
                offset=1;
                minTime = tmp;
            }
            //获取blog对应id,并添加到结果集
            blogIds.add(Long.valueOf(tuple.getValue()));
        }

        //结果拼接
        String idStr = StrUtil.join(",", blogIds);
        //根据id查找blog TODO:自定义根据传入的id排序,sql默认排序是乱序
        List<Blog> blogs = query().in("id",blogIds).last("order by field(id,"+idStr+")").list();
        //查询blog是否被该用户点赞
        for (Blog blog : blogs) {
            queryBlogUser(blog);
            isBlogLiked(blog);
        }

        //封装并返回
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setMinTime(minTime);
        scrollResult.setList(blogs);
        scrollResult.setOffset(offset);

        return Result.ok(scrollResult);
    }
}




