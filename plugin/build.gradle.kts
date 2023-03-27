plugins {
    id("com.gradle.plugin-publish") version "1.1.0"
    id("io.github.davidburstrom.version-compatibility")
    signing
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("com.google.truth:truth:1.1.3")
}

gradlePlugin {
    website.set("https://github.com/davidburstrom/version-compatibility-gradle-plugin")
    vcsUrl.set("https://github.com/davidburstrom/version-compatibility-gradle-plugin")
    plugins.register("plugin") {
        id = "io.github.davidburstrom.version-compatibility"
        implementationClass = "io.github.davidburstrom.gradle.versioncompatibility.VersionCompatibilityPlugin"
        displayName = "Version Compatibility Gradle Plugin"
        description = "Sets up a compatibility test suite against given versions of one or more dependencies, and sets up source sets to create compatibility adapters for different versions of a dependency. This is useful in any context where the runtime dependencies of a program is a matter of configuration, e.g. when integrating a 3rd party tool in a software suite."
        tags.set(listOf("versions", "compatibility", "dependencies", "testing", "adapter"))
    }
}

val functionalTestSourceSet: SourceSet = sourceSets.create("functionalTest")

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])

val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
}

gradlePlugin.testSourceSets(functionalTestSourceSet)

versionCompatibility {
    tests {
        dimensions {
            register("Gradle") {
                versions.set(listOf("7.0", "7.1", "7.2", "7.3", "7.4", "7.5.1", "7.6", "8.0.2"))
            }
            register("Java") {
                versions.set(listOf("8", "11", "17"))
            }
        }
        testSourceSetName.set("functionalTest")
        eachTestTask {
            val (gradleVersion, javaVersion) = versions
            testTask.systemProperty("GRADLE_VERSION", gradleVersion)
            testTask.javaLauncher.set(
                javaToolchains.launcherFor {
                    languageVersion.set(JavaLanguageVersion.of(javaVersion))
                }
            )
            if (gradleVersion == "7.0" || gradleVersion == "7.1") {
                testTask.enabled = false
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named("check").configure {
    dependsOn("testCompatibility")
    dependsOn(functionalTest)
}
