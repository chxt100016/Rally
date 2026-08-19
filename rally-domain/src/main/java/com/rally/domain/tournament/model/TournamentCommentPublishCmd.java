package com.rally.domain.tournament.model;

import com.rally.domain.meetup.enums.ChatContentTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发布赛事评论命令。
 */
@Data
public class TournamentCommentPublishCmd {

    @NotBlank(message = "tournamentId不能为空")
    private String tournamentId;

    @NotBlank(message = "content不能为空")
    private String content;

    @NotNull(message = "contentType不能为空")
    private ChatContentTypeEnum contentType;
}
