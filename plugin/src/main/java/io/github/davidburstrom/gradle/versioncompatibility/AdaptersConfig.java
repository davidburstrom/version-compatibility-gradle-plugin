package io.github.davidburstrom.gradle.versioncompatibility;

import org.gradle.api.NamedDomainObjectContainer;

public interface AdaptersConfig {

  /**
   * Gets the container that holds the configuration for the namespaced compatibility adapters.
   *
   * @return the namespace configuration container.
   */
  NamedDomainObjectContainer<NamespaceConfig> getNamespaces();
}
