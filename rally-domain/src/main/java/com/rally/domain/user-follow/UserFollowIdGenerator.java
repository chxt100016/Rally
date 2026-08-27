package com.rally.domain.social.userfollow;

/** C1 首次建立关注时生成雪花业务编号的端口。 */
@FunctionalInterface
public interface UserFollowIdGenerator {

    String nextBizId();
}
