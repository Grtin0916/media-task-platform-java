package com.ryan.media.ranking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RankerRecommendationHistoryTest {
    @Test
    void appendsWithoutOverwritingOlderVersion() {
        RecommendationHistory history = new RecommendationHistory();
        history.append(item("r1", "v1", null));
        history.append(item("r2", "v2", "r1"));
        assertEquals(2, history.forCase("case-1").size());
        assertEquals("v1", history.forCase("case-1").get(0).rankerVersion());
        assertEquals("r1", history.forCase("case-1").get(1).supersedesRecommendationId());
    }

    @Test
    void unknownCaseHasEmptyImmutableHistory() {
        RecommendationHistory history = new RecommendationHistory();
        assertEquals(0, history.forCase("missing").size());
    }

    private RankerRecommendation item(String id, String version, String supersedes) {
        return new RankerRecommendation(
                id, "case-1", version, version.repeat(32).substring(0, 64),
                "f".repeat(64), "A", 0.5, 0.1,
                RankerRecommendationStatus.NEEDS_HUMAN_REVIEW,
                "test", Instant.now(), supersedes);
    }
}
