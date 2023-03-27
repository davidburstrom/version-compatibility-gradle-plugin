plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.4.0")
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
}

rootProject.name = "version-compatibility-gradle-plugin"

include(":plugin")
include(":example")
