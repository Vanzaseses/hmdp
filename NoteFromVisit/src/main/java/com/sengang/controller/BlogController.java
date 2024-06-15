package com.sengang.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sengang.dto.Result;
import com.sengang.dto.UserDTO;
import com.sengang.entity.Blog;
import com.sengang.entity.User;
import com.sengang.service.BlogService;
import com.sengang.service.UserService;
import com.sengang.utils.SystemConstants;
import com.sengang.utils.UserHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/blog")
public class BlogController {
    @Resource
    private BlogService blogService;
    @Resource
    private JdbcTemplate jdbcTemplate;

    /***
     * @description: TODO :添加blog到数据库,并推送给粉丝
     * @params: [blog]
     * @return: com.sengang.dto.Result
     * @author: SenGang
     */
    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        return blogService.saveBlog(blog);
    }
    /***
     * @description: TODO :根绝用户查询对应博客
     * @params: [current, id]
     * @return: com.sengang.dto.Result
     * @author: SenGang
     */

    @GetMapping("/of/user")
    public Result queryBlogByUserId(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam("id") Long id) {
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", id).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    @GetMapping("/{id}")
    public Result queryBlogById(@PathVariable(value = "id")Long id){

        return blogService.queryBlogById(id);
    }
    /***
     * @description: TODO :点赞
     * @params: [id]
     * @return: com.sengang.dto.Result
     * @author: SenGang
     */
    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        return blogService.likeBlog(id);
    }

    /***
     * @description: TODO :返回点赞时间最早的5位用户
     * @params: [id]
     * @return: com.sengang.dto.Result
     * @author: SenGang
     */

    @GetMapping("/likes/{id}")
    public Result likesBlog(@PathVariable("id")Long id){
        return blogService.likesBlog(id);
    }
    /***
     * @description: TODO :返回我的主页中自己的BLOG
     * @params: [current]
     * @return: com.sengang.dto.Result
     * @author: SenGang
     */
    @GetMapping("of/me")
    public Result queryMyBlog(@RequestParam(value = "current",defaultValue = "1")Integer current){
        //通过ThreadLocal获得user
        UserDTO user = UserHolder.getUser();
        Page<Blog> myBlog = blogService.query().eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        return Result.ok(myBlog.getRecords());

    }

    /***
     * @description: TODO:返回首页热点信息
     * @params: [current]
     * @return: com.sengang.dto.Result
     * @author: SenGang
     */
    @GetMapping("hot")
    public Result queryHotBlog(@RequestParam(value = "current",defaultValue = "1")Integer current){

        return blogService.queryHotBlog(current);
    }
    @GetMapping("/of/follow")
    public Result queryBlogOfFollow(@RequestParam("lastId")Long max,@RequestParam(value = "offset",defaultValue = "0")Integer offset){
        return blogService.queryBlogOfFollow(max,offset);
    }

}
