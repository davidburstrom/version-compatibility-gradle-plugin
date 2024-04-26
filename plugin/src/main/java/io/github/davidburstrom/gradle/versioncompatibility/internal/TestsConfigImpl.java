/*
 * Copyright 2022-2024 David Burström
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.davidburstrom.gradle.versioncompatibility.internal;

import io.github.davidburstrom.gradle.versioncompatibility.TestRuntimeOnlyConfig;
import io.github.davidburstrom.gradle.versioncompatibility.TestTaskConfig;
import io.github.davidburstrom.gradle.versioncompatibility.TestsConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import org.gradle.api.Action;

public abstract class TestsConfigImpl implements TestsConfig {
  private final List<Predicate<List<String>>> filterPredicates = new ArrayList<>();
  private final List<Action<TestRuntimeOnlyConfig>> testRuntimeOnlyAction = new ArrayList<>();
  private final List<Action<TestTaskConfig>> eachTestTaskAction = new ArrayList<>();

  @Override
  public void filter(final Predicate<List<String>> versionTuplePredicate) {
    filterPredicates.add(versionTuplePredicate);
  }

  @Override
  public void eachTestRuntimeOnly(@Nonnull Action<TestRuntimeOnlyConfig> action) {
    testRuntimeOnlyAction.add(action);
  }

  @Override
  public void eachTestTask(@Nonnull Action<TestTaskConfig> action) {
    eachTestTaskAction.add(action);
  }

  public List<Predicate<List<String>>> getFilterPredicates() {
    return filterPredicates;
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
