package com.sengang.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * @TableName tb_blog
 */
@TableName(value ="tb_blog")//指定表名
@Data
public class Blog implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long shopId;

    private Long userId;

    /**
     * 该Blog对应用户的icon，需要从User表获取
     * 因此将该属性设为exist=false
     */
    @TableField(exist = false)
    private String icon;

    /**
     * 该Blog对应用户的名，需要从User表获取
     * 因此将该属性设为exist=false
     */
    @TableField(exist = false)
    private String name;

    /**
     * 是否点赞过了
     */
    @TableField(exist = false)
    private Boolean isLike;

    private String title;

    private String images;

    private String content;

    private Integer liked;

    private Integer comments;

    private Date createTime;

    private Date updateTime;

    private static final long serialVersionUID = 1L;
}