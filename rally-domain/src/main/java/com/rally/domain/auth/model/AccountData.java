package com.rally.domain.auth.model;

import com.rally.domain.auth.enums.ChannelEnum;
import lombok.Data;

@Data
public class AccountData {
    private String accountId;
    private String userId;
    private ChannelEnum channel;
    private String identifier;
    private String unionId;
}
