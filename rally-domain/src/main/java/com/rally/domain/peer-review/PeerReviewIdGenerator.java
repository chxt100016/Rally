package com.rally.domain.meetup.peerreview;

/** 首次提交某目标维度时生成雪花业务编号的端口。 */
@FunctionalInterface
public interface PeerReviewIdGenerator {

    String nextReviewId();
}
