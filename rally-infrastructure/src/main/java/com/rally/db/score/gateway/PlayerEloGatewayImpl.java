package com.rally.db.score.gateway;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.db.score.entity.PlayerEloPO;
import com.rally.db.score.repository.PlayerEloRepository;
import com.rally.domain.config.gateway.ConfigGateway;
import com.rally.domain.score.gateway.PlayerEloGateway;
import com.rally.domain.score.model.EloResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ELO 聚合表网关实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerEloGatewayImpl implements PlayerEloGateway {

    private final PlayerEloRepository playerEloRepository;
    private final ConfigGateway config;

    @Override
    public float getEloScore(String userId) {
        PlayerEloPO po = playerEloRepository.findByUserId(userId);
        if (po != null) {
            return po.getEloScore();
        }
        // 不存在则返回初始值
        return config.getFloat("score.elo.initial", 1500);
    }

    @Override
    public void batchUpsert(List<EloResult> results) {
        List<PlayerEloPO> toSave = new ArrayList<>();

        for (EloResult result : results) {
            PlayerEloPO existing = playerEloRepository.findByUserId(result.getUserId());

            if (existing != null) {
                // 更新
                existing.setEloScore(result.getAfter());
                existing.setMatchCount(existing.getMatchCount() + result.getMatchCount());
                toSave.add(existing);
            } else {
                // 新增
                PlayerEloPO po = new PlayerEloPO();
                po.setBizId(IdWorker.getIdStr());
                po.setUserId(result.getUserId());
                po.setEloScore(result.getAfter());
                po.setMatchCount(result.getMatchCount());
                toSave.add(po);
            }
        }

        if (!toSave.isEmpty()) {
            playerEloRepository.saveBatch(toSave);
            log.info("ELO 批量更新完成，数量={}", toSave.size());
        }
    }
}
