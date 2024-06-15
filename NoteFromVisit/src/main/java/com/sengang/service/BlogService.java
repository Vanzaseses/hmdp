package com.sengang.service;

import com.sengang.dto.Result;
import com.sengang.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author SenGang
* @description 针对表【tb_blog】的数据库操作Service
* @createDate 2024-04-07 21:44:45
*/
public interface BlogService extends IService<Blog> {

    Result queryBlogById(Long id);

    Result queryHotBlog(Integer current);

    Result likeBlog(Long id);

    Result likesBlog(Long id);

    Result saveBlog(Blog blog);

    Result queryBlogOfFollow(Long max, Integer offset);
}
