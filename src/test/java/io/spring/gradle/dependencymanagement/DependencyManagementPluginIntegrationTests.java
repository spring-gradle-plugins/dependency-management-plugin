/*
 * Copyright 2014-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.gradle.dependencymanagement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link DependencyManagementPlugin}.
 *
 * @author Andy Wilkinson
 */
class DependencyManagementPluginIntegrationTests {

	@RegisterExtension
	private final GradleBuild gradleBuild = new GradleBuild();

	@Test
	void importedBomCanBeUsedToApplyDependencyManagement() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-core-4.0.6.RELEASE.jar");
	}

	@Test
	void importedBomsVersionsCanBeOverridden() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-core-4.0.5.RELEASE.jar");
	}

	@Test
	void dependencyManagementCanBeDeclaredInTheBuild() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-core-4.0.4.RELEASE.jar",
				"commons-logging-1.1.2.jar");
	}

	@Test
	void dependencyManagementCanBeDeclaredInTheBuildUsingTheNewSyntax() {
		this.gradleBuild.runner().withArguments("managedVersions", "exclusions").build();
		assertThat(readLines("managed-versions.txt")).containsOnly("alpha:bravo -> 1.0",
				"commons-logging:commons-logging -> 1.1.2", "charlie:delta -> 2.0",
				"org.springframework:spring-core -> 4.0.4.RELEASE");
		assertThat(readLines("exclusions.txt")).containsOnly("commons-logging:commons-logging -> [foo:bar]",
				"charlie:delta -> [bar:baz]");
	}

	@Test
	void dependencyManagementWithExclusionsCanBeDeclaredInTheBuild() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-core-4.0.4.RELEASE.jar");
	}

	@Test
	void versionsOfDirectDependenciesTakePrecedenceOverDirectDependencyManagement() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-core-4.0.6.RELEASE.jar",
				"commons-logging-1.1.3.jar");
	}

	@Test
	void directProjectDependenciesTakePrecedenceOverDependencyManagement() {
		writeLines(Paths.get("settings.gradle"), "include ':child'");
		writeLines(Paths.get("child", "build.gradle"), "group = 'test'", "version = '1.1.0'", "apply plugin: 'java'");
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("child-1.1.0.jar");
	}

	@Test
	void transitiveProjectDependenciesTakePrecedenceOverDependencyManagement() {
		writeLines(Paths.get("settings.gradle"), "include ':child'", "include ':grandchild'");
		writeLines(Paths.get("child", "build.gradle"), "group = 'test'", "version = '1.1.0'", "apply plugin: 'java'",
				"dependencies {", "\timplementation project([path: ':grandchild'])", "}");
		writeLines(Paths.get("grandchild", "build.gradle"), "group = 'test-other'", "version = '1.1.0'",
				"apply plugin: 'java'", "dependencies {", "\timplementation project([path: ':grandchild'])", "}");
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("child-1.1.0.jar", "grandchild-1.1.0.jar");
	}

	@Test
	void versionsOfDirectDependenciesTakePrecedenceOverDependencyManagementInAnImportedBom() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-core-4.0.4.RELEASE.jar");
	}

	@Test
	void dependencyManagementCanBeAppliedToASpecificConfiguration() {
		this.gradleBuild.runner().withArguments("resolveManaged", "resolveUnmanaged").build();
		assertThat(readLines("resolved-managed.txt")).containsOnly("spring-core-4.0.6.RELEASE.jar",
				"commons-logging-1.1.2.jar");
		assertThat(readLines("resolved-unmanaged.txt")).containsOnly("spring-core-4.0.6.RELEASE.jar",
				"commons-logging-1.1.3.jar");
	}

	@Test
	void dependencyManagementCanBeAppliedToMultipleSpecificConfigurations() {
		this.gradleBuild.runner().withArguments("resolveManaged1", "resolveManaged2", "resolveUnmanaged").build();
		assertThat(readLines("resolved-managed1.txt")).containsOnly("spring-core-4.0.6.RELEASE.jar",
				"commons-logging-1.1.2.jar");
		assertThat(readLines("resolved-managed2.txt")).containsOnly("spring-core-4.0.6.RELEASE.jar",
				"commons-logging-1.1.2.jar");
		assertThat(readLines("resolved-unmanaged.txt")).containsOnly("spring-core-4.0.6.RELEASE.jar",
				"commons-logging-1.1.3.jar");
	}

	@Test
	void configurationSpecificDependencyManagementTakesPrecedenceOverGlobalDependencyManagement() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-core-4.0.6.RELEASE.jar",
				"commons-logging-1.1.1.jar");
	}

	@Test
	void configurationSpecificDependencyManagementIsInheritedByExtendingConfigurations() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("commons-logging-1.1.1.jar");
	}

	@Test
	void versionOnADirectDependencyProvidesDependencyManagementToExtendingConfigurations() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("commons-logging-1.1.3.jar");
	}

	@Test // gh-3
	void jbossJavaEEBomCanBeImportedAndUsedForDependencyManagement() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("jboss-el-api_2.2_spec-1.0.0.Final.jar");
	}

	@Test // gh-41
	void bomWithNoDependencyManagementCanBeImportedAndItsPropertiesUsed() {
		this.gradleBuild.runner().withArguments("importedProperties").build();
		assertThat(readLines("imported-properties.txt")).containsOnly("a -> alpha");
	}

	@Test
	void springCloudStarterParentBomCanBeImportedAndUsedForDependencyManagement() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt"))
				.contains("org.springframework.cloud:spring-cloud-starter-eureka-server -> 1.0.0.M3");
	}

	@Test
	void dependencySetCanBeUsedToProvideDependencyManagementForMultipleModulesWithTheSameGroupAndVersion() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("slf4j-simple-1.7.7.jar", "slf4j-api-1.7.7.jar");
	}

	@Test
	void exclusionCanBeDeclaredOnAnEntryInADependencySet() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-core-4.1.4.RELEASE.jar");
	}

	@Test
	void managedVersionsCanBeAccessedProgramatically() {
		this.gradleBuild.runner().withArguments("verify").build();
	}

	@Test
	void propertiesImportedFromABomCanBeAccessed() {
		this.gradleBuild.runner().withArguments("verify").build();
	}

	@Test
	void bomPropertyCanBeUsedToVersionADependency() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).contains("hibernate-envers-4.3.5.Final.jar");
	}

	@Test
	void bomThatReferencesJavaHomeCanBeImported() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).contains("com.sun:tools -> 1.6");
	}

	@Test
	void dependencyVersionsCanBeDefinedUsingProperties() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).contains("org.springframework:spring-beans -> 4.1.1.RELEASE",
				"org.springframework:spring-core -> 4.1.1.RELEASE", "org.springframework:spring-tx -> 4.1.1.RELEASE",
				"org.slf4j:slf4j-api -> 1.7.7");
	}

	@Test
	void importOfABomCanReferenceAProperty() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).contains("org.springframework:spring-core -> 4.0.6.RELEASE");
	}

	@Test
	void importOfABomCanUsePropertyMethodToReferenceAProperty() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).contains("org.springframework:spring-core -> 4.0.6.RELEASE");
	}

	@Test
	void explicitDependencyPreventsTheDependencyFromBeingExcluded() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).contains("spring-core-4.0.6.RELEASE.jar", "commons-logging-1.1.3.jar",
				"hive-common-0.14.0.jar");
	}

	@Test
	void exclusionDeclaredOnTheDependencyThatHasTheExcludedDependencyIsHonored() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("direct-exclude-1.0.jar", "spring-core-4.1.2.RELEASE.jar",
				"spring-tx-4.1.2.RELEASE.jar", "spring-beans-4.1.2.RELEASE.jar");
	}

	@Test
	void dependencyWithAnOtherwiseExcludedTransitiveDependencyOverridesTheExclude() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("direct-exclude-1.0.jar", "spring-core-4.1.2.RELEASE.jar",
				"spring-tx-4.1.2.RELEASE.jar", "commons-logging-1.1.3.jar", "spring-beans-4.1.2.RELEASE.jar");
	}

	@Test
	void exclusionThatAppliesTransitivelyIsHonored() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("transitive-exclude-1.0.jar",
				"spring-beans-4.1.2.RELEASE.jar", "spring-tx-4.1.2.RELEASE.jar", "spring-core-4.1.2.RELEASE.jar");
	}

	@Test
	void directExclusionDeclaredInABomIsHonored() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-tx-4.1.2.RELEASE.jar",
				"spring-beans-4.1.2.RELEASE.jar", "spring-core-4.1.2.RELEASE.jar");
	}

	@Test
	void wildcardExclusionDeclaredInABomIsHonored() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-tx-4.1.2.RELEASE.jar",
				"spring-beans-4.1.2.RELEASE.jar", "spring-core-4.1.2.RELEASE.jar");
	}

	@Test
	void transitiveExclusionDeclaredInABomIsHonored() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-tx-4.1.2.RELEASE.jar",
				"spring-beans-4.1.2.RELEASE.jar", "spring-core-4.1.2.RELEASE.jar");
	}

	@Test // gh-21
	void exclusionsAreNotInheritedAndDoNotAffectDirectDependencies() {
		this.gradleBuild.runner().withArguments("resolveCompile", "resolveTestCompile").build();
		assertThat(readLines("resolved-compile.txt")).containsOnly("direct-exclude-1.0.jar",
				"spring-core-4.1.2.RELEASE.jar", "spring-tx-4.1.2.RELEASE.jar", "spring-beans-4.1.2.RELEASE.jar");
		assertThat(readLines("resolved-test-compile.txt")).containsOnly("direct-exclude-1.0.jar",
				"commons-logging-1.1.3.jar", "spring-core-4.1.2.RELEASE.jar", "spring-tx-4.1.2.RELEASE.jar",
				"spring-beans-4.1.2.RELEASE.jar");
	}

	@Test // gh-21
	void exclusionsAreNotInheritedAndDoNotAffectTransitiveDependencies() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("groovy-2.3.8.jar",
				"spring-boot-starter-test-1.2.0.RELEASE.jar", "junit-4.12.jar", "mockito-core-1.10.8.jar",
				"hamcrest-core-1.3.jar", "hamcrest-library-1.3.jar", "spring-core-4.1.3.RELEASE.jar",
				"spring-test-4.1.3.RELEASE.jar", "objenesis-2.1.jar");
	}

	@Test // gh-23
	void exclusionsFromAncestorsOfADependencyAreAppliedCorrectly() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).contains("ribbon-loadbalancer-2.0-RC13.jar");
	}

	@Test
	void pomExclusionsCanBeDisabled() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-core-4.0.6.RELEASE.jar",
				"commons-logging-1.1.3.jar");
	}

	@Test
	void exclusionsAreAppliedCorrectlyToDependenciesThatAreReferencedMultipleTimes() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).noneMatch((line) -> line.startsWith("groovy-all"));
	}

	@Test // gh-33
	void transitiveDependenciesWithACircularReferenceAreTolerated() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).isNotEmpty();
	}

	@Test
	void managedDependencyCanBeConfiguredUsingAGString() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).containsOnly("org.springframework:spring-core -> 4.1.5.RELEASE");
	}

	@Test
	void exclusionsAreHandledCorrectlyForDependenciesThatAppearMultipleTimes() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).contains("validation-api-1.1.0.Final.jar");
	}

	@Test
	void bomImportOrderIsReflectedInManagedVersions() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).contains("org.springframework:spring-core -> 4.2.3.RELEASE");
	}

	@Test
	void bomImportOrderIsReflectedInManagedVersionsWhenSameBomIsImportedMultipleTimes() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).contains("org.springframework:spring-core -> 4.2.3.RELEASE");
	}

	@Test
	void managedVersionsOfAConfigurationCanBeAccessed() {
		this.gradleBuild.runner().withArguments("verify").build();
	}

	@Test
	void dependencyWithAMissingComponentInItsStringIdentifierProducesAHelpfulError() {
		String output = this.gradleBuild.runner().withArguments("build").buildAndFail().getOutput();
		assertThat(output)
				.contains("Dependency identifier 'a:1.0' is malformed. The required form is 'group:name:version'");
	}

	@Test
	void dependencyWithAMissingComponentInItsMapIdentifierProducesAHelpfulError() {
		String output = this.gradleBuild.runner().withArguments("build").buildAndFail().getOutput();
		assertThat(output).contains("Dependency identifier '{group=a}' did not specify name, version");
	}

	@Test
	void dynamicVersionIsNotAddedToDependencyManagement() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt"))
				.noneMatch((line) -> line.startsWith("commons-logging:commons-logging ->"));
	}

	@Test
	void dependencyManagementIsNotAppliedToADependencyUseLatestIntegration() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).doesNotContain("commons-logging-1.1.3.jar");
	}

	@Test
	void dependencyManagementIsNotAppliedToAnInheritedDependencyUseLatestIntegration() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).doesNotContain("commons-logging-1.1.3.jar");
	}

	@Test
	void dependencyManagementIsAppliedToATransitiveDependencyDeclaredWithARange() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).contains("guava-18.0.jar");
	}

	@Test
	void propertyInABomCanBeOverriddenWhenItIsImported() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).contains("org.springframework:spring-core -> 4.3.0.RELEASE");
	}

	@Test
	void propertyInABomCanBeConfiguredUsingAReferenceToAProjectProperty() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).contains("org.springframework:spring-core -> 4.3.0.RELEASE");
	}

	@Test
	void whenOverridingABomPropertyAPropertyOnAnImportTakesPrecedenceOverAProjectProperty() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).contains("org.springframework:spring-core -> 4.3.0.RELEASE");
	}

	@Test
	void userProvidedResolutionStrategyRunsAfterInternalResolutionStrategy() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).contains("spring-core-4.2.6.RELEASE.jar");
	}

	@Test
	void exclusionsInImportedBomsForUnresolvableDependenciesAreApplied() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("unresolvable-transitive-dependency-1.0.jar");
	}

	@Test
	void unresolvableDependenciesAreIgnoredWhenApplyingMavenStyleExclusions() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-core-4.0.3.RELEASE.jar",
				"unresolvable-transitive-dependency-1.0.jar");
	}

	@Test
	void dependencyManagementBeingOverridenByDependenciesCanBeDisabled() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-core-4.0.3.RELEASE.jar");
	}

	@Test
	void artifactsWithAPomAreTolerated() throws IOException {
		File libDir = new File(this.gradleBuild.runner().getProjectDir(), "lib");
		libDir.mkdirs();
		new File(libDir, "foo-1.0.0.jar").createNewFile();
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("foo-1.0.0.jar");
	}

	@Test
	void configurationCanBeUsedDirectlyWhenConfiguringConfigurationSpecificDependencyManagement() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).containsOnly("org.springframework:spring-core -> 4.0.0.RELEASE");
	}

	@Test
	void configurationCanBeReferredToByNameWhenConfiguringConfigurationSpecificDependencyManagement() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).containsOnly("org.springframework:spring-core -> 4.0.0.RELEASE");
	}

	@Test
	void whenImportingABomDependencyManagementWithAClassifierIsIgnored() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).isEmpty();
	}

	@Test
	void whenImportingABomDependencyManagementWithNoVersionIsIgnored() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).isEmpty();
	}

	@Test
	void exclusionThatIsMalformedIsTolerated() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("malformed-exclude-1.0.jar", "spring-core-4.1.2.RELEASE.jar",
				"commons-logging-1.1.3.jar");
	}

	@Test
	void exclusionsAreAppliedToDependenciesVersionedWithConstraints() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("transitive-exclude-1.0.jar",
				"spring-beans-4.1.2.RELEASE.jar", "spring-tx-4.1.2.RELEASE.jar", "spring-core-4.1.2.RELEASE.jar");
	}

	@Test
	void dependenciesWithExtremelyLargePomsAreHandled() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).contains("hisrc-basicjaxb-plugins-2.1.0.jar");
	}

	@Test
	void constraintsInTransitivePlatformDependenciesDoNotPreventExclusionsFromWorking() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).noneMatch((line) -> line.contains("junit"));
	}

	private void writeLines(Path path, String... lines) {
		try {
			Path resolvedPath = this.gradleBuild.runner().getProjectDir().toPath().resolve(path);
			Files.createDirectories(resolvedPath.getParent());
			Files.write(resolvedPath, Arrays.asList(lines), StandardOpenOption.CREATE_NEW);
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private List<String> readLines(String filename) {
		try {
			return Files.readAllLines(
					this.gradleBuild.runner().getProjectDir().toPath().resolve(Paths.get("build", filename)));
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
