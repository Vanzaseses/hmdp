package com.sengang.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * @TableName tb_blog_comments
 */
@TableName(value ="tb_blog_comments")
@Data
public class BlogComments implements Serializable {
    private Long id;

    private Long userId;

    private Long blogId;

    private Long parentId;

    private Long answerId;

    private String content;

    private Integer liked;

    private Integer status;

    private Date createTime;

    private Date updateTime;

    private static final long serialVersionUID = 1L;
}