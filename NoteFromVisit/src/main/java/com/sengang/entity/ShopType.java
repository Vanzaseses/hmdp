package com.sengang.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * @TableName tb_shop_type
 */
@TableName(value ="tb_shop_type")
@Data
public class ShopType implements Serializable {
    private Long id;

    private String name;

    private String icon;

    private Integer sort;

    private Date createTime;

    private Date updateTime;

    private static final long serialVersionUID = 1L;
}