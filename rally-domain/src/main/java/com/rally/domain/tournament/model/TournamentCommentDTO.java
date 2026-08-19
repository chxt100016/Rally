package com.rally.domain.tournament.model;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 赛事评论。
 */
@Data
public class TournamentCommentDTO {

    private String commentId;
    private String tournamentId;
    private String senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private String contentType;

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
