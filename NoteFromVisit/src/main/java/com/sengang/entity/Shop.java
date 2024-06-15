package com.sengang.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * @TableName tb_shop
 */
@TableName(value ="tb_shop")
@Data
public class Shop implements Serializable {
    @TableId(value = "id",type = IdType.AUTO)
    private Long id;
    private String name;

    private Long typeId;

    private String images;

    private String area;

    private String address;

    private Double x;

    private Double y;

    private Long avgPrice;

    private Integer sold;

    private Integer comments;

    private Integer score;

    private String openHours;

    private Date createTime;

    private Date updateTime;
    @TableField(exist = false)
    private Double distance;

    private static final long serialVersionUID = 1L;
}