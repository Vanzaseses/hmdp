package com.sengang.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * @TableName tb_user_info
 */
@TableName(value ="tb_user_info")
@Data
public class UserInfo implements Serializable {

    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    private String city;

    private String introduce;

    private Integer fans;

    private Integer followee;

    private Integer gender;

    private Date birthday;

    private Integer credits;

    private Integer level;

    private Date createTime;

    private Date updateTime;

    private static final long serialVersionUID = 1L;
}