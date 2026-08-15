package com.rally.tour.client;

import com.rally.tour.parser.CollectType;
import com.rally.tour.parser.DrawParams;

import java.util.List;

/**
 * A source-specific match collection client.
 *
 * <p>The client owns both transport response parsing and conversion to Rally's
 * canonical collection model. Callers never see an upstream response type.</p>
 */
public interface MatchCollectClient {

    List<MatchCollectResult> collect(DrawParams params);

    CollectType collectType();
}
