package com.sengang.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @TableName tb_follow
 */
@TableName(value ="tb_follow")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Data
public class Follow implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long followUserId;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}