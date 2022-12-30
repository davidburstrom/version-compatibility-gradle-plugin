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
package io.github.davidburstrom.gradle.versioncompatibility.internal;

import io.github.davidburstrom.gradle.versioncompatibility.AdaptersConfig;
import io.github.davidburstrom.gradle.versioncompatibility.TestTaskConfig;
import io.github.davidburstrom.gradle.versioncompatibility.TestsConfig;
import io.github.davidburstrom.gradle.versioncompatibility.VersionCompatibilityExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.testing.Test;

public class VersionCompatibilityExtensionImpl implements VersionCompatibilityExtension {

  private static final String COMPATIBILITY_TEST_TASK_NAME = "testCompatibility";
  private static final String TEST_COMPATIBILITY_ADAPTERS_TASK_NAME = "testCompatibilityAdapters";
  private final Project project;

  private final TaskProvider<Task> compatibilityTestLifecycleTask;
  private final TaskProvider<Task> compatibilityAdapterTestLifecycleTask;

  public VersionCompatibilityExtensionImpl(@Nonnull Project project) {
    this.project = project;
    compatibilityAdapterTestLifecycleTask = registerCompatibilityAdapterTestLifecycleTask();
    compatibilityTestLifecycleTask = registerCompatibilityTestLifecycleTask();
  }

  @Override
  public void adapters(@Nonnull final Action<AdaptersConfig> action) {
    final AdaptersConfig adaptersConfig = project.getObjects().newInstance(AdaptersConfig.class);

    action.execute(adaptersConfig);

    adaptersConfig
        .getNamespaces()
        .all(
            namespace -> {
              final SetProperty<String> versions = namespace.getVersions();
              if (versions.get().isEmpty()) {
                throw new IllegalArgumentException(
                    "No versions specified for " + namespace.getName());
              }

              final TaskContainer taskContainer = project.getTasks();
              final ConfigurationContainer configurationContainer = project.getConfigurations();
              final DependencyHandler dependencyHandler = project.getDependencies();
              final SourceSetContainer sourceSetContainer =
                  project.getExtensions().getByType(SourceSetContainer.class);
              final TaskProvider<Jar> jarTask =
                  taskContainer.named(JavaPlugin.JAR_TASK_NAME, Jar.class);

              final String targetSourceSetName =
                  namespace.getTargetSourceSetName().getOrElse(SourceSet.MAIN_SOURCE_SET_NAME);

              final NamedDomainObjectProvider<SourceSet> targetSourceSetProvider =
                  sourceSetContainer.named(targetSourceSetName);

              final String capitalizedNamespace = capitalize(namespace.getName());

              final Configuration apiConfiguration =
                  configurationContainer.findByName(JavaPlugin.API_CONFIGURATION_NAME);
              final Configuration commonCompileOnly =
                  createIfNecessary(configurationContainer, "commonCompileOnly", apiConfiguration);
              final Configuration commonImplementation =
                  createIfNecessary(
                      configurationContainer, "commonImplementation", apiConfiguration);
              final Configuration testCommonRuntimeOnly =
                  createIfNecessary(configurationContainer, "testCommonRuntimeOnly", null);
              final Configuration testCommonImplementation =
                  createIfNecessary(configurationContainer, "testCommonImplementation", null);

              extendSourceSetFromCommonConfigurations(
                  configurationContainer,
                  targetSourceSetProvider,
                  commonCompileOnly,
                  commonImplementation);

              final NamedDomainObjectProvider<SourceSet> sourceSetProvider =
                  sourceSetContainer.named(SourceSet.TEST_SOURCE_SET_NAME);
              sourceSetProvider.configure(
                  sourceSet -> {
                    configurationContainer
                        .getByName(sourceSet.getRuntimeOnlyConfigurationName())
                        .extendsFrom(testCommonRuntimeOnly);
                    configurationContainer
                        .getByName(sourceSet.getImplementationConfigurationName())
                        .extendsFrom(testCommonImplementation);
                  });

              String compatApiSourceSetName = "compat" + capitalizedNamespace + "Api";
              final NamedDomainObjectProvider<SourceSet> compatApiSourceSetProvider =
                  sourceSetContainer.register(compatApiSourceSetName);

              extendSourceSetFromCommonConfigurations(
                  configurationContainer,
                  compatApiSourceSetProvider,
                  commonCompileOnly,
                  commonImplementation);

              addOutputToImplementationConfiguration(
                  dependencyHandler, compatApiSourceSetProvider, targetSourceSetProvider);

              addOutputToJarTask(jarTask, compatApiSourceSetProvider);

              versions
                  .get()
                  .forEach(
                      version -> {
                        String unpunctuatedVersion = unpunctuate(version);
                        String compatProductionSourceSetName =
                            "compat" + capitalizedNamespace + unpunctuatedVersion;
                        String compatTestSourceSetName =
                            "testCompat" + capitalizedNamespace + unpunctuatedVersion;

                        final NamedDomainObjectProvider<SourceSet>
                            compatProductionSourceSetProvider =
                                sourceSetContainer.register(compatProductionSourceSetName);
                        final NamedDomainObjectProvider<SourceSet> compatTestSourceSetProvider =
                            sourceSetContainer.register(compatTestSourceSetName);

                        addOutputToImplementationConfiguration(
                            dependencyHandler,
                            compatApiSourceSetProvider,
                            compatProductionSourceSetProvider);

                        extendSourceSetFromCommonConfigurations(
                            configurationContainer,
                            compatProductionSourceSetProvider,
                            commonCompileOnly,
                            commonImplementation);

                        addOutputToImplementationConfiguration(
                            dependencyHandler,
                            compatProductionSourceSetProvider,
                            targetSourceSetProvider);

                        addOutputToImplementationConfiguration(
                            dependencyHandler,
                            compatProductionSourceSetProvider,
                            compatTestSourceSetProvider);

                        compatTestSourceSetProvider.configure(
                            sourceSet -> {
                              configurationContainer
                                  .getByName(sourceSet.getImplementationConfigurationName())
                                  .extendsFrom(
                                      configurationContainer.getByName(
                                          compatProductionSourceSetProvider
                                              .get()
                                              .getImplementationConfigurationName()));
                            });

                        compatTestSourceSetProvider.configure(
                            sourceSet -> {
                              configurationContainer
                                  .getByName(sourceSet.getRuntimeOnlyConfigurationName())
                                  .extendsFrom(testCommonRuntimeOnly);
                              configurationContainer
                                  .getByName(sourceSet.getImplementationConfigurationName())
                                  .extendsFrom(testCommonImplementation);
                            });

                        final TaskProvider<Test> specificCompatibilityTest =
                            taskContainer.register(
                                compatTestSourceSetName,
                                Test.class,
                                test -> {
                                  test.setGroup("verification");
                                  test.setDescription(
                                      "Runs the test suite for the "
                                          + compatProductionSourceSetName
                                          + " adapter.");
                                  final SourceSet sourceSet = compatTestSourceSetProvider.get();
                                  test.setTestClassesDirs(sourceSet.getOutput().getClassesDirs());
                                  test.setClasspath(sourceSet.getRuntimeClasspath());
                                });

                        compatibilityAdapterTestLifecycleTask.configure(
                            t -> t.dependsOn(specificCompatibilityTest));

                        addOutputToJarTask(jarTask, compatProductionSourceSetProvider);
                      });
            });
  }

  private static void addOutputToJarTask(
      @Nonnull final TaskProvider<Jar> jarTask,
      @Nonnull final NamedDomainObjectProvider<SourceSet> sourceSetProvider) {
    jarTask.configure(t -> t.from(sourceSetProvider.get().getOutput()));
  }

  private static void addOutputToImplementationConfiguration(
      final DependencyHandler dependencyHandler,
      @Nonnull final NamedDomainObjectProvider<SourceSet> outputSourceSetProvider,
      @Nonnull final NamedDomainObjectProvider<SourceSet> targetSourceSetProvider) {
    dependencyHandler.add(
        targetSourceSetProvider.get().getImplementationConfigurationName(),
        outputSourceSetProvider.get().getOutput());
  }

  private static void extendSourceSetFromCommonConfigurations(
      @Nonnull final ConfigurationContainer configurationContainer,
      @Nonnull final NamedDomainObjectProvider<SourceSet> sourceSetProvider,
      @Nonnull final Configuration commonCompileOnlyConfiguration,
      @Nonnull final Configuration commonImplementationConfiguration) {
    sourceSetProvider.configure(
        sourceSet -> {
          configurationContainer
              .getByName(sourceSet.getCompileOnlyConfigurationName())
              .extendsFrom(commonCompileOnlyConfiguration);
          configurationContainer
              .getByName(sourceSet.getImplementationConfigurationName())
              .extendsFrom(commonImplementationConfiguration);
        });
  }

  private Configuration createIfNecessary(
      @Nonnull final ConfigurationContainer configurationContainer,
      @Nonnull final String configurationName,
      @Nullable final Configuration optionalExtendee) {
    Configuration result;
    if (configurationContainer.findByName(configurationName) == null) {
      result = configurationContainer.create(configurationName);
      if (optionalExtendee != null) {
        result.extendsFrom(optionalExtendee);
      }
    } else {
      result = configurationContainer.getByName(configurationName);
    }
    return result;
  }

  @Override
  public void tests(@Nonnull Action<TestsConfig> action) {
    final TestsConfigImpl testConfigHandler =
        project.getObjects().newInstance(TestsConfigImpl.class);
    testConfigHandler.getTestSourceSetName().convention(SourceSet.TEST_SOURCE_SET_NAME);

    List<String> dimensionNameOrder = new ArrayList<>();
    testConfigHandler
        .getDimensions()
        .whenObjectAdded(dimensionConfig -> dimensionNameOrder.add(dimensionConfig.getName()));
    action.execute(testConfigHandler);

    final ConfigurationContainer configurationContainer = project.getConfigurations();

    final SourceSetContainer sourceSetContainer =
        project.getExtensions().getByType(SourceSetContainer.class);

    final List<List<NamedVersion>> dimensionedNamedVersions =
        dimensionNameOrder.stream()
            .map(name -> testConfigHandler.getDimensions().getByName(name))
            .map(
                d ->
                    d.getVersions().get().stream()
                        .map(v -> new NamedVersion(d.getName(), v))
                        .collect(Collectors.toList()))
            .collect(Collectors.toList());

    final List<List<NamedVersion>> combinedVersions = cartesianProduct(dimensionedNamedVersions);

    for (List<NamedVersion> combinedVersion : combinedVersions) {
      String fullName =
          createFullCompatibilityTestTaskName(
              testConfigHandler.getTestSourceSetName().get(), combinedVersion);

      final Configuration specificCompatibilityTestRuntimeOnlyConfiguration =
          configurationContainer.create(fullName + "RuntimeOnly");
      final Configuration specificCompatibilityTestRuntimeClasspath =
          configurationContainer.create(fullName + "Classpath");
      specificCompatibilityTestRuntimeOnlyConfiguration.setCanBeResolved(false);
      specificCompatibilityTestRuntimeOnlyConfiguration.extendsFrom(
          configurationContainer.getByName(
              sourceSetContainer
                  .getByName(testConfigHandler.getTestSourceSetName().get())
                  .getRuntimeClasspathConfigurationName()));
      specificCompatibilityTestRuntimeClasspath.extendsFrom(
          specificCompatibilityTestRuntimeOnlyConfiguration);

      final List<String> versionList =
          combinedVersion.stream().map(NamedVersion::getVersion).collect(Collectors.toList());

      testConfigHandler
          .getTestRuntimeOnlyAction()
          .forEach(
              a ->
                  a.execute(
                      new TestRuntimeOnlyConfigImpl(
                          versionList,
                          project.getDependencies(),
                          specificCompatibilityTestRuntimeOnlyConfiguration)));

      TaskProvider<Test> specificCompatibilityTest =
          registerSpecificCompatibilityTest(
              sourceSetContainer,
              testConfigHandler.getTestSourceSetName().get(),
              combinedVersion,
              fullName,
              specificCompatibilityTestRuntimeClasspath,
              versionList,
              testConfigHandler.getEachTestTaskAction());

      compatibilityTestLifecycleTask.configure(t -> t.dependsOn(specificCompatibilityTest));
    }
  }

  @Nonnull
  private TaskProvider<Test> registerSpecificCompatibilityTest(
      @Nonnull SourceSetContainer sourceSetContainer,
      @Nonnull final String testSourceSetName,
      @Nonnull List<NamedVersion> combinedVersion,
      @Nonnull String fullName,
      @Nonnull Configuration specificCompatibilityTestRuntimeClasspath,
      @Nonnull List<String> versionList,
      @Nonnull List<Action<TestTaskConfig>> extraTestConfigurationAction) {
    return project
        .getTasks()
        .register(
            fullName,
            Test.class,
            test -> {
              test.setGroup("verification");
              test.setDescription(
                  "Runs compatibility "
                      + testSourceSetName
                      + " with "
                      + createFullCompatibilityTestTaskDescription(combinedVersion)
                      + ".");

              final FileCollection testClassesDirs =
                  sourceSetContainer.getByName(testSourceSetName).getOutput().getClassesDirs();

              /*
               * The "test" runtime classpath cannot be used, as it usually contains some
               * fixed version(s) of the library/libraries.
               */
              final FileCollection mainRuntimeClasspath =
                  sourceSetContainer
                      .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                      .getRuntimeClasspath();

              test.setTestClassesDirs(testClassesDirs);

              final ConfigurableFileCollection testResourcesDir =
                  project.files(
                      sourceSetContainer
                          .getByName(testSourceSetName)
                          .getOutput()
                          .getResourcesDir());
              testResourcesDir.builtBy(
                  sourceSetContainer.getByName(testSourceSetName).getProcessResourcesTaskName());

              test.setClasspath(
                  testClassesDirs
                      .plus(testResourcesDir)
                      .plus(specificCompatibilityTestRuntimeClasspath)
                      .plus(mainRuntimeClasspath));

              extraTestConfigurationAction.forEach(
                  a -> a.execute(new TestTaskConfigImpl(test, versionList)));
            });
  }

  /* From https://stackoverflow.com/a/9496234/643007 */
  @Nonnull
  private static <T> List<List<T>> cartesianProduct(@Nonnull List<List<T>> lists) {
    List<List<T>> resultLists = new ArrayList<>();
    if (lists.isEmpty()) {
      resultLists.add(new ArrayList<>());
      return resultLists;
    } else {
      List<T> firstList = lists.get(0);
      List<List<T>> remainingLists = cartesianProduct(lists.subList(1, lists.size()));
      for (T condition : firstList) {
        for (List<T> remainingList : remainingLists) {
          ArrayList<T> resultList = new ArrayList<>();
          resultList.add(condition);
          resultList.addAll(remainingList);
          resultLists.add(resultList);
        }
      }
    }
    return resultLists;
  }

  @Nonnull
  private static String createFullCompatibilityTestTaskName(
      final String testSourceSetName, @Nonnull List<NamedVersion> namedVersions) {
    return testSourceSetName
        + "CompatibilityWith"
        + namedVersions.stream()
            .map(
                namedVersion ->
                    unpunctuate(capitalize(namedVersion.getName()))
                        + unpunctuate(namedVersion.getVersion()))
            .collect(Collectors.joining("And"));
  }

  @Nonnull
  private static String createFullCompatibilityTestTaskDescription(
      @Nonnull List<NamedVersion> namedVersions) {
    return namedVersions.stream()
        .map(namedVersion -> namedVersion.getName() + " " + namedVersion.getVersion())
        .collect(Collectors.joining(" and "));
  }

  @Nonnull
  private TaskProvider<Task> registerCompatibilityTestLifecycleTask() {
    return project
        .getTasks()
        .register(
            COMPATIBILITY_TEST_TASK_NAME,
            task -> {
              task.setGroup("verification");
              task.setDescription("Runs all compatibility tests.");
            });
  }

  private TaskProvider<Task> registerCompatibilityAdapterTestLifecycleTask() {
    return project
        .getTasks()
        .register(
            TEST_COMPATIBILITY_ADAPTERS_TASK_NAME,
            task -> {
              task.setGroup("verification");
              task.setDescription("Runs all compatibility adapter tests.");
            });
  }

  @Nonnull
  private static String unpunctuate(@Nonnull String string) {
    return string.replace(".", "Dot").replace("-", "Dash");
  }

  @Nonnull
  private static String capitalize(@Nonnull String string) {
    if (!string.isEmpty() && Character.isLowerCase(string.charAt(0))) {
      string = Character.toUpperCase(string.charAt(0)) + string.substring(1);
    }
    return string;
  }
}
