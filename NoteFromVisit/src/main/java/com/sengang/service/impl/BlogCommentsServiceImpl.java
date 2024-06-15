package com.sengang.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sengang.entity.BlogComments;
import com.sengang.service.BlogCommentsService;
import com.sengang.mapper.BlogCommentsMapper;
import org.springframework.stereotype.Service;

/**
* @author SenGang
* @description 针对表【tb_blog_comments】的数据库操作Service实现
* @createDate 2024-04-07 21:43:18
*/
@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments>
    implements BlogCommentsService{


}




