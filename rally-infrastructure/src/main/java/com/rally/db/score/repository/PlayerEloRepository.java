package com.rally.db.score.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rally.db.score.entity.PlayerEloPO;
import com.rally.db.score.mapper.PlayerEloMapper;
import com.rally.db.score.service.PlayerEloService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ELO 聚合表 Repository 门面
 */
@Repository
@RequiredArgsConstructor
public class PlayerEloRepository {

    private final PlayerEloMapper playerEloMapper;
    private final PlayerEloService playerEloService;

    /**
     * 根据用户 ID 查询
     */
    public PlayerEloPO findByUserId(String userId) {
        LambdaQueryWrapper<PlayerEloPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlayerEloPO::getUserId, userId);
        return playerEloMapper.selectOne(wrapper);
    }

    /**
     * 根据用户 ID 列表批量查询
     */
    public List<PlayerEloPO> findByUserIds(List<String> userIds) {
        LambdaQueryWrapper<PlayerEloPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PlayerEloPO::getUserId, userIds);
        return playerEloMapper.selectList(wrapper);
    }

    /**
     * 保存（新增或更新）
     */
    public void save(PlayerEloPO po) {
        playerEloService.saveOrUpdate(po);
    }

    /**
     * 批量保存或更新
     */
    public void saveBatch(List<PlayerEloPO> list) {
        playerEloService.saveOrUpdateBatch(list);
    }
}
