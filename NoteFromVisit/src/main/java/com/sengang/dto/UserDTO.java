package com.sengang.dto;

import lombok.Data;
/***
 * @description: TODO ：用户的简略信息，防止直接返回user暴露敏感信息
 * @params:
 * @return:
 * @author: SenGang
 */

@Data
public class UserDTO {
    private Long id;
    private String nickName;
    private String icon;
}
