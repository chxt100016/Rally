package com.rally.db.user.convert;

import com.rally.db.user.entity.UserPO;
import com.rally.domain.user.model.UserData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserConvertMapperTest {

    @Test
    public void mapsPhoneBetweenPoAndDomainData() {
        UserPO po = new UserPO();
        po.setPhone("13800138000");

        UserData data = UserConvertMapper.INSTANCE.toData(po);
        UserPO mapped = UserConvertMapper.INSTANCE.toPO(data);

        assertEquals("13800138000", data.getPhone());
        assertEquals("13800138000", mapped.getPhone());
    }

    @Test
    public void updatePoUpdatesPhoneAndKeepsOtherFields() {
        UserPO po = new UserPO();
        po.setNickname("球员");
        po.setPhone("旧手机号");
        UserData data = new UserData();
        data.setPhone("13800138000");

        UserConvertMapper.INSTANCE.updatePO(po, data);

        assertEquals("13800138000", po.getPhone());
        assertEquals("球员", po.getNickname());
    }
}
