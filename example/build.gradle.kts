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
    // This is a dependency used by all adapters
    "commonImplementation"("org.checkerframework:checker-qual:3.28.0")

    // Both the regular test and the compatibility adapter tests require this
    "testCommonImplementation"("org.junit.jupiter:junit-jupiter:5.9.1")

    // Each adapter depends on a specific version of commons-lang3 to compile, but
    // it cannot leak to the runtime classpath
    "compatLang3Dot0CompileOnly"("org.apache.commons:commons-lang3:3.0")
    "compatLang3Dot5CompileOnly"("org.apache.commons:commons-lang3:3.5")
    "compatLang3Dot10CompileOnly"("org.apache.commons:commons-lang3:3.10")

    // Each adapter test must have its specific version of commons-lang3
    "testCompatLang3Dot0RuntimeOnly"("org.apache.commons:commons-lang3:3.0")
    "testCompatLang3Dot5RuntimeOnly"("org.apache.commons:commons-lang3:3.5")
    "testCompatLang3Dot10RuntimeOnly"("org.apache.commons:commons-lang3:3.10")

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
    dependsOn("testCompatibilityAdapters")
}
