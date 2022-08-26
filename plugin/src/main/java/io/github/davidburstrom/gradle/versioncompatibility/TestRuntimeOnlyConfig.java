package io.github.davidburstrom.gradle.versioncompatibility;

import javax.annotation.Nonnull;

/**
 * Used for configuring the dependencies that should be available on the runtime classpath for a
 * given version in the compatibility test suite.
 */
public interface TestRuntimeOnlyConfig extends HasVersionTuple {
  /**
   * Adds a constraint on the dependencies on the test runtime classpath based on the versions
   * provided through {@link #getVersions()}.
   *
   * <p>It's possible to use the same notation as the Gradle dependency block would use. It's
   * usually a good idea to add "!!" on the constraint, e.g. "group:id:version!!" so that any
   * conflicting requirements are discovered. Otherwise, Gradle will pick the latest version, which
   * is most likely not what's intended.
   *
   * @param constraint The constraint notation.
   */
  void addConstraint(@Nonnull Object constraint);
}
