# Changelog

All notable project changes are grouped by release version. GitHub Releases use
the matching version section from this file as release notes.

## [Unreleased]

## [v1.4.3] - 2026-05-05

- Added the server/client mode settings shell and persisted its preference.
- Added notification permission gating for quota usage alerts.
- Updated the recommended settings action to respect the current notification
  permission state.
- Added an adaptive provider detail layout for wider screens.
- Updated the Kimi trademark asset reference to the current drawable path.
- Updated Material 3 to 1.5.0-alpha18.

## [v1.4.2] - 2026-05-01

- Added a manual Check for updates action to the About screen, reusing the
  GitHub Releases update dialog and showing inline check status.

## [v1.4.1] - 2026-04-30

- Fixed the landscape Home monitor for Z.ai and Zhipu subscriptions so their
  quota limits render as compact quota bars instead of a single fallback
  headroom bar.

## [v1.4] - 2026-04-30

- Wired the Update rhythm setting into real refresh behavior across Home,
  provider detail, and Settings views.
- Added app-wide automatic quota refresh while QuotaHub is running, using
  Live, Balanced, and Manual cadence settings.
- Changed the Live refresh cadence from 15 minutes to 1 minute.
- Updated provider detail auto-refresh to honor the selected refresh cadence
  instead of using a fixed interval.
- Improved the landscape Home monitor for edge-to-edge and display cutout
  devices by using safe drawing insets.
- Auto-hide floating navigation and landscape monitor controls, revealing them
  again when the user taps the screen.
- Let the landscape monitor Provider and Quota windows headers collapse with
  the compact Settings and Add controls.
- Requested immersive system bars for the edge-to-edge app shell.

## [v1.3] - 2026-04-30

- Added a dark mode preference that can keep QuotaHub in dark mode while
  preserving system-following behavior when disabled.
- Added an immersive landscape monitor HUD setting, enabled by default, to hide
  the landscape Home status HUD while keeping compact monitor controls
  available.
- Reworked the landscape Home monitor into a denser provider list with
  provider-specific quota progress bars for rolling windows, weekly limits, and
  plan quotas.
- Added landscape monitor quota progress support for Kimi, MiniMax, and OpenAI
  Codex projections.
- Improved landscape navigation so Settings can reliably return to the Home
  monitor.
- Made provider selection and credential dialogs scrollable on compact
  landscape displays.
- Redacted sensitive network logging.

## [v1.2] - 2026-04-29

- Added automatic update checks against the latest GitHub Release.
- Added a Material 3 update dialog with the latest version, release notes, and
  a GitHub release action.
- Added persisted dismissal for already-seen update tags.
- Added an app version label to the About screen.

## [v1.1] - 2026-04-29

- Added an About screen with the app icon, author GitHub profile, and app
  GitHub repository links.
- Added Landscape monitor mode to lock QuotaHub in landscape where supported
  and keep the display awake for desk or tablet monitoring.
- Added instrumentation coverage for persisted UI preferences.
- Switched changelog entries from date-grouped history to release-grouped notes.
- Updated the release workflow to publish GitHub Release notes from this file.

## [v1.0] - 2026-04-28

- Added GitHub Actions workflows for debug and signed release builds.
- Added signed release APK generation with R8 minification and resource
  shrinking.
- Added release artifact publishing for the APK and R8 mapping file.
- Added GitHub Release creation for version tags.
- Configured release signing through GitHub repository secrets.
- Updated release builds to allow Gradle cache writes on tag workflows.
- Added application screenshots and refreshed the README architecture overview.
- Updated the application launcher icon to use the current app icon asset.
- Reworked the Home provider access area into a compact provider list.
- Simplified the Settings header by removing the decorative console label.
- Added the Kimi Coding Plan provider, including API integration, quota
  normalization, UI projection, and tests.
- Hardened subscription sync replay and provider fallback behavior.
- Updated Gradle to 9.4.1 and Android Gradle Plugin to 9.2.0.
- Adopted the Apache 2.0 license and added project notices.
- Consolidated monitor-style quota providers into a shared provider family.
- Added read-only handling for unsupported providers.
- Added encrypted storage for subscription API keys.
- Added replayable raw quota payload storage for future snapshot upgrades.
- Formalized quota snapshot migrations and upgrade replay.
- Added OpenAI Codex, Zhipu BigModel, and Z.ai quota provider support.
- Added haptic confirmation preferences and high-emphasis metrics settings.
- Refined Material 3 Expressive screens, bottom navigation, subscription cards,
  provider detail views, and loading indicators.
- Added MiniMax quota integration, local quota caching, and provider entry flows.
- Initialized the Android application with Material 3 navigation and quota
  overview screens.
