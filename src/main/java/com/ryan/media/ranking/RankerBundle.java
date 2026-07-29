package com.ryan.media.ranking;

import java.nio.file.Path;

public record RankerBundle(Path sourceDirectory, RankerBundleManifest manifest) {
}
