package io.github.davidburstrom.gradle.versioncompatibility;

import io.github.davidburstrom.gradle.versioncompatibility.internal.VersionCompatibilityExtensionImpl;
import javax.annotation.Nonnull;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class VersionCompatibilityPlugin implements Plugin<Project> {

  private static final String EXTENSION_NAME = "versionCompatibility";

  @Override
  public void apply(@Nonnull Project project) {
    project
        .getExtensions()
        .create(
            VersionCompatibilityExtension.class,
            EXTENSION_NAME,
            VersionCompatibilityExtensionImpl.class,
            project);
  }
}
