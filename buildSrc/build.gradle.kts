/*
 * Enables the project to dogfood the plugin by building the sources and making them available
 * on the build script classpath.
 */

plugins {
    `java-gradle-plugin`
}

gradlePlugin {
    plugins.register("plugin") {
        id = "io.github.davidburstrom.version-compatibility"
        implementationClass = "io.github.davidburstrom.gradle.versioncompatibility.VersionCompatibilityPlugin"
    }
}

val syncSources by tasks.registering(Sync::class) {
    from(File(project.projectDir, "../plugin/src/main/java"))
    into(File(project.buildDir, "generated/plugin-src"))
}

sourceSets["main"].java.setSrcDirs(listOf(syncSources))
