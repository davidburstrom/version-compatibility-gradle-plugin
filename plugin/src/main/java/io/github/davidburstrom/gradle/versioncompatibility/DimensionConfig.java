package io.github.davidburstrom.gradle.versioncompatibility;

import org.gradle.api.provider.SetProperty;

public interface DimensionConfig {

  /**
   * Gets the name of the dimension.
   *
   * @return the name.
   */
  String getName();

  /**
   * Gets the set property that contains the versions for the dimension.
   *
   * <p>The property must be set to contain at least one version.
   *
   * @return the version set property.
   */
  SetProperty<String> getVersions();
}
