# Version Compatibility Gradle Plugin

## Overview and purpose

The plugin has two related but independent functions:

* Sets up source sets to create compatibility adapters for different versions of a dependency.
* Sets up a compatibility test suite against given versions of one or more dependencies.

This is useful in any context where the runtime dependencies of a program is a matter of
configuration, e.g. when integrating a 3rd party tool in a software suite.

It is compatible with Gradle 7.0 and up, and works with both Java and Kotlin.

There is a fully featured example in the `example` directory, as a complement to the documentation.

### Compatibility adapters

If a project is using a given dependency, but its actual version is provided during runtime, it is
sometimes necessary to write compatibility adapters if the possible versions have binary or functional
incompatibilities.

As an example, some dependency "dep" has three releases 1.0, 2.0 and 3.0, but only 1.0 and 2.0 are binary compatible.
It is recommended that the indicated version for each adapter is the *earliest* version the adapter supports, for consistency.
This means that there's a need for an adapter for version 1.0 and 3.0:

```kotlin
plugins {
    id("io.github.davidburstrom.version-compatibility") version "0.5.0"
}

versionCompatibility {
    adapters {
        namespaces.register("Dep") {
            versions = listOf("1.0", "3.0")
        }
    }
}

dependencies {
    "compatDep1Dot0CompileAndTestOnly"("dep:dep:1.0")
    "compatDep3Dot0CompileAndTestOnly"("dep:dep:3.0")
}
```

The plugin will create the production source sets `compatDepApi`, `compatDep1Dot0` and `compatDep3Dot0`,
as well as the test source sets `testCompatDep1Dot0` and `testCompatDep3Dot0`.

The `compatDepApi` source set should contain the interface through which the production code will call the
compatibility adapters, and the `compatDep1Dot0` and `compatDep3Dot0` source sets should contain the implementations of said adapters.
Those source sets depend on the output from `compatDepApi`, and the `main` source set depends on the output from all of them.
See the graph below for an overview:

![Sourcesets](./docs/images/sourcesets.svg "Sourcesets")

To provide the specific library dependencies, the most convenient option is to add them to the
`compat*CompileAndTestOnly` configurations. These are parents of the `compat*CompileOnly` and `testCompat*TestImplementation`
configurations.

Since all source sets are likely to have some common dependencies, e.g. for SpotBugs annotations, etc., the plugin will
set up `commonImplementation` and `commonCompileOnly` configurations that the corresponding source set configurations depend on.
See the graph below for an overview:

![Configurations](./docs/images/configurations.svg "Configurations")

The production code will have to select the proper adapter based on the runtime version of the dependency, for example
by resolving the version through the classpath, or provided as a configuration parameter.

### Compatibility adapter test suites

The compatibility adapters can be tested individually using the test source sets. The plugin
automatically sets up test tasks, one per adapter version, with a lifecycle task
`testCompatibilityAdapters` that depends on all of them. To wire up the lifecycle task in the build
process, see [Lifecycle tasks](#lifecycle-tasks).

Given the build script example above, test code can be added to the `testCompatDep1Dot0` and
`testCompatDep3Dot0` source sets, that tests each compatibility adapter.
The test tasks will be named `testCompatDep1Dot0` and `testCompatDep3Dot0`.

Since the `compatDep1Dot0CompileAndTestOnly` and `compatDep3Dot0CompileAndTestOnly` configurations
are used, the specific versions of the library are automatically put on the test implementation and
test runtime classpaths. Should it be necessary to add more dependencies to the tests, the regular
`testCompat*Implementation` and `testCompat*RuntimeOnly` configurations can be used as well.

As both the compatibility test suites and the conventional test suite likely need the same
test infrastructure, there are two configurations `testCommonImplementation` and `testCommonRuntimeOnly`
that can be used to reduce duplication. The `testImplementation`/`testRuntimeOnly` and
`testCompat*Implementation`/`testCompat*RuntimeOnly` configurations extend the common ones accordingly.
See the build script and graph below for an overview:

```kotlin
dependencies {
    "testCommonImplementation"("test:api:x.y")
    "testCommonRuntimeOnly"("test:runtime:x.y")
}
```

![Configurations](./docs/images/test-configurations.svg "Test Configurations")

### Compatibility test suites

In order to test that the production code works well with any given version of a dependency, the plugin
extension object is used to define which versions to pull in. In the example below, the plugin
defines three test tasks, `testCompatibilityWithMyDependency1Dot0`, `testCompatibilityWithMyDependency2Dot0`
and `testCompatibilityWithMyDependency3Dot0` respectively.

```kotlin
plugins {
    id("io.github.davidburstrom.version-compatibility") version "0.5.0"
}

versionCompatibility {
    tests {
        dimensions.register("myDependency") {
            versions = listOf("1.0", "2.0", "3.0")
        }
        eachTestRuntimeOnly {
            addConstraint("my.dependency:dependency:${versions[0]}!!")
        }
        eachTestTask {
            // Optional, unless the test should verify which version is resolved.
            testTask.systemProperty("MY_DEPENDENCY_VERSION", versions[0])
        }
    }
}
```

The plugin will also create a lifecycle task called `testCompatibility` which depends on all the compatibility test tasks.
As the tests may take a substantial time to execute depending on the efficiency of the test implementations and the
number of versions, it is not wired up with the Gradle `check` or `build` lifecycle tasks by default.
To wire it up, see [Lifecycle tasks](#lifecycle-tasks).

The compatibility tests can be run just like normal tests in IntelliJ.

In case there are multiple dimensions, e.g. if the test suite should run against different versions
of the JDK and the dependency, just add another `dimensions.register()` call. The plugin will generate
test tasks for each tuple in the Cartesian product of the registered dimensions. See the `example`
project for more details. The tuples can be filtered in case they for some reason don't work,
for example incompatibility between libraries and JDKs. This saves memory and build time.

## <a name="lifecycle-tasks"></a>Lifecycle tasks

In order to execute the lifecycle tasks as part of the overall build process, they can be wired up
like this to the `build` (or `check`) task:

```kotlin
tasks.named("build").configure {
    dependsOn(tasks.named("testCompatibilityAdapters"))
    dependsOn(tasks.named("testCompatibility"))
}
```

## Name conversions

In contexts where `'.'` and `'-'` characters are illegal, they will be replaced with `"Dot"` and `"Dash"` respectively.

## Releases
* 0.5.0
  * Added: Support for filtering version combinations that are dysfunctional
  * Changed: Example uses Gradle toolchains to run JDK compatibility tests
* 0.4.0
  * Added: A "compileAndTestOnly" configuration that helps reduce duplication when writing adapter tests
  * Added: Example for multidimensional compatibility tests
  * Other: Improved and clarified documentation
* 0.3.0
  * Added: Support for Gradle 8.0
  * Fixed: Undeclared task dependency on test resource processing
* 0.2.0
  * Added: Support for adapter test source generation
  * Changed: "compatibilityTest" has been renamed to "testCompatibility"
  * Changed: Lifecycle tasks are always created when the plugin is applied
* 0.1.0 Initial release

## License

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Copyright 2022-2023 David Burström.

## Acknowledgements

* Erik Ghonyan, for improvements to the documentation.
* [@Goooler](https://github.com/Goooler), for helping keeping the project up to date.

## Future improvements

* Support older versions of Gradle: 5.3 and up should be possible, but it requires wiring up the extension properties manually.
* Support for adding bespoke dependencies to a compatibility test classpath.

