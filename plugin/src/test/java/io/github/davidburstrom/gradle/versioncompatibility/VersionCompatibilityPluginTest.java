/*
 * Copyright 2022 David Burström
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.davidburstrom.gradle.versioncompatibility;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.truth.Correspondence;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.function.Function;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.FileCollectionDependency;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.jvm.tasks.Jar;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

class VersionCompatibilityPluginTest {
  @Test
  void pluginRegistersExtension() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    assertThat(project.getExtensions().getByType(VersionCompatibilityExtension.class)).isNotNull();
  }

  @Test
  void extensionRegistersAdapterSourceSets() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.adapters(ac -> ac.getNamespaces().register("", ns -> ns.getVersions().add("1.0")));

    final SourceSetContainer sourceSetContainer =
        project.getExtensions().getByType(SourceSetContainer.class);
    assertThat(sourceSetContainer.getByName("compatApi")).isNotNull();
    assertThat(sourceSetContainer.getByName("compat1Dot0")).isNotNull();
    assertThat(sourceSetContainer.getByName("testCompat1Dot0")).isNotNull();
  }

  @Test
  void extensionRegistersNamespacedSourceSets() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.adapters(
        ac -> ac.getNamespaces().register("dummy", ns -> ns.getVersions().add("1.0")));

    final SourceSetContainer sourceSetContainer =
        project.getExtensions().getByType(SourceSetContainer.class);
    assertThat(sourceSetContainer.getByName("compatDummyApi")).isNotNull();
    assertThat(sourceSetContainer.getByName("compatDummy1Dot0")).isNotNull();
    assertThat(sourceSetContainer.getByName("testCompatDummy1Dot0")).isNotNull();
  }

  @Test
  void canRegisterMultipleAdaptersWithUniqueNamespaces() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.adapters(
        c -> {
          c.getNamespaces().register("namespace1", ns -> ns.getVersions().add("1.0"));
          c.getNamespaces().register("namespace2", ns -> ns.getVersions().add("1.0"));
        });

    final SourceSetContainer sourceSetContainer =
        project.getExtensions().getByType(SourceSetContainer.class);
    assertThat(sourceSetContainer.getByName("compatNamespace1Api")).isNotNull();
    assertThat(sourceSetContainer.getByName("compatNamespace2Api")).isNotNull();
  }

  @Test
  void cannotCallAdaptersWithoutSpecifyingVersions() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    final IllegalArgumentException dummy =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              extension.adapters(c -> c.getNamespaces().register("dummy"));
            });
    assertThat(dummy.getMessage()).isEqualTo("No versions specified for dummy");
  }

  @Test
  void cannotRegisterMultipleAdaptersWithSameNamespace() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.adapters(
        ac -> ac.getNamespaces().register("dummy", ns -> ns.getVersions().add("1.0")));
    assertThrows(
        InvalidUserDataException.class,
        () ->
            extension.adapters(
                ac -> ac.getNamespaces().register("dummy", ns -> ns.getVersions().add("1.0"))));
  }

  @Test
  void versionImplementationDependsOnApiSourceSetOutput() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.adapters(ac -> ac.getNamespaces().register("", ns -> ns.getVersions().add("1.0")));

    final DependencySet compat1Dot0Implementation =
        project.getConfigurations().getByName("compat1Dot0Implementation").getDependencies();
    assertThat(compat1Dot0Implementation)
        .comparingElementsUsing(
            Correspondence.from(
                (Dependency d, Class<?> c) ->
                    c.isAssignableFrom(((FileCollectionDependency) d).getFiles().getClass()),
                "is subtype of"))
        .contains(SourceSetOutput.class);
  }

  @Test
  void mainImplementationDependsOnVersionSourceSetOutput() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.adapters(ac -> ac.getNamespaces().register("", ns -> ns.getVersions().add("1.0")));

    final DependencySet implementation =
        project.getConfigurations().getByName("implementation").getDependencies();
    assertThat(implementation)
        .comparingElementsUsing(
            Correspondence.from(
                (Dependency d, Class<?> c) ->
                    c.isAssignableFrom(((FileCollectionDependency) d).getFiles().getClass()),
                "is subtype of"))
        .contains(SourceSetOutput.class);
  }

  @Test
  void commonConfigurationsAreDefined() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java-library");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.adapters(ac -> ac.getNamespaces().register("", ns -> ns.getVersions().add("1.0")));

    assertThat(project.getConfigurations().findByName("commonImplementation")).isNotNull();
    assertThat(project.getConfigurations().findByName("commonCompileOnly")).isNotNull();
    assertThat(project.getConfigurations().findByName("testCommonImplementation")).isNotNull();
    assertThat(project.getConfigurations().findByName("testCommonRuntimeOnly")).isNotNull();
  }

  @Test
  void commonConfigurationsExtendsFromApiIfAvailable() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java-library");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.adapters(ac -> ac.getNamespaces().register("", ns -> ns.getVersions().add("1.0")));

    final Set<Configuration> extendsFrom =
        project.getConfigurations().getByName("commonImplementation").getExtendsFrom();
    assertThat(extendsFrom)
        .comparingElementsUsing(
            Correspondence.transforming(Configuration::getName, "has a name of"))
        .containsExactly(JavaPlugin.API_CONFIGURATION_NAME);
  }

  @Test
  void customSourceSetHasCompatAdaptersOnImplementationClasspath() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java-library");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final SourceSetContainer sourceSetContainer =
        project.getExtensions().getByType(SourceSetContainer.class);
    sourceSetContainer.create("custom");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.adapters(
        ac ->
            ac.getNamespaces()
                .register(
                    "",
                    ns -> {
                      ns.getVersions().add("1.0");
                      ns.getTargetSourceSetName().set("custom");
                    }));

    final DependencySet implementation =
        project.getConfigurations().getByName("customImplementation").getDependencies();
    assertThat(implementation)
        .comparingElementsUsing(
            Correspondence.from(
                (Dependency d, Class<?> c) ->
                    c.isAssignableFrom(((FileCollectionDependency) d).getFiles().getClass()),
                "is subtype of"))
        .contains(SourceSetOutput.class);
  }

  @Test
  void versionSourceSetsExtendsFromCommon() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java-library");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.adapters(ac -> ac.getNamespaces().register("", ns -> ns.getVersions().add("1.0")));

    final Set<Configuration> extendsFrom =
        project.getConfigurations().getByName("compat1Dot0CompileOnly").getExtendsFrom();
    assertThat(extendsFrom)
        .comparingElementsUsing(
            Correspondence.transforming(Configuration::getName, "has a name of"))
        .contains("commonCompileOnly");
  }

  @Test
  void versionSourceSetsExtendsFromCompileAndTestOnlyConfiguration() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java-library");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.adapters(ac -> ac.getNamespaces().register("", ns -> ns.getVersions().add("1.0")));

    final Set<Configuration> extendsFrom =
        project.getConfigurations().getByName("compat1Dot0CompileOnly").getExtendsFrom();
    assertThat(extendsFrom)
        .comparingElementsUsing(
            Correspondence.transforming(Configuration::getName, "has a name of"))
        .contains("compat1Dot0CompileAndTestOnly");
  }

  @Test
  void versionTestSourceSetsExtendsFromCompileAndTestOnlyConfiguration() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java-library");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.adapters(ac -> ac.getNamespaces().register("", ns -> ns.getVersions().add("1.0")));

    final Set<Configuration> extendsFrom =
        project.getConfigurations().getByName("testCompat1Dot0Implementation").getExtendsFrom();
    assertThat(extendsFrom)
        .comparingElementsUsing(
            Correspondence.transforming(Configuration::getName, "has a name of"))
        .contains("compat1Dot0CompileAndTestOnly");
  }

  @Test
  void compatibilityTestTaskIsCreated() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java-library");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.tests(
        compatibilityTestConfig ->
            compatibilityTestConfig
                .getDimensions()
                .register("dim", dc -> dc.getVersions().add("1.0")));
    final org.gradle.api.tasks.testing.Test compatibilityTestWithDim1Dot0Task =
        (org.gradle.api.tasks.testing.Test)
            project.getTasks().findByName("testCompatibilityWithDim1Dot0");
    assertThat(compatibilityTestWithDim1Dot0Task).isNotNull();
    assertThat(compatibilityTestWithDim1Dot0Task.getGroup()).isEqualTo("verification");
    assertThat(compatibilityTestWithDim1Dot0Task.getDescription())
        .isEqualTo("Runs compatibility test with dim 1.0.");

    // Uses the project classes from the test source set
    project
        .getConfigurations()
        .getByName("testCompatibilityWithDim1Dot0RuntimeOnly")
        .getDependencies()
        .add(project.getDependencies().create(project.files("dummy")));
    final SourceSetContainer sourceSetContainer =
        project.getExtensions().getByType(SourceSetContainer.class);
    assertThat(compatibilityTestWithDim1Dot0Task.getTestClassesDirs())
        .isEqualTo(sourceSetContainer.getByName("test").getOutput().getClassesDirs());
    assertThat(compatibilityTestWithDim1Dot0Task.getClasspath().getFiles()).hasSize(5);
  }

  @Test
  void compatibilityTestTaskIsCreatedWithDimensionOrderBasedOnAdditionOrder() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java-library");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.tests(
        compatibilityTestConfig -> {
          compatibilityTestConfig
              .getDimensions()
              .register("dimA", dc -> dc.getVersions().add("1.0"));
          compatibilityTestConfig
              .getDimensions()
              .register("dimB", dc -> dc.getVersions().add("1.0"));
        });
    assertThat(project.getTasks().findByName("testCompatibilityWithDimA1Dot0AndDimB1Dot0"))
        .isNotNull();

    extension.tests(
        compatibilityTestConfig -> {
          compatibilityTestConfig
              .getDimensions()
              .register("dim2B", dc -> dc.getVersions().add("1.0"));
          compatibilityTestConfig
              .getDimensions()
              .register("dim2A", dc -> dc.getVersions().add("1.0"));
        });
    assertThat(project.getTasks().findByName("testCompatibilityWithDim2B1Dot0AndDim2A1Dot0"))
        .isNotNull();
  }

  @Test
  void configurationsHaveCorrectResolutionSettings() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java-library");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.tests(
        compatibilityTestConfig ->
            compatibilityTestConfig
                .getDimensions()
                .register("dim", dc -> dc.getVersions().add("1.0")));
    assertThat(
            project
                .getConfigurations()
                .getByName("testCompatibilityWithDim1Dot0RuntimeOnly")
                .isCanBeResolved())
        .isFalse();
    assertThat(
            project
                .getConfigurations()
                .getByName("testCompatibilityWithDim1Dot0Classpath")
                .isCanBeResolved())
        .isTrue();
  }

  @Test
  void configuresTestDependencies() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java-library");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.tests(
        compatibilityTestConfig -> {
          compatibilityTestConfig
              .getDimensions()
              .register("dim", dc -> dc.getVersions().add("1.0"));
          compatibilityTestConfig.eachTestRuntimeOnly(
              c -> c.addConstraint("a:b:" + c.getVersions().get(0)));
        });
    assertThat(
            project
                .getConfigurations()
                .getByName("testCompatibilityWithDim1Dot0RuntimeOnly")
                .getDependencyConstraints())
        .containsExactly(project.getDependencies().getConstraints().create("a:b:1.0"));
  }

  @Test
  void configuresTestTask() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java-library");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.tests(
        compatibilityTestConfig -> {
          compatibilityTestConfig
              .getDimensions()
              .register("dim", dc -> dc.getVersions().add("1.0"));
          compatibilityTestConfig.eachTestTask(
              tc ->
                  tc.getTestTask()
                      .setDescription("custom description for " + tc.getVersions().get(0)));
          compatibilityTestConfig.eachTestRuntimeOnly(
              c -> c.addConstraint("a:b:" + c.getVersions().get(0)));
        });
    assertThat(project.getTasks().getByName("testCompatibilityWithDim1Dot0").getDescription())
        .isEqualTo("custom description for 1.0");
  }

  @Test
  void compatibilityTestTaskIsCreatedWithCustomTestSourceSet() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java-library");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final SourceSetContainer sourceSetContainer =
        project.getExtensions().getByType(SourceSetContainer.class);
    sourceSetContainer.create("functionalTest");
    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.tests(
        compatibilityTestConfig -> {
          compatibilityTestConfig
              .getDimensions()
              .register("dim", dc -> dc.getVersions().add("1.0"));
          compatibilityTestConfig.getTestSourceSetName().set("functionalTest");
        });
    final Task functionalCompatibilityTestWithDim1Dot0Task =
        project.getTasks().findByName("functionalTestCompatibilityWithDim1Dot0");
    assertThat(functionalCompatibilityTestWithDim1Dot0Task).isNotNull();
    assertThat(functionalCompatibilityTestWithDim1Dot0Task.getGroup()).isEqualTo("verification");
    assertThat(functionalCompatibilityTestWithDim1Dot0Task.getDescription())
        .isEqualTo("Runs compatibility functionalTest with dim 1.0.");

    // Uses the classes from the functionalTest source set
    assertThat(
            ((org.gradle.api.tasks.testing.Test) functionalCompatibilityTestWithDim1Dot0Task)
                .getTestClassesDirs())
        .isEqualTo(sourceSetContainer.getByName("functionalTest").getOutput().getClassesDirs());
  }

  @Test
  void createsLifecycleTask() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java-library");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.tests(
        compatibilityTestConfig ->
            compatibilityTestConfig
                .getDimensions()
                .register("dim", dc -> dc.getVersions().add("1.0")));
    final Task lifecycleCompatibilityTest = project.getTasks().findByName("testCompatibility");
    assertThat(lifecycleCompatibilityTest).isNotNull();
    assertThat(lifecycleCompatibilityTest.getGroup()).isEqualTo("verification");
    assertThat(lifecycleCompatibilityTest.getDescription())
        .isEqualTo("Runs all compatibility tests.");
    assertThat(lifecycleCompatibilityTest.getDependsOn()).isNotEmpty();
  }

  @Test
  void addsCompatOutputsToJarTask() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java-library");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.adapters(ac -> ac.getNamespaces().register("", nc -> nc.getVersions().add("1.0")));

    final SourceSetContainer sourceSetContainer =
        project.getExtensions().getByType(SourceSetContainer.class);

    Function<String, File> dummyClassCreator =
        (sourceSetName) -> {
          final File outputDir =
              sourceSetContainer
                  .getByName(sourceSetName)
                  .getOutput()
                  .getClassesDirs()
                  .getSingleFile();
          assertTrue(outputDir.mkdirs());
          final File dummyClass = new File(outputDir, "dummy.class");
          try {
            assertTrue(dummyClass.createNewFile());
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          return dummyClass;
        };

    final File compatApiDummyClass = dummyClassCreator.apply("compatApi");
    final File compat1Dot0DummyClass = dummyClassCreator.apply("compat1Dot0");

    final Jar jarTask = (Jar) project.getTasks().findByName(JavaPlugin.JAR_TASK_NAME);
    assertThat(jarTask).isNotNull();
    assertThat(jarTask.getSource()).containsAtLeast(compatApiDummyClass, compat1Dot0DummyClass);
  }

  @Test
  void addsCompatOutputsToTargetClasspath() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java-library");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.adapters(ac -> ac.getNamespaces().register("", nc -> nc.getVersions().add("1.0")));

    final SourceSetContainer sourceSetContainer =
        project.getExtensions().getByType(SourceSetContainer.class);

    final File compatApiDummyClass = getOutputFolder(sourceSetContainer, "compatApi");
    final File compat1Dot0DummyClass = getOutputFolder(sourceSetContainer, "compat1Dot0");

    final Configuration mainImplementationClasspath =
        project
            .getConfigurations()
            .getByName(
                sourceSetContainer
                    .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                    .getCompileClasspathConfigurationName());

    assertThat(mainImplementationClasspath).isNotNull();
    assertThat(mainImplementationClasspath.getFiles())
        .containsAtLeast(compatApiDummyClass, compat1Dot0DummyClass);
  }

  @Test
  void targetSourceSetExtendsFromCommonConfigurations() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java-library");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.adapters(ac -> ac.getNamespaces().register("", nc -> nc.getVersions().add("1.0")));

    final SourceSetContainer sourceSetContainer =
        project.getExtensions().getByType(SourceSetContainer.class);
    final ConfigurationContainer configurationContainer = project.getConfigurations();

    assertThat(
            configurationContainer
                .getByName(
                    sourceSetContainer
                        .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                        .getImplementationConfigurationName())
                .getExtendsFrom())
        .contains(configurationContainer.getByName("commonImplementation"));
    assertThat(
            configurationContainer
                .getByName(
                    sourceSetContainer
                        .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                        .getCompileOnlyConfigurationName())
                .getExtendsFrom())
        .contains(configurationContainer.getByName("commonCompileOnly"));
  }

  @Test
  void testSourceSetExtendsFromCommonConfigurations() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java-library");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.adapters(ac -> ac.getNamespaces().register("", nc -> nc.getVersions().add("1.0")));

    final SourceSetContainer sourceSetContainer =
        project.getExtensions().getByType(SourceSetContainer.class);
    final ConfigurationContainer configurationContainer = project.getConfigurations();

    assertThat(
            configurationContainer
                .getByName(
                    sourceSetContainer.getByName("test").getImplementationConfigurationName())
                .getExtendsFrom())
        .contains(configurationContainer.getByName("testCommonImplementation"));
    assertThat(
            configurationContainer
                .getByName(sourceSetContainer.getByName("test").getRuntimeOnlyConfigurationName())
                .getExtendsFrom())
        .contains(configurationContainer.getByName("testCommonRuntimeOnly"));
  }

  @Test
  void testSourceSetImplementationContainsCorrespondingProductionSourceSetOutputClasses() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java-library");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.adapters(ac -> ac.getNamespaces().register("", nc -> nc.getVersions().add("1.0")));

    final SourceSetContainer sourceSetContainer =
        project.getExtensions().getByType(SourceSetContainer.class);
    final ConfigurationContainer configurationContainer = project.getConfigurations();

    final File compat1Dot0 = getOutputFolder(sourceSetContainer, "compat1Dot0");
    final Set<File> testCompat1Dot0 =
        configurationContainer
            .getByName(
                sourceSetContainer
                    .getByName("testCompat1Dot0")
                    .getCompileClasspathConfigurationName())
            .getFiles();
    assertThat(testCompat1Dot0).contains(compat1Dot0);
  }

  @Test
  void testSourceSetImplementationExtendsFromTheCorrespondingProductionImplementation() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java-library");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.adapters(ac -> ac.getNamespaces().register("", nc -> nc.getVersions().add("1.0")));

    final ConfigurationContainer configurationContainer = project.getConfigurations();

    assertThat(configurationContainer.getByName("testCompat1Dot0Implementation").getExtendsFrom())
        .contains(configurationContainer.getByName("compat1Dot0Implementation"));
  }

  @Test
  void testSourceSetImplementationExtendsFromCommonTestConfigurations() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java-library");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.adapters(ac -> ac.getNamespaces().register("", nc -> nc.getVersions().add("1.0")));

    final ConfigurationContainer configurationContainer = project.getConfigurations();

    assertThat(configurationContainer.getByName("testCompat1Dot0RuntimeOnly").getExtendsFrom())
        .contains(configurationContainer.getByName("testCommonRuntimeOnly"));
    assertThat(configurationContainer.getByName("testCompat1Dot0Implementation").getExtendsFrom())
        .contains(configurationContainer.getByName("testCommonImplementation"));
  }

  @Test
  void adapterTestTasksAreCreatedAndWiredToClasspaths() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java-library");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.adapters(ac -> ac.getNamespaces().register("", nc -> nc.getVersions().add("1.0")));

    final Task testTask = project.getTasks().findByName("testCompat1Dot0");
    assertThat(testTask).isNotNull();
    assertThat(testTask.getGroup()).isEqualTo("verification");
    assertThat(testTask.getDescription())
        .isEqualTo("Runs the test suite for the compat1Dot0 adapter.");

    final SourceSetContainer sourceSetContainer =
        project.getExtensions().getByType(SourceSetContainer.class);
    assertThat(((org.gradle.api.tasks.testing.Test) testTask).getTestClassesDirs())
        .isEqualTo(sourceSetContainer.getByName("testCompat1Dot0").getOutput().getClassesDirs());
    assertThat(((org.gradle.api.tasks.testing.Test) testTask).getClasspath())
        .isEqualTo(sourceSetContainer.getByName("testCompat1Dot0").getRuntimeClasspath());
  }

  @Test
  void compatApiSourceSetExtendsFromCommonConfigurations() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java-library");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.adapters(ac -> ac.getNamespaces().register("", nc -> nc.getVersions().add("1.0")));

    final SourceSetContainer sourceSetContainer =
        project.getExtensions().getByType(SourceSetContainer.class);
    final ConfigurationContainer configurationContainer = project.getConfigurations();

    assertThat(
            configurationContainer
                .getByName(
                    sourceSetContainer.getByName("compatApi").getImplementationConfigurationName())
                .getExtendsFrom())
        .contains(configurationContainer.getByName("commonImplementation"));
    assertThat(
            configurationContainer
                .getByName(
                    sourceSetContainer.getByName("compatApi").getCompileOnlyConfigurationName())
                .getExtendsFrom())
        .contains(configurationContainer.getByName("commonCompileOnly"));
  }

  @Test
  void createsCompatibilityAdapterTestsLifecycleTask() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java-library");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.adapters(ac -> ac.getNamespaces().register("", nc -> nc.getVersions().add("1.0")));
    final Task lifecycleAdapterCompatibilityTest =
        project.getTasks().findByName("testCompatibilityAdapters");
    assertThat(lifecycleAdapterCompatibilityTest).isNotNull();
    assertThat(lifecycleAdapterCompatibilityTest.getGroup()).isEqualTo("verification");
    assertThat(lifecycleAdapterCompatibilityTest.getDescription())
        .isEqualTo("Runs all compatibility adapter tests.");
    assertThat(lifecycleAdapterCompatibilityTest.getDependsOn()).isNotEmpty();
  }

  @Test
  void reusesPreviousCompatibilityAdapterTestsLifecycleTask() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java-library");
    project.getPlugins().apply("io.github.davidburstrom.version-compatibility");

    final VersionCompatibilityExtension extension =
        project.getExtensions().getByType(VersionCompatibilityExtension.class);
    extension.adapters(ac -> ac.getNamespaces().register("A", nc -> nc.getVersions().add("1.0")));
    assertDoesNotThrow(
        () -> {
          extension.adapters(
              ac -> ac.getNamespaces().register("B", nc -> nc.getVersions().add("1.0")));
        });
  }

  private static File getOutputFolder(
      final SourceSetContainer sourceSetContainer, String sourceSetName) {
    return sourceSetContainer.getByName(sourceSetName).getOutput().getClassesDirs().getSingleFile();
  }
}
