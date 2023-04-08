import com.diffplug.gradle.spotless.SpotlessExtension
import info.solidsoft.gradle.pitest.PitestPluginExtension
import info.solidsoft.gradle.pitest.PitestTask

plugins {
    id("com.diffplug.spotless") version "6.18.0" apply false
    id("info.solidsoft.pitest") version "1.9.11" apply false
    // held back because dependencyUpdates is resolved incorrectly in 0.46.0
    id("com.github.ben-manes.versions") version "0.45.0"
    id("net.ltgt.errorprone") version "3.0.1" apply false
}

val errorProneVersion = "2.18.0"
val googleJavaFormatVersion = "1.16.0"
// held back because it seems to be nigh impossible to disable the trailing-comma-on-call-site rule
val ktlintVersion = "0.47.1"
val pitestJUnit5PluginVersion = "1.1.2"
val pitestMainVersion = "1.11.6"
val pmdVersion = "6.55.0"

configurations {
    register("dependencyUpdates")
}

dependencies {
    "dependencyUpdates"("com.google.errorprone:error_prone_core:$errorProneVersion")
    "dependencyUpdates"("com.google.googlejavaformat:google-java-format:$googleJavaFormatVersion")
    "dependencyUpdates"("com.pinterest.ktlint:ktlint-core:$ktlintVersion")
    "dependencyUpdates"("org.pitest:pitest-junit5-plugin:$pitestJUnit5PluginVersion")
    "dependencyUpdates"("org.pitest:pitest:$pitestMainVersion")
    "dependencyUpdates"("net.sourceforge.pmd:pmd-core:$pmdVersion")
}
tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask>().configureEach {
    rejectVersionIf {
        setOf("rc", "alpha", "beta").any { candidate.version.contains(it, ignoreCase = true) }
    }
}

apply(plugin = "com.diffplug.spotless")
configure<SpotlessExtension> {

    kotlin {
        target("**/*.kt")
        ktlint(ktlintVersion)
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        ktlint(ktlintVersion)
    }
}

allprojects {
    afterEvaluate {

        version = "0.6.0-SNAPSHOT"
        group = "io.github.davidburstrom.gradle.version-compatibility"

        apply(plugin = "com.diffplug.spotless")

        configure<SpotlessExtension> {
            if (plugins.hasPlugin(JavaPlugin::class.java)) {
                java {
                    googleJavaFormat(googleJavaFormatVersion)
                    licenseHeaderFile(rootProject.file("config/license-header.txt"))
                }
            }
        }

        project.tasks.withType<SourceTask>().configureEach {
            if (this.name != "spotlessJavaApply") {
                dependsOn("spotlessJavaApply")
            }
        }

        if (plugins.hasPlugin(JavaPlugin::class.java)) {
            tasks.withType(JavaCompile::class).configureEach {
                options.release.set(8)
                options.compilerArgs.add("-Werror")
            }

            apply(plugin = "net.ltgt.errorprone")
            dependencies {
                "errorprone"(
                    "com.google.errorprone:error_prone_core:$errorProneVersion"
                )
            }
            apply(plugin = "pmd")
            configure<PmdExtension> {
                toolVersion = pmdVersion
                isConsoleOutput = true
                /* Disable default rules and provide specific ones. */
                ruleSets = listOf()
                ruleSetFiles(rootProject.files("config/pmd/rulesets.xml"))
            }

            apply(plugin = "info.solidsoft.pitest")
            configure<PitestPluginExtension> {
                pitestVersion.set(pitestMainVersion)
                junit5PluginVersion.set(pitestJUnit5PluginVersion)
                timestampedReports.set(false)
                targetClasses.set(
                    setOf(
                        "example.*",
                        "io.github.davidburstrom.gradle.versioncompatibility.*"
                    )
                )
                threads.set(2)
                failWhenNoMutations.set(true)
                mutators.set(listOf("DEFAULTS"))
                useClasspathFile.set(true)
                mutationThreshold.set(100)
                if (JavaVersion.current() >= JavaVersion.VERSION_17) {
                    jvmArgs.addAll(
                        "--add-opens=java.base/java.lang=ALL-UNNAMED",
                        "--add-opens=java.base/java.util=ALL-UNNAMED"
                    )
                }

                tasks.named("build").configure {
                    dependsOn("pitest")
                }
            }
            tasks.named<PitestTask>("pitest").configure {
                inputs.property("src", file("src/test"))
                onlyIf {
                    (inputs.properties["src"] as File).exists()
                }

                /*
                 * Carry over all system properties defined for test tasks into the Pitest tasks, except for the "junit"
                 * ones, as they can interfere with test stability.
                 */
                systemProperties(tasks.getByName<Test>("test").systemProperties.filterKeys { !it.contains("junit") })

                /*
                 * Include a "pitest" system property to be able to run tests differently if necessary. Use sparingly!
                 */
                systemProperty("pitest", "true")

                // Stabilizes test executions, especially in Docker
                environment.remove("HOME")

                outputs.cacheIf { true }
            }
        }
    }
}

open class VersionVerifier : DefaultTask() {
    @Input
    lateinit var version: String

    @InputFile
    lateinit var readme: File

    @TaskAction
    fun verify() {
        if (!version.endsWith("-SNAPSHOT")) {
            readme.readLines()
                .filter { "version-compatibility" in it && "\"$version\"" !in it }
                .forEach {
                    throw GradleException("Outdated version in README.md, could not find $version in $it")
                }
        }
    }
}

val verifyVersion = tasks.register("verifyVersion", VersionVerifier::class.java) {
    version = project.version.toString()
    readme = project.file("README.md")
}

tasks.named("build").configure {
    dependsOn(verifyVersion)
}
