# Dependency management plugin [![Build status][2]][1]

[![Join the chat at https://gitter.im/spring-gradle-plugins/dependency-management-plugin](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/spring-gradle-plugins/dependency-management-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

A Gradle plugin that provides Maven-like dependency management and exclusions. Based on the
configured dependency management metadata, the plugin will control the versions of your
project's direct and transitive dependencies and will honour any exclusions declared in the
poms of your project's dependencies.

## Requirements

 - Gradle 1.x or 2.x (the plugin is tested against 1.12 and 2.0)

## Using the plugin

The plugin is [available from Gradle's plugin portal][3]. The latest release is `0.4.0.RELEASE`.

With Gradle 2.1 or later, you can use it as follows:

```groovy
plugins {
    id "io.spring.dependency-management" version "0.4.0.RELEASE"
}

```

Alternatively, on earlier versions of Gradle:

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath "io.spring.gradle:dependency-management-plugin:0.4.0.RELEASE"
    }
}

apply plugin: "io.spring.dependency-management"
```

With this basic configuration in place, you're ready to configure the project's dependency
management and declare its dependencies.

## Dependency management configuration

You have two options for configuring the plugin's dependency management: use the plugin's
DSL to configure dependency management directly, or you can import one or more existing Maven boms.
Dependency management can be applied to every configuration (the default) or to one or more specific
configurations.

### Dependency management DSL

The DSL allows you to declare dependency management using `dependency 'groupId:artifactId:version'`
or `dependency group:'group', name:'name', version:version'`. For example:

```groovy
dependencyManagement {
     dependencies {
          dependency 'org.springframework:spring-core:4.0.3.RELEASE'
          dependency group:'commons-logging', name:'commons-logging', version:'1.1.2'
     }
}

dependencies {
     compile 'org.springframework:spring-core'
}
```

This configuration will cause all dependencies (direct or transitive) on `spring-core` and
`commons-logging` to have the versions `4.0.3.RELEASE` and `1.1.2` respectively:

```
$ gradle dependencies --configuration compile
:dependencies

------------------------------------------------------------
Root project
------------------------------------------------------------

compile - Compile classpath for source set 'main'.
\--- org.springframework:spring-core: -> 4.0.3.RELEASE
     \--- commons-logging:commons-logging:1.1.3 -> 1.1.2
```

When you want to provide dependency management for multiple modules with the same group and
version you should use a dependency set. Using a dependency set removes the need to specify
the same group and version multiple times:

```groovy
dependencyManagement {
     dependencies {
          dependencySet(group:'org.slf4j', version: '1.7.7') {
               entry 'slf4j-api'
               entry 'slf4j-simple'
          }
     }
}
```

#### Exclusions

You can also use the DSL to declare exclusions. The two main advantages of using this mechanism
are that they will be included in the `<dependencyManagement>` of your project's
[generated pom](#pom-generation) and that they will be applied using
[Maven's exclusion semantics](#maven-exclusions).

An exclusion can be declared on individual dependencies:

```groovy
dependencyManagement {
    dependencies {
        dependency('org.springframework:spring-core:4.0.3.RELEASE') {
            exclude 'commons-logging:commons-logging'
        }
    }
}
```

An exclusion can also be declared on an entry in a dependency set:

```groovy
dependencyManagement {
    dependencies {
        dependencySet(group:'org.springframework', version: '4.1.4.RELEASE') {
            entry('spring-core') {
                exclude group: 'commons-logging', name: 'commons-logging'
            }
        }
    }
}
```

Note that, as shown in the two examples above, an exclusion can be identified using `'group:name'`
or `group: 'group', name: 'name'`.

### Importing a Maven bom

The plugin also allows you to import an existing Maven bom to utilise its dependency management.
For example:

```groovy
dependencyManagement {
     imports {
          mavenBom 'io.spring.platform:platform-bom:1.0.1.RELEASE'
     }
}

dependencies {
     compile 'org.springframework.integration:spring-integration-core'
}
```

This configuration will apply the [versions in the Spring IO Platform bom][4] to the project's
dependencies:

```
$ gradle dependencies --configuration compile
:dependencies

------------------------------------------------------------
Root project
------------------------------------------------------------

compile - Compile classpath for source set 'main'.
\--- org.springframework.integration:spring-integration-core: -> 4.0.2.RELEASE
     +--- org.springframework.retry:spring-retry:1.1.0.RELEASE
     |    \--- org.springframework:spring-context:4.0.3.RELEASE -> 4.0.6.RELEASE
     |         +--- org.springframework:spring-aop:4.0.6.RELEASE
     |         |    +--- aopalliance:aopalliance:1.0
     |         |    +--- org.springframework:spring-beans:4.0.6.RELEASE
     |         |    |    \--- org.springframework:spring-core:4.0.6.RELEASE
     |         |    |         \--- commons-logging:commons-logging:1.1.3
     |         |    \--- org.springframework:spring-core:4.0.6.RELEASE (*)
     |         +--- org.springframework:spring-beans:4.0.6.RELEASE (*)
     |         +--- org.springframework:spring-core:4.0.6.RELEASE (*)
     |         \--- org.springframework:spring-expression:4.0.6.RELEASE
     |              \--- org.springframework:spring-core:4.0.6.RELEASE (*)
     +--- org.springframework:spring-tx:4.0.5.RELEASE -> 4.0.6.RELEASE
     |    +--- org.springframework:spring-beans:4.0.6.RELEASE (*)
     |    \--- org.springframework:spring-core:4.0.6.RELEASE (*)
     +--- org.springframework:spring-messaging:4.0.5.RELEASE -> 4.0.6.RELEASE
     |    +--- org.springframework:spring-beans:4.0.6.RELEASE (*)
     |    +--- org.springframework:spring-context:4.0.6.RELEASE (*)
     |    \--- org.springframework:spring-core:4.0.6.RELEASE (*)
     +--- org.springframework:spring-context:4.0.5.RELEASE -> 4.0.6.RELEASE (*)
     \--- org.springframework:spring-aop:4.0.5.RELEASE -> 4.0.6.RELEASE (*)
```

It's provided a version of `4.0.2.RELEASE` for the `spring-integration-core` dependency. It has
also set the version of all of the Spring Framework dependencies to `4.0.6.RELEASE`

#### Overriding versions in a bom

If you want to deviate slightly from the dependency management provided by a bom, it can be
useful to be able to override a particular managed version. There are two ways to do this;
changing the value of a version property and overriding the dependency management.

##### Changing the value of a version property

If the bom has been written to use properties for its versions then you can override the version
by providing a different value for the relevant version property. You should only use this
approach if you do not intend to [generate and publish a Maven pom](#pom-generation) for your
project as it will result in a pom that does not override the version.

When the bom is being processed, Gradle's properties are used as a source during the property
resolution process. If the bom is written to use properties for its versions, this allows you to
override a version.

Building on the example above, the Spring IO Platform bom that is used contains a property
named `spring.version`. This property determines the version of all of the Spring Framework modules
and, by default, its value is `4.0.6.RELEASE`. It can be overridden by configuring the
`spring.version` property via any of the mechanisms that Gradle provides. For example, you may
choose to configure it in your `build.gradle` script:

```groovy
ext['spring.version'] = '4.0.4.RELEASE'
```

Or in `gradle.properties`

```
spring.version=4.0.4.RELEASE
```

Wherever you configure it, the version of any Spring Framework modules will now match the value of
the property:

```
gradle dependencies --configuration compile
:dependencies

------------------------------------------------------------
Root project
------------------------------------------------------------

compile - Compile classpath for source set 'main'.
\--- org.springframework.integration:spring-integration-core: -> 4.0.2.RELEASE
     +--- org.springframework.retry:spring-retry:1.1.0.RELEASE
     |    \--- org.springframework:spring-context:4.0.3.RELEASE -> 4.0.4.RELEASE
     |         +--- org.springframework:spring-aop:4.0.4.RELEASE
     |         |    +--- aopalliance:aopalliance:1.0
     |         |    +--- org.springframework:spring-beans:4.0.4.RELEASE
     |         |    |    \--- org.springframework:spring-core:4.0.4.RELEASE
     |         |    |         \--- commons-logging:commons-logging:1.1.3
     |         |    \--- org.springframework:spring-core:4.0.4.RELEASE (*)
     |         +--- org.springframework:spring-beans:4.0.4.RELEASE (*)
     |         +--- org.springframework:spring-core:4.0.4.RELEASE (*)
     |         \--- org.springframework:spring-expression:4.0.4.RELEASE
     |              \--- org.springframework:spring-core:4.0.4.RELEASE (*)
     +--- org.springframework:spring-tx:4.0.5.RELEASE -> 4.0.4.RELEASE
     |    +--- org.springframework:spring-beans:4.0.4.RELEASE (*)
     |    \--- org.springframework:spring-core:4.0.4.RELEASE (*)
     +--- org.springframework:spring-messaging:4.0.5.RELEASE -> 4.0.4.RELEASE
     |    +--- org.springframework:spring-beans:4.0.4.RELEASE (*)
     |    +--- org.springframework:spring-context:4.0.4.RELEASE (*)
     |    \--- org.springframework:spring-core:4.0.4.RELEASE (*)
     +--- org.springframework:spring-context:4.0.5.RELEASE -> 4.0.4.RELEASE (*)
     \--- org.springframework:spring-aop:4.0.5.RELEASE -> 4.0.4.RELEASE (*)
```

##### Overriding the dependency management

If the bom that you have imported does not use properties, or you want the override to be honoured
in the Maven pom that's generated for your Gradle project, you should use dependency management to
perform the override. For example, if you're using the Spring IO Platform bom, you can override its
version of Guava and have that override apply to the generated pom:

```groovy
dependencyManagement {
    imports {
        mavenBom 'io.spring.platform:platform-bom:1.1.1.RELEASE'
    }
    dependencies {
        dependency 'com.google.guava:guava:18.0'
    }
}
```

This will produce the following `<dependencyManagement>` in the generated pom file:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.spring.platform</groupId>
            <artifactId>platform-bom</artifactId>
            <version>1.1.1.RELEASE</version>
            <scope>import</scope>
            <type>pom</type>
        </dependency>
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>18.0</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

The dependency management for Guava that's declared directly in the pom takes precedence over
any dependency management for Guava in the `platform-bom` that's been imported.

### Dependency management for specific configurations

To target dependency management at a single configuration, you nest the dependency management
within a block named after the configuration. For example, the following will apply dependency
management to the compile configuration:

```groovy
dependencyManagement {
     compile {
          dependencies {
               …
          }
          imports {
               …
          }
     }
}
```

To target dependency management at multiple configurations, you use `configurations` to list the
configurations to which the dependency management should be applied. For example, the following
will apply dependency management to the compile and custom configurations:

```groovy
dependencyManagement {
     configurations(compile, custom) {
          dependencies {
               …
          }
          imports {
               …
          }
     }
}
```

## Accessing properties from imported boms

The plugin makes all of the properties from imported boms available for use in your Gradle build.
Properties from both global dependency management and configuration-specific dependency management
can be accessed. For example, accessing a property named `spring.version` from global dependency
management:

```groovy
dependencyManagement.importedProperties['spring.version']
```

And the same property from the compile configuration's dependency management:

```groovy
dependencyManagement.compile.importedProperties['spring.version']
```

## Maven exclusions

While Gradle can consume dependencies described with a Maven pom file, Gradle doesn't not
honour Maven's semantics when it is using the pom to build the dependency graph. A notable
difference that results from this is in how exclusions are handled. This is best illustrated
with an example:

Consider a Maven artifact, `exclusion-example`, that declares a dependency on
`org.springframework:spring-core` in its pom with an exclusion for
`commons-logging:commons-logging`:

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-core</artifactId>
    <version>4.1.3.RELEASE</version>
    <exclusions>
        <exclusion>
            <groupId>commons-logging</groupId>
            <artifactId>commons-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

If we have a Maven project, `consumer`, that depends on
`exclusion-example` and `org.springframework:spring-beans` the exclusion in `exlusion-example`
prevents a transitive dependency on `commons-logging:commons-logging`. This can be seen in the
output from `mvn dependency:tree`:

```
+- com.example:exclusion-example:jar:1.0:compile
|  \- org.springframework:spring-core:jar:4.1.3.RELEASE:compile
\- org.springframework:spring-beans:jar:4.1.3.RELEASE:compile
```

If we create a similar project in Gradle the dependencies are different as the exclusion of
`commons-logging:commons-logging` is not honored. This can be seen in the output from
`gradle dependencies`:

```
+--- com.example:exclusion-example:1.0
|    \--- org.springframework:spring-core:4.1.3.RELEASE
|         \--- commons-logging:commons-logging:1.2
\--- org.springframework:spring-beans:4.1.3.RELEASE
     \--- org.springframework:spring-core:4.1.3.RELEASE (*)
```

Despite `exclusion-example` excluding `commons-logging` from its `spring-core` dependency,
`spring-core` has still pulled in `commons-logging`.

The dependency management plugin improves Gradle's handling of exclusions that have been declared
in a Maven pom by honoring Maven's semantics for those exclusions. This applies to exclusions
declared in a project's dependencies that have a Maven pom and exclusions declared in imported
Maven boms.

### Disabling Maven exclusions

The plugin's support for applying Maven's exclusion semantics can be disabled:

```groovy
dependencyManagement {
    applyMavenExclusions false
}
```

## Pom generation

Gradle's `maven` and `maven-publish` plugins automatically generate a pom file that describes the
published artifact. The plugin will automatically include any global dependency management, i.e.
dependency management that does not target a specific configuration, in the
`<dependencyManagement>` section of the generated pom file. For example the following dependency
management configuration:

```groovy
dependencyManagement {
     imports {
          mavenBom 'com.example:bom:1.0'
     }
     dependencies {
          dependency 'com.example:dependency:1.5'
     }
}
```

Will result in the following `<dependencyManagement>` in the generated pom file:

```xml
<dependencyManagement>
     <dependencies>
          <dependency>
               <groupId>com.example</groupId>
               <artifactId>bom</artifactId>
               <version>1.0</version>
               <scope>import</scope>
               <type>pom</type>
          <dependency>
          <dependency>
               <groupId>com.example</groupId>
               <artifactId>dependency</artifactId>
               <version>1.5</version>
          </dependency>
     <dependencies>
</dependencyManagement>
```

### Using a bom that is not in Maven Central

Gradle's pom generation requires any boms referenced in a pom's `<dependencyManagement>`
section to be available from Maven Central and publishing will fail if this is not the
case. To work around this limitation the plugin can be configured to copy a bom into the
generated pom rather than importing it:

```groovy
dependencyManagement {
    generatedPomCustomization {
        importedBomAction = 'copy'
    }
}
```

### Disabling the customization of a generated pom

If you prefer to have complete control over your project's generated pom, you can disable
the plugin's customization:

```groovy
dependencyManagement {
    generatedPomCustomization {
        enabled = false
    }
}
```

## Accessing the managed versions

The plugin provides an API for accessing the versions provided by the configured dependency
management. For example, accessing the version for `org.springframework:spring-core` from
global dependency management:

```groovy
dependencyManagement.managedVersions['org.springframework:spring-core']
```

And from the compile configuration's dependency management:

```groovy
dependencyManagement.compile.managedVersions['org.springframework:spring-core']
```

[1]: https://build.spring.io/browse/GRADLEPLUGINS-DMP
[2]: https://build.spring.io/plugins/servlet/buildStatusImage/GRADLEPLUGINS-DMP (Build status)
[3]: http://plugins.gradle.org/plugin/io.spring.dependency-management
[4]: http://docs.spring.io/platform/docs/1.0.1.RELEASE/reference/htmlsingle/#appendix-dependency-versions