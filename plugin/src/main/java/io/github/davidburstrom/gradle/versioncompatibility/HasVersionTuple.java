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
package io.github.davidburstrom.gradle.versioncompatibility;

import java.util.List;
import javax.annotation.Nonnull;

/** Indicates that the object has an n-dimensional version tuple, for n &gt;= 1. */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface HasVersionTuple {
  /**
   * Returns a list of versions, one per dimension in the matrix of compatibility tests, in the
   * order the dimensions were registered/created.
   *
   * @return The list of versions.
   */
  @Nonnull
  List<String> getVersions();
}
