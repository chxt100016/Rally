package com.rally.domain.tournament.match;

/** C1 为比赛根和每名参与者生成业务 id 的端口。 */
@FunctionalInterface
public interface TournamentMatchIdGenerator {

    String nextId();
}
