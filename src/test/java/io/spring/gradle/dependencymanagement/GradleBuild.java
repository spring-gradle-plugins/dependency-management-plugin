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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * JUnit rule to ease running a Gradle build in a test. Creates a {@link GradleRunner}
 * pre-configured with a project directory that contains a build script found by
 * convention using the test's class name and method name.
 *
 * @author Andy Wilkinson
 */
public class GradleBuild implements TestRule {

    private final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private GradleRunner runner = null;

    @Override
    public Statement apply(final Statement base, final Description description) {
        return this.temporaryFolder.apply(new Statement() {

            @Override
            public void evaluate() throws Throwable {
                List<File> pluginClasspath = Arrays.asList(new File("build/classes/main").getAbsoluteFile(), new File("build/resources/main").getAbsoluteFile(),
                        new File("build/libs/maven-repack-3.0.4.jar").getAbsoluteFile());
                runner = GradleRunner.create().withPluginClasspath(pluginClasspath).withProjectDir(temporaryFolder.getRoot());
                runner.withArguments("-PmavenRepo=" + new File("src/test/resources/maven-repo").getAbsolutePath());
                Class<?> testClass = description.getTestClass();
                String methodName = description.getMethodName();
                if (methodName.contains("[")) {
                    methodName = methodName.substring(0, methodName.indexOf('['));
                }
                InputStream input = testClass.getResourceAsStream(initials(testClass) + "/" + methodName + ".gradle");
                if (input == null) {
                    throw new IllegalStateException("No build script found for " + testClass.getName() + " " + methodName);
                }
                copy(input, temporaryFolder.newFile("build.gradle"));
                copyRecursively(new File("src/test/resources/maven-repo"), temporaryFolder.newFolder("maven-repo"));
                try {
                    base.evaluate();
                }
                finally {
                    runner = null;
                }

            }
        }, description);
    }

    private String initials(Class<?> type) {
        String simpleName = type.getName().substring(type.getPackage().getName().length() + 1);
        StringBuilder initials = new StringBuilder();
        for (char c: simpleName.toCharArray()) {
            if (Character.isUpperCase(c)) {
                initials.append(c);
            }
        }
        return initials.toString();
    }

    public GradleRunner runner() {
        return runner;
    }

    public static void copyRecursively(File src, File dest) throws IOException {
        doCopyRecursively(src, dest);
    }

    private static void doCopyRecursively(File src, File dest) throws IOException {
        if (src.isDirectory()) {
            dest.mkdir();
            File[] entries = src.listFiles();
            for (File entry : entries) {
                doCopyRecursively(entry, new File(dest, entry.getName()));
            }
        }
        else {
            copy(new FileInputStream(src), dest);
        }
    }


    private static void copy(InputStream input, File target) {
        try {
            copy(input, new FileOutputStream(target));
        }
        catch (IOException ex) {
        }
    }

    private static void copy(InputStream input, OutputStream output) {
        try {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) > 0) {
                output.write(buffer, 0, read);
            }
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        finally {
            closeQuietly(input);
            closeQuietly(output);
        }
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            }
            catch (IOException ex) {
                // Swallow
            }
        }
    }

}
