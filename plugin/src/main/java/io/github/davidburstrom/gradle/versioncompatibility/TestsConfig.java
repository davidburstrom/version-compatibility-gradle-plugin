package io.github.davidburstrom.gradle.versioncompatibility;

import javax.annotation.Nonnull;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.provider.Property;

/** Used for configuring a compatibility test suite. */
public interface TestsConfig {

  /**
   * Holds the named dimensions in the test matrix and their given versions.
   *
   * <p>The names must consist of the characters [a-zA-Z0-9.-].
   *
   * @return the dimension configurations.
   */
  NamedDomainObjectContainer<DimensionConfig> getDimensions();

  /**
   * Gets the property to configure the test source set name that the compatibility test should run
   * against. If not set, it will run against "test".
   *
   * @return the test source set name property.
   */
  Property<String> getTestSourceSetName();

  /**
   * Adds a dependencies configuration block for each compatibility test runtime classpath.
   *
   * @param action The configuration action.
   */
  void eachTestRuntimeOnly(@Nonnull Action<TestRuntimeOnlyConfig> action);

  /**
   * Adds an extra configuration block for each compatibility test task.
   *
   * @param action The configuration action.
   */
  void eachTestTask(@Nonnull Action<TestTaskConfig> action);
}
