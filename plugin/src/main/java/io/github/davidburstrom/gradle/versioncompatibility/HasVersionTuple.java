package io.github.davidburstrom.gradle.versioncompatibility;

import java.util.List;
import javax.annotation.Nonnull;

/** Indicates that the object has an n-dimensional version tuple, for n &gt;= 1. */
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
