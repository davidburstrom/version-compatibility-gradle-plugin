package io.github.davidburstrom.gradle.versioncompatibility.internal;

import io.github.davidburstrom.gradle.versioncompatibility.AdaptersConfig;
import io.github.davidburstrom.gradle.versioncompatibility.TestTaskConfig;
import io.github.davidburstrom.gradle.versioncompatibility.TestsConfig;
import io.github.davidburstrom.gradle.versioncompatibility.VersionCompatibilityExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.testing.Test;

public class VersionCompatibilityExtensionImpl implements VersionCompatibilityExtension {

  private final Project project;

  private boolean hasRegisteredTestLifecycleTask;

  public VersionCompatibilityExtensionImpl(@Nonnull Project project) {
    this.project = project;
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

              final String targetSourceSetName =
                  namespace.getTargetSourceSetName().getOrElse("main");
              final String capitalizedNamespace = capitalize(namespace.getName());

              final List<String> compatVersionSourceSetNames =
                  versions.get().stream()
                      .map(VersionCompatibilityExtensionImpl::unpunctuate)
                      .map(version -> "compat" + capitalizedNamespace + version)
                      .collect(Collectors.toList());

              final SourceSetContainer sourceSetContainer =
                  project.getExtensions().getByType(SourceSetContainer.class);

              final ConfigurationContainer configurations = project.getConfigurations();

              final Configuration commonCompileOnly;
              final Configuration commonImplementation;
              if (configurations.findByName("commonCompileOnly") == null) {
                commonCompileOnly = configurations.create("commonCompileOnly");
                commonImplementation = configurations.create("commonImplementation");
                final Configuration api = configurations.findByName("api");
                if (api != null) {
                  commonCompileOnly.extendsFrom(api);
                  commonImplementation.extendsFrom(api);
                }
              } else {
                commonCompileOnly = configurations.getByName("commonCompileOnly");
                commonImplementation = configurations.getByName("commonImplementation");
              }

              final List<String> allCompatSourceSetNames =
                  new ArrayList<>(compatVersionSourceSetNames);
              final String compatApiSourceSetName = "compat" + capitalizedNamespace + "Api";
              allCompatSourceSetNames.add(compatApiSourceSetName);

              final List<NamedDomainObjectProvider<SourceSet>> allCompatSourceSetProviders =
                  allCompatSourceSetNames.stream()
                      .map(sourceSetContainer::register)
                      .collect(Collectors.toList());
              final List<NamedDomainObjectProvider<SourceSet>> compatVersionSourceSetProviders =
                  compatVersionSourceSetNames.stream()
                      .map(sourceSetContainer::named)
                      .collect(Collectors.toList());

              final NamedDomainObjectProvider<SourceSet> targetSourceSetProvider =
                  sourceSetContainer.named(targetSourceSetName);

              Stream.concat(
                      Stream.of(targetSourceSetProvider), allCompatSourceSetProviders.stream())
                  .map(NamedDomainObjectProvider::get)
                  .forEach(
                      sourceSet -> {
                        configurations
                            .getByName(sourceSet.getCompileOnlyConfigurationName())
                            .extendsFrom(commonCompileOnly);
                        configurations
                            .getByName(sourceSet.getImplementationConfigurationName())
                            .extendsFrom(commonImplementation);
                      });

              compatVersionSourceSetProviders.stream()
                  .map(NamedDomainObjectProvider::get)
                  .forEach(
                      sourceSet ->
                          project
                              .getDependencies()
                              .add(
                                  sourceSet.getImplementationConfigurationName(),
                                  sourceSetContainer
                                      .getByName(compatApiSourceSetName)
                                      .getOutput()));

              allCompatSourceSetProviders.stream()
                  .map(NamedDomainObjectProvider::get)
                  .forEach(
                      sourceSet ->
                          project
                              .getDependencies()
                              .add(
                                  targetSourceSetProvider
                                      .get()
                                      .getImplementationConfigurationName(),
                                  sourceSet.getOutput()));

              project
                  .getTasks()
                  .named(
                      "jar",
                      Jar.class,
                      task -> {
                        final List<SourceSetOutput> sourceSets =
                            allCompatSourceSetProviders.stream()
                                .map(NamedDomainObjectProvider::get)
                                .map(SourceSet::getOutput)
                                .collect(Collectors.toList());
                        task.from(sourceSets);
                      });
            });
  }

  @Override
  public void tests(@Nonnull Action<TestsConfig> action) {
    final TestsConfigImpl testConfigHandler =
        project.getObjects().newInstance(TestsConfigImpl.class);
    testConfigHandler.getTestSourceSetName().convention("test");

    List<String> dimensionNameOrder = new ArrayList<>();
    testConfigHandler
        .getDimensions()
        .whenObjectAdded(dimensionConfig -> dimensionNameOrder.add(dimensionConfig.getName()));
    action.execute(testConfigHandler);

    final ConfigurationContainer configurations = project.getConfigurations();
    final TaskProvider<Task> lifecycleCompatibilityTest = setupLifecycleCompatibilityTest();

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
          createFullName(testConfigHandler.getTestSourceSetName().get(), combinedVersion);

      final Configuration specificCompatibilityTestRuntimeOnlyConfiguration =
          configurations.create(fullName + "RuntimeOnly");
      final Configuration specificCompatibilityTestRuntimeClasspath =
          configurations.create(fullName + "Classpath");
      specificCompatibilityTestRuntimeOnlyConfiguration.setCanBeResolved(false);
      specificCompatibilityTestRuntimeOnlyConfiguration.extendsFrom(
          configurations.getByName(
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

      lifecycleCompatibilityTest.configure(t -> t.dependsOn(specificCompatibilityTest));
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
                      + createFullDescription(combinedVersion)
                      + ".");

              final FileCollection testClassesDirs =
                  sourceSetContainer.getByName(testSourceSetName).getOutput().getClassesDirs();

              /*
               * The "test" runtime classpath cannot be used, as it's been contaminated with the most recent
               * libraries.
               */
              final FileCollection mainRuntimeClasspath =
                  sourceSetContainer.getByName("main").getRuntimeClasspath();

              test.setTestClassesDirs(testClassesDirs);

              final ConfigurableFileCollection testResourcesDir =
                  project.files(
                      sourceSetContainer
                          .getByName(testSourceSetName)
                          .getOutput()
                          .getResourcesDir());

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
  private static String createFullName(
      final String testSourceSetName, @Nonnull List<NamedVersion> namedVersions) {
    return "compatibility"
        + capitalize(testSourceSetName)
        + "With"
        + namedVersions.stream()
            .map(item -> unpunctuate(capitalize(item.getName())) + unpunctuate(item.getVersion()))
            .collect(Collectors.joining("And"));
  }

  @Nonnull
  private static String createFullDescription(@Nonnull List<NamedVersion> namedVersions) {
    return namedVersions.stream()
        .map(item -> item.getName() + " " + item.getVersion())
        .collect(Collectors.joining(" and "));
  }

  @Nonnull
  private TaskProvider<Task> setupLifecycleCompatibilityTest() {
    if (hasRegisteredTestLifecycleTask) {
      return project.getTasks().named("compatibilityTest");
    }
    hasRegisteredTestLifecycleTask = true;
    return project
        .getTasks()
        .register(
            "compatibilityTest",
            task -> {
              task.setGroup("verification");
              task.setDescription("Runs all compatibility tests.");
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
