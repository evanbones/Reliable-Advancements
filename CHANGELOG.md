# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [6.1.1] - 2026-08-28

### Changed

- Modifying a tab will no longer send advancement toasts.

### Fixed

- Various syncing fixes.

## [6.1.0] - 2026-08-28

### Fixed

- Massive performance improvements.

## [6.0.0] - 2026-08-27

### Added

- Added a visual advancement editor and revamped the raw JSON criteria editor.
- Added a loading overlay for when advancements are reloading.
- Added a config option for how deep advancements are revealed (replaces "discovery mode").

### Changed

- Switched to Codec UI for advancement criteria editing.
- Rewrote large parts of the UI.
- Advancements can now have multiple parents or no parents at all.
- Advancement layouts are now fully deterministic.
- Most in-game messages now use translation keys.

### Fixed

- Fixed server-side changes requiring a full reload.
- Fixed issues with tab resetting.
- Many miscellaneous fixes.

## [5.0.1] - 2026-08-19

### Fixed

- Minor fixes to advancement lines.

## [5.0.0] - 2026-08-18

### Changed

- By default, advancement edits are now saved **globally** for modpack purposes.
- Advancement edits now use a priority system for edits:
    - First priority is world-specific edits.
    - Second is the legacy pre-5.x.x datapacks.
    - Finally is the global advancement edits in `config/reliable_advancements_edits`
- Changed mod logo.

### Fixed

- Fixed issues removing Blueprint-based remolded advancements.

## [4.2.0] - 2026-08-18

### Fixed

- Fixed removed child advancements still showing up.
- Fixed zoom resetting on page reload.
- Fixed edit mode-visible advancements disappearing on /reload.
- Fixed scroll position resetting when closing and reopening the advancement tab.

## [4.1.3] - 2026-08-18

### Fixed

- Fixed small visual bug with advancement connections.

## [4.1.2] - 2026-08-05

### Fixed

- Fixed right arrow texture being one pixel off (still literally unplayable).
- Pressing E from the advancements menu now returns you to your previous screen.

## [4.1.1] - 2026-08-04

### Fixed

- Fixed arrow texture being one pixel off (literally unplayable).

## [4.1.0] - 2026-08-03

### Changed

- Improved error reporting.
- Reworked advancement arrows.

### Fixed

- Fixed scrolling working strangely on certain tabs.

## [4.0.2] - 2026-06-23

### Fixed

- Fixed calculations for number of tabs.

## [4.0.1] - 2026-06-23

### Fixed

- Fixed tab pagination on smaller GUI scales.

## [4.0.0] - 2026-05-31

### Changed

- Renamed mod from Advancement Enhancement to Reliable Advancements.

### Added

- Added a field to configure randomized backgrounds.
- Added a config option to darken the background (closer matching the pre-1.12 achievements screen).

### Fixed

- Fixed screen shifting not working properly with the inventory button options.

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