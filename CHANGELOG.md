# Change Log

## [Unreleased]
### Changed

## 0.2.1
### Added
- Add `qualified-name`

## 0.2.0
### Added
- Add components such as Jetty9Server, HikariCP and Carmine and functions to use them
- Add ring middlewares such as wrap-health-check and wrap-trailing-slash

### Changed
- Use jsonista for encoding and decoding JSON

## 0.1.5
### Fixed
- Fix `toyokumo.commons.ring.response/attachment` to work on IE

## 0.1.4
### Added
- Add `toyokumo.commons.io/excluding-bom-reader`

## 0.1.3
### Added
- Add session utilities to `toyokumo.commons.ring.response`

## 0.1.2
### Added
- Add `charset` option to `url-encode`, `url-decode` in `toyokumo.commons.url` and `html`, `json` and `csv` in `toyokumo.commons.ring.response`

### Breaking
- Divide `toyokumo.commons.ring.response/csv` into `attachment` and `csv`

## 0.1.1
### Fixed
- Remove schema from a cljc file

## 0.1.0
Initial release.
