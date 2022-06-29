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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link DependencyManagementPlugin}.
 *
 * @author Andy Wilkinson
 */
public class DependencyManagementPluginIntegrationTests {

	@Rule
	public final GradleBuild gradleBuild = new GradleBuild();

	@Test
	public void importedBomCanBeUsedToApplyDependencyManagement() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-core-4.0.6.RELEASE.jar");
	}

	@Test
	public void importedBomsVersionsCanBeOverridden() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-core-4.0.5.RELEASE.jar");
	}

	@Test
	public void dependencyManagementCanBeDeclaredInTheBuild() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-core-4.0.4.RELEASE.jar",
				"commons-logging-1.1.2.jar");
	}

	@Test
	public void dependencyManagementCanBeDeclaredInTheBuildUsingTheNewSyntax() {
		this.gradleBuild.runner().withArguments("managedVersions", "exclusions").build();
		assertThat(readLines("managed-versions.txt")).containsOnly("alpha:bravo -> 1.0",
				"commons-logging:commons-logging -> 1.1.2", "charlie:delta -> 2.0",
				"org.springframework:spring-core -> 4.0.4.RELEASE");
		assertThat(readLines("exclusions.txt")).containsOnly("commons-logging:commons-logging -> [foo:bar]",
				"charlie:delta -> [bar:baz]");
	}

	@Test
	public void dependencyManagementWithExclusionsCanBeDeclaredInTheBuild() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-core-4.0.4.RELEASE.jar");
	}

	@Test
	public void versionsOfDirectDependenciesTakePrecedenceOverDirectDependencyManagement() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-core-4.0.6.RELEASE.jar",
				"commons-logging-1.1.3.jar");
	}

	@Test
	public void directProjectDependenciesTakePrecedenceOverDependencyManagement() {
		File settings = new File(this.gradleBuild.runner().getProjectDir(), "settings.gradle");
		writeLines(settings, "include ':child'");
		File child = new File(this.gradleBuild.runner().getProjectDir(), "child/build.gradle");
		writeLines(child, "group = 'test'", "version = '1.1.0'", "apply plugin: 'java'");
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("child-1.1.0.jar");
	}

	@Test
	public void transitiveProjectDependenciesTakePrecedenceOverDependencyManagement() {
		File settings = new File(this.gradleBuild.runner().getProjectDir(), "settings.gradle");
		writeLines(settings, "include ':child'", "include ':grandchild'");
		File child = new File(this.gradleBuild.runner().getProjectDir(), "child/build.gradle");
		writeLines(child, "group = 'test'", "version = '1.1.0'", "apply plugin: 'java'", "dependencies {",
				"\tcompile project([path: ':grandchild'])", "}");
		File grandchild = new File(this.gradleBuild.runner().getProjectDir(), "grandchild/build.gradle");
		writeLines(grandchild, "group = 'test-other'", "version = '1.1.0'", "apply plugin: 'java'", "dependencies {",
				"\tcompile project([path: ':grandchild'])", "}");
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("child-1.1.0.jar", "grandchild-1.1.0.jar");
	}

	@Test
	public void versionsOfDirectDependenciesTakePrecedenceOverDependencyManagementInAnImportedBom() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-core-4.0.4.RELEASE.jar");
	}

	@Test
	public void dependencyManagementCanBeAppliedToASpecificConfiguration() {
		this.gradleBuild.runner().withArguments("resolveManaged", "resolveUnmanaged").build();
		assertThat(readLines("resolved-managed.txt")).containsOnly("spring-core-4.0.6.RELEASE.jar",
				"commons-logging-1.1.2.jar");
		assertThat(readLines("resolved-unmanaged.txt")).containsOnly("spring-core-4.0.6.RELEASE.jar",
				"commons-logging-1.1.3.jar");
	}

	@Test
	public void dependencyManagementCanBeAppliedToMultipleSpecificConfigurations() {
		this.gradleBuild.runner().withArguments("resolveManaged1", "resolveManaged2", "resolveUnmanaged").build();
		assertThat(readLines("resolved-managed1.txt")).containsOnly("spring-core-4.0.6.RELEASE.jar",
				"commons-logging-1.1.2.jar");
		assertThat(readLines("resolved-managed2.txt")).containsOnly("spring-core-4.0.6.RELEASE.jar",
				"commons-logging-1.1.2.jar");
		assertThat(readLines("resolved-unmanaged.txt")).containsOnly("spring-core-4.0.6.RELEASE.jar",
				"commons-logging-1.1.3.jar");
	}

	@Test
	public void configurationSpecificDependencyManagementTakesPrecedenceOverGlobalDependencyManagement() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-core-4.0.6.RELEASE.jar",
				"commons-logging-1.1.1.jar");
	}

	@Test
	public void configurationSpecificDependencyManagementIsInheritedByExtendingConfigurations() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("commons-logging-1.1.1.jar");
	}

	@Test
	public void versionOnADirectDependencyProvidesDependencyManagementToExtendingConfigurations() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("commons-logging-1.1.3.jar");
	}

	@Test // gh-3
	public void jbossJavaEEBomCanBeImportedAndUsedForDependencyManagement() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("jboss-el-api_2.2_spec-1.0.0.Final.jar");
	}

	@Test // gh-41
	public void bomWithNoDependencyManagementCanBeImportedAndItsPropertiesUsed() {
		this.gradleBuild.runner().withArguments("importedProperties").build();
		assertThat(readLines("imported-properties.txt")).containsOnly("a -> alpha");
	}

	@Test
	public void springCloudStarterParentBomCanBeImportedAndUsedForDependencyManagement() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt"))
				.contains("org.springframework.cloud:spring-cloud-starter-eureka-server -> 1.0.0.M3");
	}

	@Test
	public void dependencySetCanBeUsedToProvideDependencyManagementForMultipleModulesWithTheSameGroupAndVersion() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("slf4j-simple-1.7.7.jar", "slf4j-api-1.7.7.jar");
	}

	@Test
	public void exclusionCanBeDeclaredOnAnEntryInADependencySet() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-core-4.1.4.RELEASE.jar");
	}

	@Test
	public void managedVersionsCanBeAccessedProgramatically() {
		this.gradleBuild.runner().withArguments("verify").build();
	}

	@Test
	public void propertiesImportedFromABomCanBeAccessed() {
		this.gradleBuild.runner().withArguments("verify").build();
	}

	@Test
	public void bomPropertyCanBeUsedToVersionADependency() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).contains("hibernate-envers-4.3.5.Final.jar");
	}

	@Test
	public void bomThatReferencesJavaHomeCanBeImported() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).contains("com.sun:tools -> 1.6");
	}

	@Test
	public void dependencyVersionsCanBeDefinedUsingProperties() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).contains("org.springframework:spring-beans -> 4.1.1.RELEASE",
				"org.springframework:spring-core -> 4.1.1.RELEASE", "org.springframework:spring-tx -> 4.1.1.RELEASE",
				"org.slf4j:slf4j-api -> 1.7.7");
	}

	@Test
	public void importOfABomCanReferenceAProperty() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).contains("org.springframework:spring-core -> 4.0.6.RELEASE");
	}

	@Test
	public void importOfABomCanUsePropertyMethodToReferenceAProperty() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).contains("org.springframework:spring-core -> 4.0.6.RELEASE");
	}

	@Test
	public void explicitDependencyPreventsTheDependencyFromBeingExcluded() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).contains("spring-core-4.0.6.RELEASE.jar", "commons-logging-1.1.3.jar",
				"hive-common-0.14.0.jar");
	}

	@Test
	public void exclusionDeclaredOnTheDependencyThatHasTheExcludedDependencyIsHonored() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("direct-exclude-1.0.jar", "spring-core-4.1.2.RELEASE.jar",
				"spring-tx-4.1.2.RELEASE.jar", "spring-beans-4.1.2.RELEASE.jar");
	}

	@Test
	public void dependencyWithAnOtherwiseExcludedTransitiveDependencyOverridesTheExclude() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("direct-exclude-1.0.jar", "spring-core-4.1.2.RELEASE.jar",
				"spring-tx-4.1.2.RELEASE.jar", "commons-logging-1.1.3.jar", "spring-beans-4.1.2.RELEASE.jar");
	}

	@Test
	public void exclusionThatAppliesTransitivelyIsHonored() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("transitive-exclude-1.0.jar",
				"spring-beans-4.1.2.RELEASE.jar", "spring-tx-4.1.2.RELEASE.jar", "spring-core-4.1.2.RELEASE.jar");
	}

	@Test
	public void directExclusionDeclaredInABomIsHonored() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-tx-4.1.2.RELEASE.jar",
				"spring-beans-4.1.2.RELEASE.jar", "spring-core-4.1.2.RELEASE.jar");
	}

	@Test
	public void wildcardExclusionDeclaredInABomIsHonored() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-tx-4.1.2.RELEASE.jar",
				"spring-beans-4.1.2.RELEASE.jar", "spring-core-4.1.2.RELEASE.jar");
	}

	@Test
	public void transitiveExclusionDeclaredInABomIsHonored() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-tx-4.1.2.RELEASE.jar",
				"spring-beans-4.1.2.RELEASE.jar", "spring-core-4.1.2.RELEASE.jar");
	}

	@Test // gh-21
	public void exclusionsAreNotInheritedAndDoNotAffectDirectDependencies() {
		this.gradleBuild.runner().withArguments("resolveCompile", "resolveTestCompile").build();
		assertThat(readLines("resolved-compile.txt")).containsOnly("direct-exclude-1.0.jar",
				"spring-core-4.1.2.RELEASE.jar", "spring-tx-4.1.2.RELEASE.jar", "spring-beans-4.1.2.RELEASE.jar");
		assertThat(readLines("resolved-test-compile.txt")).containsOnly("direct-exclude-1.0.jar",
				"commons-logging-1.1.3.jar", "spring-core-4.1.2.RELEASE.jar", "spring-tx-4.1.2.RELEASE.jar",
				"spring-beans-4.1.2.RELEASE.jar");
	}

	@Test // gh-21
	public void exclusionsAreNotInheritedAndDoNotAffectTransitiveDependencies() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("groovy-2.3.8.jar",
				"spring-boot-starter-test-1.2.0.RELEASE.jar", "junit-4.12.jar", "mockito-core-1.10.8.jar",
				"hamcrest-core-1.3.jar", "hamcrest-library-1.3.jar", "spring-core-4.1.3.RELEASE.jar",
				"spring-test-4.1.3.RELEASE.jar", "objenesis-2.1.jar");
	}

	@Test // gh-23
	public void exclusionsFromAncestorsOfADependencyAreAppliedCorrectly() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).contains("ribbon-loadbalancer-2.0-RC13.jar");
	}

	@Test
	public void pomExclusionsCanBeDisabled() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-core-4.0.6.RELEASE.jar",
				"commons-logging-1.1.3.jar");
	}

	@Test
	public void exclusionsAreAppliedCorrectlyToDependenciesThatAreReferencedMultipleTimes() {
		this.gradleBuild.runner().withArguments("resolve").build();
		for (String line : readLines("resolved.txt")) {
			assertThat(line.startsWith("groovy-all")).isFalse();
		}
	}

	@Test // gh-33
	public void transitiveDependenciesWithACircularReferenceAreTolerated() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).isNotEmpty();
	}

	@Test
	public void managedDependencyCanBeConfiguredUsingAGString() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).containsOnly("org.springframework:spring-core -> 4.1.5.RELEASE");
	}

	@Test
	public void exclusionsAreHandledCorrectlyForDependenciesThatAppearMultipleTimes() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).contains("validation-api-1.1.0.Final.jar");
	}

	@Test
	public void bomImportOrderIsReflectedInManagedVersions() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).contains("org.springframework:spring-core -> 4.2.3.RELEASE");
	}

	@Test
	public void bomImportOrderIsReflectedInManagedVersionsWhenSameBomIsImportedMultipleTimes() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).contains("org.springframework:spring-core -> 4.2.3.RELEASE");
	}

	@Test
	public void managedVersionsOfAConfigurationCanBeAccessed() {
		this.gradleBuild.runner().withArguments("verify").build();
	}

	@Test
	public void dependencyWithAMissingComponentInItsStringIdentifierProducesAHelpfulError() {
		String output = this.gradleBuild.runner().withArguments("build").buildAndFail().getOutput();
		assertThat(output)
				.contains("Dependency identifier 'a:1.0' is malformed. The required form is 'group:name:version'");
	}

	@Test
	public void dependencyWithAMissingComponentInItsMapIdentifierProducesAHelpfulError() {
		String output = this.gradleBuild.runner().withArguments("build").buildAndFail().getOutput();
		assertThat(output).contains("Dependency identifier '{group=a}' did not specify name, version");
	}

	@Test
	public void dynamicVersionIsNotAddedToDependencyManagement() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		for (String managedVersion : readLines("managed-versions.txt")) {
			assertThat(managedVersion.startsWith("commons-logging:commons-logging ->")).isFalse();
		}
	}

	@Test
	public void dependencyManagementIsNotAppliedToADependencyUseLatestIntegration() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).doesNotContain("commons-logging-1.1.3.jar");
	}

	@Test
	public void dependencyManagementIsNotAppliedToAnInheritedDependencyUseLatestIntegration() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).doesNotContain("commons-logging-1.1.3.jar");
	}

	@Test
	public void dependencyManagementIsAppliedToATransitiveDependencyDeclaredWithARange() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).contains("guava-18.0.jar");
	}

	@Test
	public void propertyInABomCanBeOverriddenWhenItIsImported() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).contains("org.springframework:spring-core -> 4.3.0.RELEASE");
	}

	@Test
	public void propertyInABomCanBeConfiguredUsingAReferenceToAProjectProperty() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).contains("org.springframework:spring-core -> 4.3.0.RELEASE");
	}

	@Test
	public void whenOverridingABomPropertyAPropertyOnAnImportTakesPrecedenceOverAProjectProperty() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).contains("org.springframework:spring-core -> 4.3.0.RELEASE");
	}

	@Test
	public void userProvidedResolutionStrategyRunsAfterInternalResolutionStrategy() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).contains("spring-core-4.2.6.RELEASE.jar");
	}

	@Test
	public void exclusionsInImportedBomsForUnresolvableDependenciesAreApplied() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("unresolvable-transitive-dependency-1.0.jar");
	}

	@Test
	public void unresolvableDependenciesAreIgnoredWhenApplyingMavenStyleExclusions() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-core-4.0.3.RELEASE.jar",
				"unresolvable-transitive-dependency-1.0.jar");
	}

	@Test
	public void dependencyManagementBeingOverridenByDependenciesCanBeDisabled() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("spring-core-4.0.3.RELEASE.jar");
	}

	@Test
	public void artifactsWithAPomAreTolerated() throws IOException {
		File libDir = new File(this.gradleBuild.runner().getProjectDir(), "lib");
		libDir.mkdirs();
		new File(libDir, "foo-1.0.0.jar").createNewFile();
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("foo-1.0.0.jar");
	}

	@Test
	public void configurationCanBeUsedDirectlyWhenConfiguringConfigurationSpecificDependencyManagement() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).containsOnly("org.springframework:spring-core -> 4.0.0.RELEASE");
	}

	@Test
	public void configurationCanBeReferredToByNameWhenConfiguringConfigurationSpecificDependencyManagement() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).containsOnly("org.springframework:spring-core -> 4.0.0.RELEASE");
	}

	@Test
	public void whenImportingABomDependencyManagementWithAClassifierIsIgnored() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).isEmpty();
	}

	@Test
	public void whenImportingABomDependencyManagementWithNoVersionIsIgnored() {
		this.gradleBuild.runner().withArguments("managedVersions").build();
		assertThat(readLines("managed-versions.txt")).isEmpty();
	}

	@Test
	public void exclusionThatIsMalformedIsTolerated() {
		this.gradleBuild.runner().withArguments("resolve").build();
		assertThat(readLines("resolved.txt")).containsOnly("malformed-exclude-1.0.jar", "spring-core-4.1.2.RELEASE.jar",
				"commons-logging-1.1.3.jar");
	}

	private void writeLines(File file, String... lines) {
		file.getParentFile().mkdirs();
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(file));
			for (String line : lines) {
				writer.println(line);
			}
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		finally {
			if (writer != null) {
				writer.close();
			}
		}
	}

	private List<String> readLines(String filename) {
		File projectDir = this.gradleBuild.runner().getProjectDir();
		File file = new File(projectDir, "build/" + filename);
		return readLines(file);
	}

	private List<String> readLines(File input) {
		BufferedReader reader = null;
		try {
			List<String> lines = new ArrayList<String>();
			reader = new BufferedReader(new FileReader(input));
			String line = null;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
			return lines;
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				}
				catch (IOException ex2) {
					// Swallow
				}
			}
		}
	}

}
