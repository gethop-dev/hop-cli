# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [UNRELEASED]

## [0.1.22] - 2025-05-14
- Fixed some linting error that made CI fail.

## [0.1.21] - 2025-05-14
- Updated linting tools to latest versions

## [0.1.20] - 2025-05-14
- Updated most dependencies to latest versions.
- [aws profile] Added 'raw' flag to 'get-id-token' command to return the id-token in raw format.

## [0.1.19] - 2025-05-12
- [aws profile] Fix AWS_ROLE_ARN value. We were setting it to the local development IAM *User* ARN. But we need
to set it to the local development IAM *Role* ARN.
- [aws profile] Make the AWS profile inject AWS env variables for local development
- [aws profile][core profile] Make sure we run 'lein clean' in the start-dev.sh scripts with enough permissions to do its job.
- Use the latest magnetcoop:bob docker image

## [0.1.18] - 2025-03-17
- [aws profile] Allow Cognito to automatically send messages to verify and confirm user email
- [aws profile] Don't rely on $PATH to find bb executable. Ut seems the latest Amazon Linux 2 AMI instances don't add /usr/local/bin to the $PATH by default.
- [aws profile] [keycloak profile] Fix --temporary? option handling when changing the user password.

## [0.1.17] - 2025-02-06
- [keycloak cli] Added 'insecure-connection' flag to skip the SSL certificate verification step on API calls.
- [keycloak cli] Added 'raw' flag to 'get-id-token' command to return the id-token in raw format.
- [keycloak cli] Added 'client-secret' parameter to work with clients that require a secret.
- [core profile] Fixed Transit custom tags for Instants.
- [persistence] Refactored <-pgvalue to make it work with scalar JSON(B) values (number, string, boolean)
- [keycloak cli] Added 'cacert' parameter to support custom CA certificates

## [0.1.16] - 2025-01-15
- [aws profile] Added permissions to the local development role to be able to change or reset the Cognito users' password (but only for the local development User Pool).
- [persistence-profile] Make constraint violation check more robust
- Enhance error handling when missing sub-commands
- [aws profile] Only remove from SSM variables with empty values (but not vars with "falsy" values)
- [aws profile] Use Launch Templates when creating ElasticBeanstalk environments, instead of the obsolete Launch Configurations. 

## [0.1.15] - 2024-12-07
- Various profiles: Backported a bunch of utility functions from various customer projects.
- [core profile] Correctly handle environment variables with '$' (and other potentially problematic characters) in them [issue #24]
- [settings editor] Upgraded default Grafana version.
- [settings editor] Upgraded default Keycloak version.
- [on-premises profile] Fixed on-premises-files copying. They were being copied to the wrong directory.
- [keycloak profile] Fix front-end routing when using Keycloak.
- [settings editor] Upgraded default Postgresql version to 17 (now that AWS RDS supports it)
- [settings editor] Added a visual marker to non-collapsible sections in the settings editor.

## [0.1.14] - 2024-11-20
- Upgrade external dependencies of multiple libraries
- Added cljfmt linting for the HOP CLI source code.
- Made AWS platform prebuild script always use the same HOP CLI as the one that was used to create the proejct. [issue #15]
- Added `--help` support for all the HOP CLI commands and sub-commands

## [0.1.13] - 2024-11-12
- [frontend profile] Add browser history replace-state re-frame events
- [object-storage profile] Upgrade object-storage.s3 dependency version
- [core profile] Explicitly set http-kit version instead of using transitive dependencies
- [core profile] Add thread-transactions utility function
- [ci] Upgrade bob container image version (linting tools)
- [settings editor] Upgrade default Postgresql version to 16
- Add support for both `docker-compose` (v1) and `docker compose` (v2) [issue #]
- [core profile] Upgraded JDK to Java 21 (latest LTS at the moment).
- [core profile] Upgraded leiningen and clj-kondo versions (to 2.11.2 and 2024.08.29 respectively)
- [aws profile] Fixed one cloudformation stack description.
- [aws profile] Enhanced messages about what specific stack is being skipped or run.

## [0.1.12] - 2024-04-30
- [aws profile] Correctly handle environment variables with '$'
  characters in them (and other potentially problematic characters in
  them) [issue #24]

## [0.1.11] - 2024-04-12
- [deployment S3]: Fixed incomplete S3 policy [issue #19]
- [cli]: Fixed the cli http client hiding certain failed http request errors [issue #21]
- [auth]: Upgraded buddy-auth.jwt-oidc to latest version (includes several fixes)
- [frontend profile]: Upgrade duct/compiler.sass to 0.3.0 [issue #22][#issue 20]
- [ci][core profile] Upgrade clj-kondo and babashka versions
- [aws profile] Make sure we use the same babashka version as the core profile [issue #16]
- [aws profile] Update hop-cli version on AWS platform prebuild script [issue #15]
- [ci] Make sure short SHA1 commits have at least 10 chars [issue #23]

## [0.1.10] - 2023-11-13
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
- [ci] Fix github-actions AWS environment variables being always included.
- Upgrade external dependencies of multiple libraries

## [0.1.9] - 2023-05-30
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

[UNRELEASED]:  https://github.com/gethop-dev/hop-cli/compare/0.1.22...HEAD
[0.1.22]: https://github.com/gethop-dev/hop-cli/releases/tag/0.1.22
[0.1.21]: https://github.com/gethop-dev/hop-cli/releases/tag/0.1.21
[0.1.20]: https://github.com/gethop-dev/hop-cli/releases/tag/0.1.20
[0.1.19]: https://github.com/gethop-dev/hop-cli/releases/tag/0.1.19
[0.1.18]: https://github.com/gethop-dev/hop-cli/releases/tag/0.1.18
[0.1.17]: https://github.com/gethop-dev/hop-cli/releases/tag/0.1.17
[0.1.16]: https://github.com/gethop-dev/hop-cli/releases/tag/0.1.16
[0.1.15]: https://github.com/gethop-dev/hop-cli/releases/tag/0.1.15
[0.1.14]: https://github.com/gethop-dev/hop-cli/releases/tag/0.1.14
[0.1.13]: https://github.com/gethop-dev/hop-cli/releases/tag/0.1.13
[0.1.12]: https://github.com/gethop-dev/hop-cli/releases/tag/0.1.12
[0.1.11]: https://github.com/gethop-dev/hop-cli/releases/tag/0.1.11
[0.1.10]: https://github.com/gethop-dev/hop-cli/releases/tag/0.1.10
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
