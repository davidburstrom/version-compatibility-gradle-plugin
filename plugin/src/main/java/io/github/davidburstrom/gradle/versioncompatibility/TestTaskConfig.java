/*
 * Copyright 2022-2023 David Burström
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
package io.github.davidburstrom.gradle.versioncompatibility;

import javax.annotation.Nonnull;
import org.gradle.api.tasks.testing.Test;

public interface TestTaskConfig extends HasVersionTuple {
  /**
   * Gets the test task to be configured, based on the versions provided through {@link
   * #getVersions()}
   *
   * @return The test task.
   */
  @Nonnull
  Test getTestTask();
}
