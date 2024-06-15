package com.sengang.dto;

import lombok.Data;

import java.util.List;

/***
 * @description: TODO :滚动分页返回对象
 * @params:
 * @return:
 * @author: SenGang
 */

@Data
public class ScrollResult {
    //ids
    private List<?> list;
    //最小的值的id
    private Long minTime;
    //偏移量
    private Integer offset;
}
