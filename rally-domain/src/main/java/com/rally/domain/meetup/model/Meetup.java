package com.rally.domain.meetup.model;

import com.rally.domain.meetup.enums.*;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 约球聚合根
 */
@Getter
public class Meetup {

    private final MeetupData data;

    public Meetup(MeetupData data) {
        this.data = data;
    }

    /**
     * 懒判定：计算真实状态
     */
    public MeetupStatusEnum getRealStatus() {
        if ((data.getStatus() == MeetupStatusEnum.OPEN || data.getStatus() == MeetupStatusEnum.FULL)
                && data.getEndTime().isBefore(LocalDateTime.now())) {
            return MeetupStatusEnum.FINISHED;
        }
        return data.getStatus();
    }

    /**
     * 是否可编辑
     */
    public boolean canEdit(String userId, int lockMinutes) {
        MeetupStatusEnum realStatus = getRealStatus();
        return userId.equals(data.getCreatorId())
                && realStatus != MeetupStatusEnum.FINISHED
                && realStatus != MeetupStatusEnum.CLOSED
                && LocalDateTime.now().isBefore(data.getStartTime().minusMinutes(lockMinutes));
    }

    /**
     * 是否可关闭
     */
    public boolean canClose(String userId) {
        MeetupStatusEnum realStatus = getRealStatus();
        return userId.equals(data.getCreatorId())
                && realStatus != MeetupStatusEnum.FINISHED
                && realStatus != MeetupStatusEnum.CLOSED;
    }

    /**
     * 是否为创建人
     */
    public boolean isCreator(String userId) {
        return userId.equals(data.getCreatorId());
    }

    /**
     * 是否已满
     */
    public boolean isFull() {
        return data.getCurrentPlayers() >= data.getMaxPlayers();
    }
}
