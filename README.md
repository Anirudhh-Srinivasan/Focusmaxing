# Focusmaxxing

A gamified competitive study app where friends create lobbies, lock in, study, and get ranked.

## Features

- Email/password, Google, and guest sign-in
- Unique usernames and account upgrades for guests
- User stats summary and public lobby discovery
- Create or join private/public study rooms with six-character codes
- Live room roster and host handoff when someone leaves
- Username search, friend requests, and read-only friend profiles
- Firebase-backed repositories with in-memory development fakes

## Stack

- Kotlin, Jetpack Compose, Material 3
- MVVM with StateFlow, repository interfaces, and a small app container
- Firebase Authentication and Cloud Firestore (optional local configuration)

## Setup

The app runs with in-memory repositories when Firebase is not configured. To use Firebase:

1. Create a Firebase project and add an Android app with package `com.topdawg.focusmaxxing`.
2. Enable Email/Password, Google, and Anonymous providers in Authentication.
3. Create a Cloud Firestore database and deploy the root `firestore.rules` rules.
4. Download the Android `google-services.json` configuration and place it at `app/google-services.json`. This file is ignored by Git.
5. In Firebase Console, open Project settings → General → Your apps → Web app configuration and copy the Web client ID. Put it in the ignored local file `local.properties` as `FOCUSMAXXING_GOOGLE_WEB_CLIENT_ID=...`.
6. Build and run with `./gradlew assembleDebug`.

The Google Services Gradle plugin is applied only when `app/google-services.json` exists. Never commit Firebase configuration or signing secrets.
