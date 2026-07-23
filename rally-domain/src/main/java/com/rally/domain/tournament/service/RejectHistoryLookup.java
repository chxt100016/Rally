package com.rally.domain.tournament.service;

/**
 * 拒绝历史查询函数：判断两名用户是否互相拒绝过
 */
public interface RejectHistoryLookup {

    boolean hasRejected(String userIdA, String userIdB);
}
