package io.github.davidburstrom.gradle.versioncompatibility.internal;

import io.github.davidburstrom.gradle.versioncompatibility.TestTaskConfig;
import java.util.List;
import javax.annotation.Nonnull;
import org.gradle.api.tasks.testing.Test;

public class TestTaskConfigImpl implements TestTaskConfig {
  private final Test test;
  private final List<String> versions;

  public TestTaskConfigImpl(@Nonnull Test test, @Nonnull List<String> versions) {
    this.test = test;
    this.versions = versions;
  }

  @Nonnull
  @Override
  public Test getTestTask() {
    return test;
  }

  @Nonnull
  @Override
  public List<String> getVersions() {
    return versions;
  }
}
