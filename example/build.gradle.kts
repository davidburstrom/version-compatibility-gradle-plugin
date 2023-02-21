plugins {
    kotlin("jvm") version "1.8.10"
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

            // Shows that it is possible to add a test matrix, where the test task can be
            // wired up to run against a given release of Java (though not implemented here).
            register("Java") {
                versions.set(listOf("1.8", "11"))
            }
        }

        eachTestRuntimeOnly {
            val (commonsLangVersion, javaVersion) = versions
            addConstraint("org.apache.commons:commons-lang3:$commonsLangVersion!!")
        }

        // Used to test that the correct commons-lang3 version is resolved automatically
        eachTestTask {
            val (commonsLangVersion, javaVersion) = versions
            testTask.systemProperty("COMMONS_LANG_VERSION", commonsLangVersion)
        }
    }
}

dependencies {
    // This is a dependency used by all adapters
    "commonImplementation"("org.checkerframework:checker-qual:3.31.0")

    // Both the regular test and the compatibility adapter tests require this
    "testCommonImplementation"("org.junit.jupiter:junit-jupiter:5.9.1")

    // Each adapter depends on a specific version of commons-lang3, which will be used to
    // compile the production sources and run the tests, but won't leak to the runtime classpath
    "compatLang3Dot0CompileAndTestOnly"("org.apache.commons:commons-lang3:3.0")
    "compatLang3Dot5CompileAndTestOnly"("org.apache.commons:commons-lang3:3.5")
    "compatLang3Dot10CompileAndTestOnly"("org.apache.commons:commons-lang3:3.10")

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
    dependsOn("testCompatibility")
    dependsOn("testCompatibilityAdapters")
}
