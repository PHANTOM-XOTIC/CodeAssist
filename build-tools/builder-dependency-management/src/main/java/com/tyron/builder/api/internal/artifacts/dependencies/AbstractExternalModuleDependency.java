package com.tyron.builder.api.internal.artifacts.dependencies;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.tyron.builder.api.Action;
import com.tyron.builder.api.InvalidUserDataException;
import com.tyron.builder.api.artifacts.ExternalModuleDependency;
import com.tyron.builder.api.artifacts.ModuleIdentifier;
import com.tyron.builder.api.artifacts.ModuleVersionIdentifier;
import com.tyron.builder.api.artifacts.MutableVersionConstraint;
import com.tyron.builder.api.artifacts.VersionConstraint;
import com.tyron.builder.api.internal.artifacts.DefaultModuleIdentifier;
import com.tyron.builder.api.internal.artifacts.ModuleVersionSelectorStrictSpec;
import com.tyron.builder.internal.deprecation.DeprecationLogger;

import javax.annotation.Nullable;

public abstract class AbstractExternalModuleDependency extends AbstractModuleDependency implements ExternalModuleDependency {
    private final ModuleIdentifier moduleIdentifier;
    private boolean changing;
    private boolean force;
    private final DefaultMutableVersionConstraint versionConstraint;

    public AbstractExternalModuleDependency(ModuleIdentifier module, String version, @Nullable String configuration) {
        super(configuration);
        if (module == null) {
            throw new InvalidUserDataException("Module must not be null!");
        }
        this.moduleIdentifier = module;
        this.versionConstraint = new DefaultMutableVersionConstraint(version);
    }

    public AbstractExternalModuleDependency(ModuleIdentifier module, MutableVersionConstraint version) {
        super(null);
        if (module == null) {
            throw new InvalidUserDataException("Module must not be null!");
        }
        this.moduleIdentifier = module;
        this.versionConstraint = (DefaultMutableVersionConstraint) version;
    }

    protected void copyTo(AbstractExternalModuleDependency target) {
        super.copyTo(target);
        DeprecationLogger.whileDisabled(() -> target.setForce(isForce()));
        target.setChanging(isChanging());
    }

    protected boolean isContentEqualsFor(ExternalModuleDependency dependencyRhs) {
        if (!isCommonContentEquals(dependencyRhs)) {
            return false;
        }
        return force == dependencyRhs.isForce() && changing == dependencyRhs.isChanging() &&
            Objects.equal(getVersionConstraint(), dependencyRhs.getVersionConstraint());
    }

    @Override
    public boolean matchesStrictly(ModuleVersionIdentifier identifier) {
        return new ModuleVersionSelectorStrictSpec(this).test(identifier);
    }

    @Override
    public String getGroup() {
        return moduleIdentifier.getGroup();
    }

    @Override
    public String getName() {
        return moduleIdentifier.getName();
    }

    @Override
    public String getVersion() {
        return Strings.emptyToNull(versionConstraint.getVersion());
    }

    @Override
    public boolean isForce() {
        return force;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ExternalModuleDependency setForce(boolean force) {
        validateMutation(this.force, force);
        if (force) {
            DeprecationLogger.deprecate("Using force on a dependency")
                .withAdvice("Consider using strict version constraints instead (version { strictly ... } }).")
                .willBeRemovedInGradle8()
                .withUpgradeGuideSection(5, "forced_dependencies")
                .nagUser();
        }
        this.force = force;
        return this;
    }

    @Override
    public boolean isChanging() {
        return changing;
    }

    @Override
    public ExternalModuleDependency setChanging(boolean changing) {
        validateMutation(this.changing, changing);
        this.changing = changing;
        return this;
    }

    @Override
    public VersionConstraint getVersionConstraint() {
        return versionConstraint;
    }

    @Override
    public void version(Action<? super MutableVersionConstraint> configureAction) {
        validateMutation();
        configureAction.execute(versionConstraint);
    }

    @Override
    public ModuleIdentifier getModule() {
        return moduleIdentifier;
    }

    static ModuleIdentifier assertModuleId(@Nullable String group, @Nullable String name) {
        if (name == null) {
            throw new InvalidUserDataException("Name must not be null!");
        }
        return DefaultModuleIdentifier.newId(group, name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractExternalModuleDependency that = (AbstractExternalModuleDependency) o;
        return isContentEqualsFor(that);
    }

    @Override
    public int hashCode() {
        int result = getGroup() != null ? getGroup().hashCode() : 0;
        result = 31 * result + getName().hashCode();
        result = 31 * result + (getVersion() != null ? getVersion().hashCode() : 0);
        return result;
    }
}
