plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
}

rootProject.name = "version-compatibility-gradle-plugin"

if (!JavaVersion.current().isJava11Compatible) {
    throw GradleException(
        "The project requires JDK 11 or later to build. Current JDK is ${JavaVersion.current()}."
    )
}

include(":plugin")
include(":example")
