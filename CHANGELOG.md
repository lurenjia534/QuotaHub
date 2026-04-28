# Changelog

All notable project changes are grouped by release version. GitHub Releases use
the matching version section from this file as release notes.

## [Unreleased]

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
