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
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;

/**
 * JUnit Extension to ease running a Gradle build in a test. Creates a
 * {@link GradleRunner} pre-configured with a project directory that contains a build
 * script found by convention using the test's class name and method name.
 *
 * @author Andy Wilkinson
 */
public class GradleBuild implements BeforeEachCallback {

	private GradleRunner runner = null;

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		Store store = context.getStore(Namespace.create(GradleBuild.class));
		Project project = new Project();
		store.put("project", project);
		this.runner = GradleRunner.create()
			.withPluginClasspath()
			.withProjectDir(project.dir.toFile())
			.withArguments("-PmavenRepo=" + new File("src/test/resources/maven-repo").getAbsolutePath());
		Class<?> testClass = context.getRequiredTestClass();
		String methodName = context.getRequiredTestMethod().getName();
		InputStream input = testClass.getResourceAsStream(initials(testClass) + "/" + methodName + ".gradle");
		if (input == null) {
			throw new IllegalStateException("No build script found for " + testClass.getName() + " " + methodName);
		}
		Files.copy(input, project.dir.resolve("build.gradle"));
		Files.copy(testClass.getResourceAsStream("/gradle.properties"), project.dir.resolve("gradle.properties"));
		copyRecursively(Paths.get("src", "test", "resources", "maven-repo"), project.dir.resolve("maven-repo"));
	}

	private String initials(Class<?> type) {
		String simpleName = type.getName().substring(type.getPackage().getName().length() + 1);
		StringBuilder initials = new StringBuilder();
		for (char c : simpleName.toCharArray()) {
			if (Character.isUpperCase(c)) {
				initials.append(c);
			}
		}
		return initials.toString();
	}

	public GradleRunner runner() {
		return this.runner;
	}

	public static void copyRecursively(Path src, Path dest) throws IOException {
		BasicFileAttributes srcAttr = Files.readAttributes(src, BasicFileAttributes.class);

		if (srcAttr.isDirectory()) {
			Files.walkFileTree(src, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
					new SimpleFileVisitor<Path>() {

						@Override
						public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
								throws IOException {
							Files.createDirectories(dest.resolve(src.relativize(dir)));
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							Files.copy(file, dest.resolve(src.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
							return FileVisitResult.CONTINUE;
						}

					});
		}
		else if (srcAttr.isRegularFile()) {
			Files.copy(src, dest);
		}
	}

	private static final class Project implements CloseableResource {

		private final Path dir;

		private Project() throws IOException {
			this.dir = Files.createTempDirectory("gradle-build-");
		}

		@Override
		public void close() throws Throwable {
			Files.walkFileTree(this.dir, new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult postVisitDirectory(Path path, IOException ex) throws IOException {
					Files.delete(path);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path path, BasicFileAttributes ex) throws IOException {
					Files.delete(path);
					return FileVisitResult.CONTINUE;
				}

			});
		}

	}

}
