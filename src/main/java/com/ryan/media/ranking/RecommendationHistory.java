package com.ryan.media.ranking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RecommendationHistory {
    private final Map<String, List<RankerRecommendation>> byCase = new HashMap<>();

    public synchronized void append(RankerRecommendation recommendation) {
        byCase.computeIfAbsent(recommendation.caseId(), ignored -> new ArrayList<>())
                .add(recommendation);
    }

    public synchronized List<RankerRecommendation> forCase(String caseId) {
        return List.copyOf(byCase.getOrDefault(caseId, List.of()));
    }
}
