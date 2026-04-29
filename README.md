# QuotaHub

QuotaHub is an Android quota dashboard built with Jetpack Compose. It is designed to help users track remaining AI subscription quotas, inspect model-level limits, and monitor upcoming reset windows from a single app. The current codebase integrates multiple providers, including `OpenAI Codex`, `Kimi Coding Plan`, `MiniMax Coding Plan`, `Z.ai`, and `Zhipu BigModel`.

<p align="center">
  <img src="docs/screenshots/quota-hub-home.png" alt="QuotaHub home screen" width="19%">
  <img src="docs/screenshots/quota-hub-detail.png" alt="QuotaHub quota detail screen" width="19%">
  <img src="docs/screenshots/quota-hub-add-provider.png" alt="QuotaHub add provider screen" width="19%">
  <img src="docs/screenshots/quota-hub-settings.png" alt="QuotaHub settings screen" width="19%">
  <img src="docs/screenshots/quota-hub-kimi.png" alt="QuotaHub Kimi quota screen" width="19%">
</p>

## Current Features

- Add Codex, Kimi, MiniMax, Z.ai, or Zhipu subscriptions from the app and validate credentials before saving them.
- Show connected subscriptions on the home screen with model counts, remaining calls, risk level, and nearest reset time.
- Open a subscription detail page to inspect model-level quota data, pull to refresh, rename the subscription, or disconnect it.
- Cache subscriptions and the latest quota snapshot locally with Room.
- Persist provider-specific raw payloads so normalized quota snapshots can be replayed after schema upgrades.
- Persist UI preferences for `High-emphasis metrics` and `Haptic confirmation`.

## Tech Stack

- Kotlin 2.3.10
- Android Gradle Plugin 9.1.1
- Jetpack Compose + Material 3
- Navigation Compose
- ViewModel + Kotlin Flow
- Room
- Retrofit + OkHttp + Kotlinx Serialization

## Requirements

- A recent stable Android Studio version
- JDK 11
- Android SDK 36
- A device or emulator running at least Android 13 (`minSdk 33`)

## Run Locally

```bash
./gradlew assembleDebug
```

To install the debug build on a connected device:

```bash
./gradlew installDebug
```

After launching the app:

1. Tap the add action on the home screen.
2. Choose `OpenAI Codex`, `Kimi Coding Plan`, `MiniMax Coding Plan`, `Z.ai`, or `Zhipu BigModel`.
3. Enter the required credentials and optionally set a custom title.
4. Save the subscription to validate credentials and fetch the initial quota snapshot.

## CI/CD

GitHub Actions builds the project in two paths:

- `Android Debug CI` runs on every push and pull request, executes
  `testDebugUnitTest`, builds the debug APK, and uploads it as an artifact.
- `Android Release CI` runs manually or on `v*` tags, executes
  `testDebugUnitTest`, builds the signed release APK with R8 and resource
  shrinking enabled, uploads both the APK and R8 mapping file, and creates a
  GitHub Release when triggered by a tag.

The release workflow expects these repository secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

To prepare the current release key for `ANDROID_KEYSTORE_BASE64`:

```bash
base64 -w 0 /home/luren/Android/MyBuildKeys/Skxkey.jks
```

## Architecture Overview

The app is organized around one provider registry, one repository-backed quota
store, and provider modules that supply API clients plus UI projection logic.

```mermaid
flowchart LR
    subgraph App["Application composition"]
        MainActivity["MainActivity"]
        QuotaApplication["QuotaApplication"]
        ProviderModules["ProviderModules.all"]
        Assembly["ProviderRegistryAssembly"]
        ProviderCatalog["ProviderCatalog"]
        ProviderUiMetadata["ProviderUiMetadata"]
        ProviderUiRegistry["ProviderUiRegistry"]
        CardRegistry["SubscriptionCardProjectorRegistry"]
        DetailRegistry["ProviderQuotaDetailProjectorRegistry"]
        RefreshPolicy["DefaultSubscriptionRefreshPolicy"]
        SyncCoordinator["DefaultSubscriptionSyncCoordinator"]
        UpgradeCoordinator["QuotaUpgradeCoordinator"]
    end

    subgraph UI["Compose UI"]
        QuotaApp["QuotaApp"]
        NavHost["QuotaNavHost"]
        HomeScreen["HomeScreen"]
        DetailScreen["ProviderQuotaScreen"]
        SettingsScreen["SettingsScreen"]
        BottomNav["QuotaNavigationBar"]
        HomeVM["HomeHubViewModel"]
        DetailVM["ProviderQuotaViewModel"]
        PreferencesRepo["UiPreferencesRepository"]
    end

    subgraph SubscriptionLayer["Subscription runtime"]
        Registry["SubscriptionRegistry"]
        Gateway["SubscriptionGateway"]
        WriteGateway["RepositoryBackedSubscriptionGateway"]
        ReadOnlyGateway["ReadOnlySubscriptionGateway"]
        GatewayStore["SubscriptionGatewayStore"]
    end

    subgraph RepositoryLayer["Repository and sync"]
        Repository["SubscriptionRepository"]
        ReplayRunner["QuotaSnapshotReplayRunner"]
        CredentialVault["EncryptedCredentialVault"]
        ApiKeyCipher["AndroidKeystoreApiKeyCipher"]
        FailureMapping["ProviderFailure to SyncState mapping"]
    end

    subgraph Persistence["Local persistence"]
        Database[(QuotaDatabase)]
        SubscriptionDao["SubscriptionDao"]
        SnapshotDao["QuotaSnapshotDao"]
        UpgradeStateDao["QuotaUpgradeStateDao"]
        SubscriptionTable["subscription"]
        SnapshotTable["quota_snapshot"]
        ResourceTable["quota_resource"]
        WindowTable["quota_window"]
        UpgradeStateTable["quota_upgrade_state"]
        SharedPrefs["SharedPreferences"]
    end

    subgraph ProviderContract["Provider contract"]
        ProviderDescriptor["ProviderDescriptor"]
        CredentialFields["CredentialFieldSpec"]
        SecretBundle["SecretBundle"]
        CodingProvider["CodingPlanProvider"]
        CapturedSnapshot["CapturedQuotaSnapshot"]
        ReplayPayload["ProviderReplayPayload"]
        CardProjector["SubscriptionCardProjector"]
        DetailProjector["ProviderQuotaDetailProjector"]
    end

    subgraph DomainModel["Normalized domain model"]
        Subscription["Subscription"]
        SyncStatus["SubscriptionSyncStatus"]
        QuotaSnapshot["QuotaSnapshot"]
        QuotaResource["QuotaResource"]
        QuotaWindow["QuotaWindow"]
        QuotaRisk["QuotaRisk"]
    end

    subgraph ProviderImplementations["Provider implementations"]
        Codex["Codex provider module"]
        Kimi["Kimi provider module"]
        MiniMax["MiniMax provider module"]
        MonitorFamily["MonitorQuotaProviderFamily"]
        Zai["Z.ai provider module"]
        Zhipu["Zhipu provider module"]
    end

    subgraph Network["Remote APIs"]
        CodexApi["chatgpt.com quota endpoints"]
        KimiApi["api.kimi.com coding usage"]
        MiniMaxApi["minimaxi.com coding plan remains"]
        ZaiApi["z.ai monitor usage endpoints"]
        ZhipuApi["bigmodel.cn monitor usage endpoints"]
    end

    MainActivity --> QuotaApplication
    MainActivity --> QuotaApp
    QuotaApplication --> ProviderModules
    QuotaApplication --> Assembly
    QuotaApplication --> ProviderCatalog
    QuotaApplication --> ProviderUiRegistry
    QuotaApplication --> DetailRegistry
    QuotaApplication --> RefreshPolicy
    QuotaApplication --> Database
    QuotaApplication --> Repository
    QuotaApplication --> Registry
    QuotaApplication --> SyncCoordinator
    QuotaApplication --> UpgradeCoordinator
    QuotaApplication --> PreferencesRepo
    ProviderModules --> Assembly
    ProviderModules --> ProviderCatalog
    ProviderModules --> CodingProvider
    ProviderModules --> ProviderUiMetadata
    ProviderModules --> CardProjector
    ProviderModules --> DetailProjector
    Assembly --> ProviderUiRegistry
    Assembly --> CardRegistry
    Assembly --> DetailRegistry
    ProviderModules --> Codex
    ProviderModules --> Kimi
    ProviderModules --> MiniMax
    ProviderModules --> Zai
    ProviderModules --> Zhipu

    QuotaApp --> NavHost
    QuotaApp --> BottomNav
    QuotaApp --> PreferencesRepo
    QuotaApp --> Registry
    QuotaApp --> ProviderUiRegistry
    QuotaApp --> DetailRegistry
    QuotaApp --> RefreshPolicy
    NavHost --> HomeScreen
    NavHost --> DetailScreen
    NavHost --> SettingsScreen
    NavHost --> Registry
    NavHost --> ProviderUiRegistry
    NavHost --> DetailRegistry
    NavHost --> RefreshPolicy
    HomeScreen --> HomeVM
    HomeScreen --> Registry
    HomeScreen --> ProviderUiRegistry
    DetailScreen --> DetailVM
    DetailScreen --> ProviderUiRegistry
    SettingsScreen --> PreferencesRepo
    HomeVM --> Registry
    HomeVM --> ProviderUiRegistry
    DetailVM --> Gateway
    DetailVM --> DetailRegistry
    DetailVM --> RefreshPolicy

    Registry --> Repository
    Registry --> ProviderCatalog
    Registry --> CardRegistry
    Registry --> SyncCoordinator
    Registry --> WriteGateway
    Registry --> ReadOnlyGateway
    WriteGateway -. implements .-> Gateway
    ReadOnlyGateway -. implements .-> Gateway
    WriteGateway --> GatewayStore
    ReadOnlyGateway --> GatewayStore

    SyncCoordinator --> Repository
    SyncCoordinator --> ProviderCatalog
    SyncCoordinator --> CodingProvider
    UpgradeCoordinator --> ReplayRunner
    UpgradeCoordinator --> UpgradeStateDao
    UpgradeCoordinator --> ProviderCatalog
    Repository -. implements .-> ReplayRunner
    Repository -. implements .-> GatewayStore
    Repository --> SubscriptionDao
    Repository --> SnapshotDao
    Repository --> CredentialVault
    Repository --> ProviderCatalog
    Repository --> FailureMapping
    CredentialVault --> ApiKeyCipher
    PreferencesRepo --> SharedPrefs

    SubscriptionDao --> Database
    SnapshotDao --> Database
    UpgradeStateDao --> Database
    Database --> SubscriptionTable
    Database --> SnapshotTable
    Database --> ResourceTable
    Database --> WindowTable
    Database --> UpgradeStateTable

    ProviderCatalog --> CodingProvider
    CodingProvider --> ProviderDescriptor
    ProviderUiMetadata --> ProviderUiRegistry
    ProviderDescriptor --> CredentialFields
    CredentialFields --> SecretBundle
    CodingProvider --> CapturedSnapshot
    CapturedSnapshot --> QuotaSnapshot
    CapturedSnapshot --> ReplayPayload
    ReplayPayload --> SnapshotTable
    CardRegistry --> CardProjector
    DetailRegistry --> DetailProjector
    CardProjector --> QuotaSnapshot
    DetailProjector --> QuotaSnapshot

    Repository --> Subscription
    Subscription --> SyncStatus
    QuotaSnapshot --> QuotaResource
    QuotaResource --> QuotaWindow
    QuotaWindow --> QuotaRisk

    Codex -. implements .-> CodingProvider
    Kimi -. implements .-> CodingProvider
    MiniMax -. implements .-> CodingProvider
    Zai --> MonitorFamily
    Zhipu --> MonitorFamily
    MonitorFamily -. implements .-> CodingProvider
    Codex --> CodexApi
    Kimi --> KimiApi
    MiniMax --> MiniMaxApi
    Zai --> ZaiApi
    Zhipu --> ZhipuApi
```

- `QuotaApplication` owns runtime assembly: Room, repositories, provider modules, registries, refresh policy, sync coordinator, and replay coordinator.
- `HomeHubViewModel` observes `SubscriptionRegistry.snapshots`, while `ProviderQuotaViewModel` works through a `SubscriptionGateway` selected by provider support.
- `SubscriptionRegistry` turns repository flows into card projections and returns either `RepositoryBackedSubscriptionGateway` or `ReadOnlySubscriptionGateway`.
- `DefaultSubscriptionSyncCoordinator` is the only path that performs remote refresh or credential revalidation; it calls providers, then asks the repository to cache snapshots and sync state.
- `SubscriptionRepository` owns local persistence, encrypted credential storage, normalized snapshot caching, replay payload persistence, and sync failure mapping.
- `ProviderModule` wires each provider into the shared catalog with its API client, credential descriptor, card projector, detail projector, and replay contract.
- `QuotaUpgradeCoordinator` compares provider replay fingerprints and replays stored raw payloads through `QuotaSnapshotReplayRunner` when normalizers change.

This structure is already used for multi-provider support. Adding a new provider mainly requires a new provider definition, API client, normalization logic, and provider-specific UI projector implementation.

## Implementation Status

- `OpenAI Codex`, `Kimi`, `MiniMax`, `Z.ai`, and `Zhipu` are currently integrated.
- The codebase includes unit tests for repository, gateway, upgrade replay, and provider-specific normalization/projector logic, but it still lacks broader end-to-end coverage.
- Part of the `Settings` screen is still UI-only placeholder behavior and does not fully persist or drive background update policies.

## Security and Release Notes

Based on the current source code:

- Provider credentials are encrypted before being persisted in the local Room
  database using an Android Keystore-backed AES/GCM cipher.
- Network clients use redacted HTTP logging: debug builds keep
  `HttpLoggingInterceptor.Level.BASIC` for request visibility, while release
  builds disable interceptor output. Sensitive headers such as
  `Authorization`, cookies, and account identifiers are redacted before logging.

## Possible Next Steps

- Add support for more AI providers.
- Add scheduled refresh and system notifications.
- Write unit tests for the repository, gateway, and quota calculation logic.
- Add import/export or sync support.

## License

This project is licensed under the [Apache License 2.0](LICENSE).

Third-party provider names, logos, and other brand identifiers remain subject
to their respective owners' rights and are not granted under this repository's
open-source license. See [NOTICE](NOTICE) and [TRADEMARKS.md](TRADEMARKS.md)
for the redistribution boundary.
