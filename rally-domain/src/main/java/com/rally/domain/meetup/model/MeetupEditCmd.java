package com.rally.domain.meetup.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 编辑约球入参
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MeetupEditCmd extends MeetupPublishCmd {

    /** 约球ID，编辑时必传 */
    @NotBlank(message = "约球ID不能为空")
    private String meetupId;
}
