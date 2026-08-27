package com.rally.domain.tournament.entry;

/** 在领域边界外提供报名业务 id。 */
@FunctionalInterface
public interface TournamentEntryIdGenerator {
    String nextId();
}
