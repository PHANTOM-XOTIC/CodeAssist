package com.tyron.builder.internal.component.external.model;

import com.google.common.base.Objects;
import com.tyron.builder.api.artifacts.ModuleVersionIdentifier;
import com.tyron.builder.api.capabilities.Capability;

import javax.annotation.Nullable;

public class ImmutableCapability implements CapabilityInternal {

    public static ImmutableCapability defaultCapabilityForComponent(ModuleVersionIdentifier identifier) {
        return new ImmutableCapability(identifier.getGroup(), identifier.getName(), identifier.getVersion());
    }

    private final String group;
    private final String name;
    private final String version;
    private final int hashCode;
    private final String cachedId;

    public ImmutableCapability(String group, String name, @Nullable String version) {
        this.group = group;
        this.name = name;
        this.version = version;

        this.hashCode = computeHashcode(group, name, version);

        // Using a string instead of a plain ID here might look strange, but this turned out to be
        // the fastest of several experiments, including:
        //
        //    using ModuleIdentifier (initial implementation)
        //    using ModuleIdentifier through ImmutableModuleIdentifierFactory (for interning)
        //    using a 2-level map (by group, then by name)
        //    using an interned string for the cachedId (interning turned out to cost as much as what we gain from faster checks in maps)
        //
        // And none of them reached the performance of just using a good old string
        this.cachedId = group + ":" + name;
    }

    private int computeHashcode(String group, String name, @Nullable String version) {
        // Do NOT change the order of members used in hash code here, it's been empirically
        // tested to reduce the number of collisions on a large dependency graph (performance test)
        int hash = safeHash(version);
        hash = 31 * hash + name.hashCode();
        hash = 31 * hash + group.hashCode();
        return  hash;
    }

    private static int safeHash(@Nullable String o) {
        return o == null ? 0 : o.hashCode();
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    @Nullable
    public String getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Capability)) {
            return false;
        }
        Capability that = (Capability) o;
        return Objects.equal(group, that.getGroup())
            && Objects.equal(name, that.getName())
            && Objects.equal(version, that.getVersion());
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "capability "
            + "group='" + group + '\''
            + ", name='" + name + '\''
            + ", version='" + version + '\'';
    }

    @Override
    public String getCapabilityId() {
        return cachedId;
    }
}
