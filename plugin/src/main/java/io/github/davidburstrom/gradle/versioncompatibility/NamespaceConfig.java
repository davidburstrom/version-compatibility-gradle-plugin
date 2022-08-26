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
