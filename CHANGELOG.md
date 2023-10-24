# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [UNRELEASED]
- Fix CLI complaining settings file is not compatible when it really is.
- [core profile] Add some utility namespaces: `shared.utils`,
  `shared.utils.string`, `shared.utils.transit`,
  `domain.miscellaneous`.
- [core profile] Add Duct configuration `keyword` and `edn` reader tag.
- [core profile] Upgrade base Docker image to use Debian bookworm.
- [core profile] Remove duct.module/web dependency.
- [core profile] Switch to httpkit server instead of Jetty by default.
- [core profile] Improve and fix Reitit middlewares configuration.
- [core profile] Add nested-query-params Reitit middleware.
- [core profile] Use custom Malli coercer in Reitit to improve string
  parameters support.
- [core profile] Use custom Transit decoders/encoders with Muuntaja to
  handle time Instants using `cljc.java-time`.
- [frontend profile] Introduce `cljc.java-time` for time handling.
- [frontend profile] Improve view navigation machinery.
- [frontend profile] Use custom Transit decoders/encoders to handle
  time Instants using `cljc.java-time`.
- [auth keycloak] Improve and extend FE session management code.
- [auth keycloak/cognito] Improve authentication middleware.
- [persistence] Include next.jdbc instead of gethop/sql-utils
- [persistence] Fix both rds and docker init-scripts being included
  instead of just the selected one.
- [on premises] Fix https-portal docker-compoose being always included.
- Upgrade external dependencies of multiple libraries

## [0.1.9] - 2023-05-3081
- Fix incorrect naming of AWS KMS key aliases [#18]

## [0.1.8] - 2023-05-28
- Fix incorrect management of environment entries with '$' chars in them
- Fix: "reset" volume paths that are supposed to be relative, instead of using arbitrarily made up absolute paths.

## [0.1.7] - 2023-05-07
 - Fix permissions in the deployment bundle files. Certain CI environments set an overly permissive umask and bundle files and directories were being created with 'group' and 'other' write permission.

## [0.1.6] - 2023-03-21
- [core] Dockerfile: bump curl version to 7.74.0-1.3+deb11u7

## [0.1.5] - 2023-02-23
- Fix Docker Compose version in the merged to deploy `docker-compose` file.
- Fix persistence profile generating wrong `persistence` adapter key name.
- Fix development migrations being executed before the production migrations.
- Fix typo in `reitit` `strip-extra-keys` configuration parameter.
- Fix Bitbucket Pipelines incompatibility because of the usage of the Docker Compose volumes long syntax.
- Fix using wrong Docker Compose configuration for container memory limits.
- Add missing Grafana environment variables in `docker-compose`.
- Add `app` specific `.gitignore` file.
- Add `persistence` adapter to the `config.edn` `common-config`.
- Add `docker` utility scripts to the `on-premises` deployment bundle.
- Improve `clj-kondo` and `lsp` configuration `.gitignore`s.

## [0.1.4] - 2023-02-01

- Fix `cli-and-settings-version-compatible?` logic to consider newer CLI versions.

## [0.1.3] - 2023-01-30

- Fix Grafana `:server-domain` option type in `:to-develop` configuration tree.

## [0.1.2] - 2023-01-30

- Refactor `:cloud-provider` key to `:deployment-target`.
- Add settings root node to add special metadata such as `:version`.
- Add `settings_patcher.clj` to keep backwards compatibility with settings files previous to 0.1.2.
- Implement on-premises profile and deployment target #4.

## [0.1.1] - 2023-01-14

- Make SQL instructions post-install step a bit clearer.
- Rework docker-compose files to use the compose spec.
- Improve CLI output legibility and UX.
- Fix clj-kondo warnings about unused ns and symbol.
- Rework AWS EB deployment script to work with docker-compose 2.x.
- Bump minimum supported version for Babashka.

## [0.1.0] - 2023-01-12

### Added
- Initial stable HOP CLI version.

[UNRELEASED]:  https://github.com/gethop-dev/hop-cli/compare/0.1.9...HEAD
[0.1.9]: https://github.com/gethop-dev/hop-cli/releases/tag/0.1.9
[0.1.8]: https://github.com/gethop-dev/hop-cli/releases/tag/0.1.8
[0.1.7]: https://github.com/gethop-dev/hop-cli/releases/tag/0.1.7
[0.1.6]: https://github.com/gethop-dev/hop-cli/releases/tag/0.1.6
[0.1.5]: https://github.com/gethop-dev/hop-cli/releases/tag/0.1.5
[0.1.4]: https://github.com/gethop-dev/hop-cli/releases/tag/0.1.4
[0.1.3]: https://github.com/gethop-dev/hop-cli/releases/tag/0.1.3
[0.1.2]: https://github.com/gethop-dev/hop-cli/releases/tag/0.1.2
[0.1.1]: https://github.com/gethop-dev/hop-cli/releases/tag/0.1.1
[0.1.0]: https://github.com/gethop-dev/hop-cli/releases/tag/0.1.0
