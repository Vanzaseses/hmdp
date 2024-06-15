package com.sengang.dto;

import lombok.Data;
/***
 * @description: TODO :存储登录用户前端传回的json
 * @params:
 * @return:
 * @author: SenGang
 */

@Data
public class LoginFormDTO {
    private String phone;
    private String code;
    private String password;
}
