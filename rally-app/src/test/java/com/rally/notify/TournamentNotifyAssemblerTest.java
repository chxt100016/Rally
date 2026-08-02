package com.rally.notify;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TournamentNotifyAssemblerTest {

    @Test
    public void matchedPhraseMeetsWechatTemplateLimit() {
        Map<String, Object> data = TournamentNotifyAssembler.matchedData("周末网球挑战赛");

        assertEquals("周末网球挑战赛", data.get("thing7"));
        assertEquals("匹配成功", data.get("phrase2"));
        assertEquals(4, ((String) data.get("phrase2")).length());
    }
}
