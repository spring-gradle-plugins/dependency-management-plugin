# Dependency Management Plugin

A Gradle plugin that provides Maven-like dependency management and exclusions. The
plugin provides a DSL to configure dependency management directly and by importing
existing Maven boms. Based on the configured dependency management, the plugin will
control the versions of your project's direct and transitive dependencies and will honour
any exclusions declared in the poms of your project's dependencies and any imported boms.

To learn more about using the Dependency Management Plugin, please refer to its
[reference documentation][1].

## Contributing

Contributors to this project agree to uphold its [code of conduct][2].
[Pull requests][3] are welcome. Please see the [contributor guidelines][4] for details.

## Licence

Dependency Management Plugin is open source software released under the [Apache 2.0
license][5].

[1]: https://docs.spring.io/dependency-management-plugin/docs/current-SNAPSHOT/reference/html/
[2]: CODE_OF_CONDUCT.md
[3]: https://help.github.com/articles/using-pull-requests/
[4]: CONTRIBUTING.md
[5]: https://www.apache.org/licenses/LICENSE-2.0.html
