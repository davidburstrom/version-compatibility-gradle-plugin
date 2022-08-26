package io.github.davidburstrom.gradle.versioncompatibility.internal;

import io.github.davidburstrom.gradle.versioncompatibility.TestRuntimeOnlyConfig;
import java.util.List;
import javax.annotation.Nonnull;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;

public class TestRuntimeOnlyConfigImpl implements TestRuntimeOnlyConfig {
  private final List<String> versions;
  private final DependencyHandler dependencyHandler;
  private final Configuration targetConfiguration;

  public TestRuntimeOnlyConfigImpl(
      @Nonnull List<String> versions,
      @Nonnull DependencyHandler dependencyHandler,
      @Nonnull Configuration targetConfiguration) {
    this.versions = versions;
    this.dependencyHandler = dependencyHandler;
    this.targetConfiguration = targetConfiguration;
  }

  @Nonnull
  @Override
  public List<String> getVersions() {
    return versions;
  }

  @Override
  public void addConstraint(@Nonnull Object constraint) {
    targetConfiguration
        .getDependencyConstraints()
        .add(dependencyHandler.getConstraints().create(constraint));
  }
}
