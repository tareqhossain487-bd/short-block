# ShortsBlocker

ShortsBlocker is a modern, lightweight, and privacy-focused Android utility built with Kotlin and Jetpack Compose that automatically detects and blocks distracting short-form video feeds (YouTube Shorts, Facebook Reels, and Instagram Reels) using the Android Accessibility Service.

## Features

- **Automated Feed Interception**: Automatically performs a back action when short-form video player nodes are detected in YouTube, Facebook, and Instagram.
- **Granular Platform Controls**: Individual toggle switches for YouTube Shorts, Facebook Reels, and Instagram Reels.
- **Focus & Time Saved Analytics**: Real-time counter of blocked reels/shorts with estimated time saved and platform breakdown.
- **Live Permission Status**: Visual status card indicating whether the Accessibility Service is enabled in system settings, with one-tap navigation to Android settings.
- **Modern Jetpack Compose UI**: Built with Material Design 3, dynamic theme support, edge-to-edge rendering, and adaptive layouts.
- **100% Offline & Private**: Zero network permissions, no telemetry, no tracking, and no external servers.

## Architecture

- **UI Layer**: Jetpack Compose with Material 3, ViewModel, and StateFlow.
- **Accessibility Service**: `ShortsBlockerService` listens for window and content change events from target apps and closes reel screens.
- **Persistence**: SharedPreferences for local configuration and block stats.
