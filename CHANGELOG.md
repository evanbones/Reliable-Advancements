# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [4.0.0] - 2026-05-31

### Changed

- Renamed mod from Advancement Enhancement to Reliable Advancements.

### Added

- Added a field to configure randomized backgrounds.
- Added a config option to darken the background (closer matching the pre-1.12 achievements screen).

## [3.0.1] - 2026-05-27

### Fixed

- Fixed crashes on dedicated servers.

## [3.0.0] - 2026-05-22

### Added

- Added a button to toggle advancement editing, and a config option to toggle the button visibility.
- Added a config option to show tooltips even while editing.
- Added multiselect functionality for moving advancements.

### Fixed

- Fixed progress resetting when scrolling.
- Fixed certain advancement edits not taking effect until the screen is reloaded.
- Fixed networking crash with large advancement sizes.

## [2.1.1] - 2026-05-06

### Added

- Added a config option to enable/disable the tooltip in the inventory button.

## [2.1.0] - 2026-05-04

### Added

- Clicking an advancement in chat will now open it in the advancement screen.

## [2.0.2] - 2026-05-04

### Changed

- Adjusted default inventory button texture.

## [2.0.1] - 2026-05-03

### Fixed

- Fixed advancement linking creating ghost advancements.

## [2.0.0] - 2026-05-02

### Changed

- All advancements are now unlocked by default when in Edit Mode.
- Vastly improved advancement reload speeds.
- Improved advancement deleting.

### Added

- Added option for reward claiming (default: false).

## [1.2.0] - 2026-05-02

### Added

- Added new right-click option to create a new Advancement tab.
- Added a config option to change the book icon in the tab/inventory button.
- Added a "discovery mode", where advancements are only revealed as the direct parent is unlocked.

## [1.1.0] - 2026-04-28

### Added

- Added keyboard control for microadjustments to advancement layouts.

### Fixed

- Added additional server permissions checks before editing advancements.

## [1.0.1] - 2026-04-28

### Fixed

- Fixed dedicated server crash on NeoForge.

## [1.0.0] - 2026-04-27

- Initial release.