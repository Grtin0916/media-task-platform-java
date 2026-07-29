package com.ryan.media.ranking;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class RankerRegistryRepository {
    private final Map<String, RankerVersion> versions = new LinkedHashMap<>();

    public synchronized RankerVersion find(String version) {
        return versions.get(version);
    }

    public synchronized List<RankerVersion> findAll() {
        return List.copyOf(new ArrayList<>(versions.values()));
    }

    public synchronized void save(RankerVersion version) {
        versions.put(version.rankerVersion(), version);
    }
}
