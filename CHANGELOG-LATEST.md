### Added

- Tabs are now their own thing, independent of any root advancement. 
  - A tab can hold no advancements at all, a single root, or several roots.
- Tabs now have a separate right-click context menu.
- Deleted tabs can be restored from the context menu, bringing back their advancements, their edits and all progress.
- Deleted advancements can also be restored from the context menu.
- Added multiselect support for deleting and resetting advancements.

### Changed

- Tab appearance and advancement layout are stored on the server and shared by everyone, instead of living in each
  player's local config.

### Fixed

- Closing the tab properties editor returns you to the tab you were on.
- Deleted advancements no longer reappear in unrelated tabs.
- Deleting the last root advancement in a tab no longer deletes the tab.
- Fixed mouse scroll distances.