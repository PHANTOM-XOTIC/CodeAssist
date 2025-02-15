package com.tyron.builder.cache.internal;

import static com.tyron.builder.api.internal.DocumentationRegistry.*;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.tyron.builder.api.internal.DocumentationRegistry;

import java.util.Map;
import java.util.NavigableMap;

public class CacheVersionMapping {

    private static final Function<Map.Entry<GradleVersion, CacheVersion>, CacheVersion> TO_VALUE = new Function<Map.Entry<GradleVersion, CacheVersion>, CacheVersion>() {
        @Override
        public CacheVersion apply(Map.Entry<GradleVersion, CacheVersion> input) {
            return input.getValue();
        }
    };

    private final NavigableMap<GradleVersion, CacheVersion> versions;

    private CacheVersionMapping(NavigableMap<GradleVersion, CacheVersion> versions) {
        Preconditions.checkArgument(!versions.isEmpty(), "versions must not be empty");
        this.versions = Maps.newTreeMap(versions);
    }

    public CacheVersion getLatestVersion() {
        return versions.get(versions.lastKey());
    }

    public Optional<CacheVersion> getVersionUsedBy(GradleVersion gradleVersion) {
        GradleVersion versionToFind = gradleVersion.isSnapshot() ? gradleVersion.getBaseVersion() : gradleVersion;
        return Optional.fromNullable(versions.floorEntry(versionToFind)).transform(TO_VALUE);
    }

    public static Builder introducedIn(String gradleVersion) {
        return new Builder().changedTo(1, gradleVersion);
    }

    public static class Builder {

        private final NavigableMap<GradleVersion, Integer> versions = Maps.newTreeMap();

        private Builder() {
        }

        public Builder incrementedIn(String minGradleVersion) {
            return changedTo(versions.get(versions.lastKey()) + 1, minGradleVersion);
        }

        public Builder changedTo(int cacheVersion, String minGradleVersion) {
            GradleVersion parsedGradleVersion = GradleVersion.version(minGradleVersion);
            if (!versions.isEmpty()) {
                Preconditions.checkArgument(parsedGradleVersion.compareTo(versions.lastKey()) > 0,
                    "Gradle version (%s) must be greater than all previous versions: %s", parsedGradleVersion.getVersion(), versions.keySet());
                GradleVersion currentBaseVersion = GradleVersion.current().getBaseVersion();
                Preconditions.checkArgument(parsedGradleVersion.getBaseVersion().compareTo(currentBaseVersion) <= 0,
                    "Base version of Gradle version (%s) must not be greater than base version of current Gradle version: %s", parsedGradleVersion.getVersion(), currentBaseVersion);
                Preconditions.checkArgument(cacheVersion > versions.get(versions.lastKey()),
                    "cache version (%s) must be greater than all previous versions: %s", cacheVersion, versions.values());
            }
            versions.put(parsedGradleVersion, cacheVersion);
            return this;
        }

        public CacheVersionMapping build() {
            return build(CacheVersion.empty());
        }

        public CacheVersionMapping build(CacheVersion parentVersion) {
            NavigableMap<GradleVersion, CacheVersion> convertedVersions = Maps.newTreeMap();
            for (Map.Entry<GradleVersion, Integer> entry : versions.entrySet()) {
                convertedVersions.put(entry.getKey(), parentVersion.append(entry.getValue()));
            }
            return new CacheVersionMapping(convertedVersions);
        }
    }
}
