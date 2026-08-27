package com.rally.domain.account.model;

/** 渠道与渠道唯一标识组成的身份键。 */
public record AccountIdentity(AccountChannel channel, String identifier) {
}
