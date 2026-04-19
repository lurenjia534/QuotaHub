# QuotaHub

QuotaHub is an Android quota dashboard built with Jetpack Compose. It is designed to help users track remaining AI subscription quotas, inspect model-level limits, and monitor upcoming reset windows from a single app. The current codebase integrates multiple providers, including `OpenAI Codex`, `MiniMax Coding Plan`, and `Zhipu BigModel`.

## Current Features

- Add Codex, MiniMax, or Zhipu subscriptions from the app and validate credentials before saving them.
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

## Project Structure

```text
app/src/main/java/com/lurenjia534/quotahub
├── data
│   ├── local          # Room entities, DAOs, and database
│   ├── model          # Domain models and quota calculation helpers
│   ├── preferences    # Local UI preferences
│   ├── provider       # Provider gateways and registry
│   └── repository     # Local/remote data coordination
├── ui
│   ├── components     # Shared Compose components
│   ├── navigation     # Routes and bottom navigation
│   ├── screens        # Home / Detail / Settings screens
│   └── theme          # Theme, colors, and typography
├── MainActivity.kt
└── QuotaApplication.kt
```

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
2. Choose `OpenAI Codex`, `MiniMax Coding Plan`, or `Zhipu BigModel`.
3. Enter the required credentials and optionally set a custom title.
4. Save the subscription to validate credentials and fetch the initial quota snapshot.

## Architecture Overview

The app currently follows a straightforward layered structure:

- `HomeHubViewModel` and `ProviderQuotaViewModel` drive screen state.
- `SubscriptionRegistry` acts as the application-level entry point for providers and subscription snapshots.
- `SubscriptionRepository` coordinates Room persistence, encrypted credentials, and normalized quota snapshot caching.
- `ProviderModule` wires each provider into a shared registry with provider-specific API clients, card projectors, and detail projectors.

This structure is already used for multi-provider support. Adding a new provider mainly requires a new provider definition, API client, normalization logic, and provider-specific UI projector implementation.

## Implementation Status

- `OpenAI Codex`, `MiniMax`, and `Zhipu` are currently integrated.
- The codebase includes unit tests for repository, gateway, upgrade replay, and provider-specific normalization/projector logic, but it still lacks broader end-to-end coverage.
- Part of the `Settings` screen is still UI-only placeholder behavior and does not fully persist or drive background update policies.

## Security and Release Notes

Based on the current source code, these items should be addressed before a production release:

- Provider credentials are encrypted before being persisted in the local Room database using an Android Keystore-backed AES/GCM cipher.
- The provider API clients use `HttpLoggingInterceptor.Level.BASIC`. Production builds should still review logging policy and redact sensitive request metadata where necessary.
- The Room database uses `fallbackToDestructiveMigration`, which can wipe local subscription and cache data on schema changes.

## Possible Next Steps

- Add support for more AI providers.
- Add scheduled refresh and system notifications.
- Write unit tests for the repository, gateway, and quota calculation logic.
- Add import/export or sync support.

## License

This project is licensed under the [MIT License](LICENSE).
