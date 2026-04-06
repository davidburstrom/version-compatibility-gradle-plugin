plugins {
    id("com.gradle.plugin-publish") version "2.1.1"
    id("io.github.davidburstrom.version-compatibility")
    signing
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
    testImplementation("com.google.truth:truth:1.4.5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.3")
    testImplementation(gradleTestKit())
}

gradlePlugin {
    website = "https://github.com/davidburstrom/version-compatibility-gradle-plugin"
    vcsUrl = "https://github.com/davidburstrom/version-compatibility-gradle-plugin"
    plugins.register("plugin") {
        id = "io.github.davidburstrom.version-compatibility"
        implementationClass = "io.github.davidburstrom.gradle.versioncompatibility.VersionCompatibilityPlugin"
        displayName = "Version Compatibility Gradle Plugin"
        description = "Sets up a compatibility test suite against given versions of one or more dependencies, and sets up source sets to create compatibility adapters for different versions of a dependency. This is useful in any context where the runtime dependencies of a program is a matter of configuration, e.g. when integrating a 3rd party tool in a software suite."
        tags = listOf("versions", "compatibility", "dependencies", "testing", "adapter")
    }
}

val functionalTestSourceSet: SourceSet = sourceSets.create("functionalTest")

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
}

gradlePlugin.testSourceSets(functionalTestSourceSet)

versionCompatibility {
    tests {
        dimensions {
            register("Gradle") {
                versions = listOf("7.0", "7.1", "7.2", "7.3", "7.4", "7.5.1", "7.6.3") +
                    listOf("8.0.2", "8.1.1", "8.2.1", "8.3", "8.4", "8.5", "8.6", "8.7", "8.8", "8.9", "8.10.2", "8.11.1", "8.12.1", "8.13", "8.14.4") +
                    listOf("9.0.0", "9.1.0", "9.2.1", "9.3.1", "9.4.1")
                if (GradleVersion.current().version !in versions.get()) {
                    throw GradleException("Could not find ${gradle.gradleVersion} in the compatibility test versions")
                }
            }
            register("Java") {
                versions = listOf("8", "11", "17", "21", "25")
            }
        }
        filter { (gradleVersion, javaVersion) ->
            !((javaVersion == "11" || javaVersion == "8") && gradleVersion.startsWith("9.")) &&
                !(javaVersion == "17" && (gradleVersion == "7.0" || gradleVersion == "7.1")) &&
                !(javaVersion == "21" && (gradleVersion.startsWith("7.") || gradleVersion in listOf("8.0.2", "8.1.1", "8.2.1"))) &&
                !(javaVersion == "25" && (gradleVersion.startsWith("7.") || gradleVersion.startsWith("8.") || gradleVersion == "9.0.0"))
        }
        testSourceSetName = "functionalTest"
        eachTestTask {
            val (gradleVersion, javaVersion) = versions
            testTask.systemProperty("GRADLE_VERSION", gradleVersion)
            testTask.javaLauncher = javaToolchains.launcherFor {
                languageVersion = JavaLanguageVersion.of(javaVersion)
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
