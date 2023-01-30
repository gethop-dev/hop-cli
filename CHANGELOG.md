# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [UNRELEASED]

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

[UNRELEASED]:  https://github.com/gethop-dev/hop-cli/compare/0.1.2...HEAD
[0.1.2]: https://github.com/gethop-dev/hop-cli/releases/tag/0.1.1...0.1.2
[0.1.1]: https://github.com/gethop-dev/hop-cli/releases/tag/0.1.0...0.1.1
[0.1.0]: https://github.com/gethop-dev/hop-cli/releases/tag/0.1.0
