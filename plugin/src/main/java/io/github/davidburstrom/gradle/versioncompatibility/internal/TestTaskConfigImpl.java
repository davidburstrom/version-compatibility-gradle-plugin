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
