package com.rally.db.tour.convert;

import com.rally.db.tour.entity.TourMatchPO;
import com.rally.domain.tour.model.MatchData;
import com.rally.domain.tour.model.SetScore;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TourConvertMapperTest {

    @Test
    public void scoreSnapshotRoundTripsThroughJson() {
        MatchData data = new MatchData();
        data.setSets(List.of(SetScore.builder()
                .setNumber(1)
                .p1Games(6)
                .p2Games(3)
                .build()));

        TourMatchPO po = TourConvertMapper.INSTANCE.toMatchPO(data);
        MatchData restored = TourConvertMapper.INSTANCE.toMatchData(po);

        assertEquals(1, restored.getSets().size());
        assertEquals(Integer.valueOf(6), restored.getSets().get(0).getP1Games());
        assertEquals(Integer.valueOf(3), restored.getSets().get(0).getP2Games());
    }

    @Test
    public void unknownOrEmptyScoresDoNotProduceAnUpdateValue() {
        MatchData unknown = new MatchData();
        assertNull(TourConvertMapper.INSTANCE.toMatchPO(unknown).getSetsJson());

        unknown.setSets(List.of());
        assertNull(TourConvertMapper.INSTANCE.toMatchPO(unknown).getSetsJson());
    }

    @Test
    public void missingJsonReturnsEmptyScoresForQueries() {
        TourMatchPO po = new TourMatchPO();
        MatchData data = TourConvertMapper.INSTANCE.toMatchData(po);

        assertTrue(data.getSets().isEmpty());
    }
}
