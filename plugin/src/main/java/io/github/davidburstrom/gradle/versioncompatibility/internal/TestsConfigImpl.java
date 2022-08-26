package io.github.davidburstrom.gradle.versioncompatibility.internal;

import io.github.davidburstrom.gradle.versioncompatibility.TestRuntimeOnlyConfig;
import io.github.davidburstrom.gradle.versioncompatibility.TestTaskConfig;
import io.github.davidburstrom.gradle.versioncompatibility.TestsConfig;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.gradle.api.Action;

public abstract class TestsConfigImpl implements TestsConfig {
  private final List<Action<TestRuntimeOnlyConfig>> testRuntimeOnlyAction = new ArrayList<>();
  private final List<Action<TestTaskConfig>> eachTestTaskAction = new ArrayList<>();

  @Override
  public void eachTestRuntimeOnly(@Nonnull Action<TestRuntimeOnlyConfig> action) {
    testRuntimeOnlyAction.add(action);
  }

  @Override
  public void eachTestTask(@Nonnull Action<TestTaskConfig> action) {
    eachTestTaskAction.add(action);
  }

  @Nonnull
  public List<Action<TestRuntimeOnlyConfig>> getTestRuntimeOnlyAction() {
    return testRuntimeOnlyAction;
  }

  @Nonnull
  public List<Action<TestTaskConfig>> getEachTestTaskAction() {
    return eachTestTaskAction;
  }
}
