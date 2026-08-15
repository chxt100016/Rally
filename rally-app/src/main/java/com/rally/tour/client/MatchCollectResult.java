package com.rally.tour.client;

import com.rally.tour.model.Discipline;
import com.rally.tour.model.Match;
import com.rally.tour.model.Player;
import com.rally.tour.model.TournamentEntry;
import com.rally.tour.parser.DrawMeta;
import lombok.Value;

import java.util.List;

/**
 * Canonical result returned by every match collection client.
 *
 * <p>{@code drawId} is intentionally absent here. It is a local persistence
 * identifier and is attached by {@code MatchCollectManager} after the draw has
 * been saved.</p>
 */
@Value
public class MatchCollectResult {
    Discipline discipline;
    String drawTypeCode;
    DrawMeta drawMeta;
    String tournamentId;
    int year;
    List<Match> matches;
    List<Player> players;
    List<TournamentEntry> entries;
}
