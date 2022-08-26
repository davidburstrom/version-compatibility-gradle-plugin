package io.github.davidburstrom.gradle.versioncompatibility.internal;

import javax.annotation.Nonnull;

public class NamedVersion {
  private final String name;
  private final String version;

  public NamedVersion(@Nonnull String name, @Nonnull String version) {
    this.name = name;
    this.version = version;
  }

  @Nonnull
  public String getName() {
    return name;
  }

  @Nonnull
  public String getVersion() {
    return version;
  }
}
