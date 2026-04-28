# Changelog

All notable project changes are summarized from the Git history. The project does
not currently tag releases, so entries are grouped by commit date.

## 2026-04-27

- Updated the application launcher icon to use the current app icon asset.
- Reworked the Home provider access area into a compact provider list.
- Added application screenshots to the README.
- Simplified the Settings header by removing the decorative console label.
- Refined the README architecture diagram for source-level accuracy.
- Replaced the old README structure notes with a Mermaid architecture overview.

## 2026-04-26

- Added the Kimi Coding Plan provider, including API integration, quota
  normalization, UI projection, and tests.
- Updated the Kimi provider icon resource.

## 2026-04-24

- Refined Material 3 Expressive surfaces to better distinguish UI layers.

## 2026-04-23

- Hardened subscription sync replay and provider fallback behavior.
- Updated Gradle to 9.4.1 and Android Gradle Plugin to 9.2.0.

## 2026-04-22

- Adopted the Apache 2.0 license and added project notices.

## 2026-04-20

- Consolidated monitor-style quota providers into a shared provider family.
- Regularized provider failure handling and registry assembly.
- Added read-only handling for unsupported providers.
- Added a per-provider replay ledger for quota snapshot upgrades.
- Dropped the temporary work-in-progress migration chain.
- Finished provider family and failure consolidation.

## 2026-04-19

- Excluded the local Room database from Android backup flows.
- Encrypted subscription API keys at rest.
- Normalized provider subscriptions and quota projections.
- Persisted replayable raw quota payloads for future snapshot upgrades.
- Formalized quota snapshot migrations and upgrade replay.
- Removed legacy Room migration compatibility.
- Modularized quota projections and credential recovery.
- Centralized refresh policy and recovery for interrupted syncs.
- Isolated provider credentials behind a vault abstraction.
- Cleared provider connection drafts and guarded duplicate module IDs.
- Added OpenAI Codex quota provider support.
- Added conditional subscription shortcuts in the UI.
- Added Zhipu BigModel quota provider support.
- Fixed provider form opening from the access section.
- Added Z.ai quota provider support.
- Updated the Z.ai vector drawable asset and dimensions.

## 2026-04-18

- Added haptic confirmation preferences.
- Floated the bottom navigation above content.
- Refined the bottom navigation tray surface.
- Updated Android Gradle Plugin to 9.1.1.

## 2026-04-17

- Refactored the Home, Settings, and bottom navigation UI.
- Refined the ProviderQuotaScreen quota workspace.
- Added the high-emphasis metrics preference.
- Aligned MiniMax quota risk states.
- Redesigned the navigation bar with custom tray, indicator transitions,
  adaptive item widths, and improved styling.

## 2026-04-16

- Replaced the Home subscription card with a Material 3 ListItem layout.
- Refactored ProviderQuotaScreen for Material 3 Expressive styling.

## 2026-04-15

- Added subscription renaming.
- Added missing implementation comments.
- Upgraded the Material 3 loading indicator.

## 2026-04-14

- Modeled provider quotas as subscriptions.
- Aligned subscription comments with the implementation.

## 2026-03-26

- Updated Retrofit, OkHttp, Lifecycle, and Kotlinx Serialization dependencies.
- Added responsive layout behavior for quota overview cards.

## 2026-03-24

- Persisted API keys with Room.
- Cached MiniMax quota data locally.
- Refined the MiniMax entry flow and Home loading state.
- Redesigned the provider bottom sheet with a modern Material 3 list style.
- Split quota providers into hub and detail screens.
- Abstracted quota providers behind a generic detail flow.
- Added a LargeTopAppBar to ProviderQuotaScreen with back navigation.
- Simplified bottom navigation for Settings.

## 2026-03-23

- Initialized the Android project.
- Added Material 3 bottom navigation and navigation architecture.
- Replaced the placeholder screen with a real HomeScreen quota overview.
- Added the Home floating action button and provider bottom sheet.
- Added a Material 3 provider list to the bottom sheet.
- Integrated the MiniMax API with real quota data and improved UI.
