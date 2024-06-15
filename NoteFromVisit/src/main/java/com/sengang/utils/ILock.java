package com.sengang.utils;

public interface ILock {
    /***
     * @description: TODO :尝试获取锁
     * @params: [expireTime：锁过期时间]
     * @return: boolean
     * @author: SenGang
     */
    boolean tryLock(Long expireTime);
    /***
     * @description: TODO :释放锁
     */
    void unLock();
}
