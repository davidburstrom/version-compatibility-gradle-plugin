plugins {
    kotlin("jvm") version "1.7.22"
    id("io.github.davidburstrom.version-compatibility")
}

val latestVersion = "3.12.0"

versionCompatibility {
    adapters {
        namespaces.create("Lang") {
            versions.set(listOf("3.0", "3.5", "3.10"))
        }
    }

    tests {
        dimensions {
            register("CommonsLang") {
                versions.set(
                    listOf(
                        "3.0", "3.1", "3.2", "3.3", "3.4", "3.5", "3.6", "3.7", "3.8", "3.9",
                        "3.10", "3.11", "3.12.0"
                    )
                )
            }
        }

        eachTestRuntimeOnly {
            addConstraint("org.apache.commons:commons-lang3:${versions[0]}!!")
        }

        // Used to test that the correct commons-lang3 version is resolved automatically
        eachTestTask {
            testTask.systemProperty("COMMONS_LANG_VERSION", versions[0])
        }
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")

    // This is a dependency used by all adapters
    "commonImplementation"("org.checkerframework:checker-qual:3.23.0")

    // Each adapter depends on a specific version of commons-lang3
    "compatLang3Dot0CompileOnly"("org.apache.commons:commons-lang3:3.0")
    "compatLang3Dot5CompileOnly"("org.apache.commons:commons-lang3:3.5")
    "compatLang3Dot10CompileOnly"("org.apache.commons:commons-lang3:3.10")

    // Use latestVersion if no other dependency constraint exists, but nothing less than 3.0.
    testImplementation("org.apache.commons:commons-lang3:[3.0,)!!$latestVersion")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.named<Test>("test").configure {
    systemProperty("COMMONS_LANG_VERSION", latestVersion)
}

tasks.named("check").configure {
    dependsOn("compatibilityTest")
}
