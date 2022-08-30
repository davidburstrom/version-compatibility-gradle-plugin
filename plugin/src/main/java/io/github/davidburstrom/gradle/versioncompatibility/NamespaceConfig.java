/*
 * Copyright 2022 David Burström
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

import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

public interface NamespaceConfig {
  /**
   * Gets the namespace name that will be used for the adapter source sets and configurations.
   *
   * @return the name.
   */
  String getName();

  /**
   * Gets the property with which to register the versions the namespace should contain.
   *
   * @return the property.
   */
  SetProperty<String> getVersions();

  /**
   * Gets the property with which to set the source set name whose implementation classpath the
   * adapters should be exported to.
   *
   * <p>If not set, they will be exported to "main".
   *
   * @return the property.
   */
  Property<String> getTargetSourceSetName();
}
