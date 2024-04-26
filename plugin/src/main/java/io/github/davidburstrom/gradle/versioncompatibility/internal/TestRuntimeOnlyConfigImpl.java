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
