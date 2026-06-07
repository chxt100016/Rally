package com.rally.db.review.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rally.db.review.entity.ScoreRecordPO;
import com.rally.db.review.service.ScoreRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 比分记录 Repository（门面层，含乐观锁操作）
 */
@Repository
@RequiredArgsConstructor
public class ScoreRecordRepository {

    private final ScoreRecordService scoreRecordService;

    /**
     * 新增盘记录
     */
    public void save(ScoreRecordPO po) {
        scoreRecordService.save(po);
    }

    /**
     * 根据 bizId 查询
     */
    public ScoreRecordPO findByBizId(String bizId) {
        return scoreRecordService.lambdaQuery()
                .eq(ScoreRecordPO::getBizId, bizId)
                .one();
    }

    /**
     * 查询某场全部比分（按 set_number 升序）
     */
    public List<ScoreRecordPO> listByMeetupId(String rallyMeetupId) {
        return scoreRecordService.lambdaQuery()
                .eq(ScoreRecordPO::getRallyMeetupId, rallyMeetupId)
                .orderByAsc(ScoreRecordPO::getSetNumber)
                .list();
    }

    /**
     * 乐观锁更新盘记录
     * @return 影响行数，0 表示版本冲突
     */
    public int updateWithLock(ScoreRecordPO po) {
        // 使用 MyBatis-Plus 乐观锁：@Version 注解自动拼 version 条件
        return scoreRecordService.getBaseMapper().updateById(po);
    }

    /**
     * 乐观锁删除盘记录
     * @return 影响行数，0 表示版本冲突
     */
    public int deleteByBizIdWithLock(String bizId, Integer version) {
        return scoreRecordService.getBaseMapper().delete(
                new LambdaUpdateWrapper<ScoreRecordPO>()
                        .eq(ScoreRecordPO::getBizId, bizId)
                        .eq(ScoreRecordPO::getVersion, version));
    }

    /**
     * 查询某场某盘号是否已存在
     */
    public boolean existsByMeetupAndSetNumber(String rallyMeetupId, Integer setNumber) {
        return scoreRecordService.lambdaQuery()
                .eq(ScoreRecordPO::getRallyMeetupId, rallyMeetupId)
                .eq(ScoreRecordPO::getSetNumber, setNumber)
                .count() > 0;
    }

    /**
     * 绕过 @Version 乐观锁更新（recap 流程用，手动管理 version）
     */
    public void updateWithoutVersionCheck(String bizId, ScoreRecordPO newValues, int newVersion) {
        scoreRecordService.lambdaUpdate()
                .eq(ScoreRecordPO::getBizId, bizId)
                .set(ScoreRecordPO::getSetNumber, newValues.getSetNumber())
                .set(ScoreRecordPO::getSetFormat, newValues.getSetFormat())
                .set(ScoreRecordPO::getSideAPlayer1, newValues.getSideAPlayer1())
                .set(ScoreRecordPO::getSideAPlayer2, newValues.getSideAPlayer2())
                .set(ScoreRecordPO::getSideBPlayer1, newValues.getSideBPlayer1())
                .set(ScoreRecordPO::getSideBPlayer2, newValues.getSideBPlayer2())
                .set(ScoreRecordPO::getSideAScore, newValues.getSideAScore())
                .set(ScoreRecordPO::getSideBScore, newValues.getSideBScore())
                .set(ScoreRecordPO::getRecordedBy, newValues.getRecordedBy())
                .set(ScoreRecordPO::getVersion, newVersion)
                .update();
    }

    /**
     * 根据 bizId 删除（不检查 version）
     */
    public void deleteByBizId(String bizId) {
        scoreRecordService.lambdaUpdate()
                .eq(ScoreRecordPO::getBizId, bizId)
                .remove();
    }

    /**
     * 批量更新某场所有比分的版本号（确保 meetup 级版本一致）
     */
    public void updateVersionByMeetupId(String rallyMeetupId, int newVersion) {
        scoreRecordService.lambdaUpdate()
                .eq(ScoreRecordPO::getRallyMeetupId, rallyMeetupId)
                .set(ScoreRecordPO::getVersion, newVersion)
                .update();
    }
}
