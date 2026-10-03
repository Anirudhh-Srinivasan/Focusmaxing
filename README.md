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

1. Create a Firebase project, then add an Android app with package `com.topdawg.focusmaxxing`.
2. Add your local signing certificate SHA-1 to the Android app in Project settings. Run `./gradlew signingReport` to get the debug SHA-1.
3. In **Authentication → Sign-in method**, enable **Email/Password**, **Google**, and **Anonymous**. Save the Google provider's Web client ID for step 6.
4. In **Firestore Database**, create a Native mode database. Publish the root `firestore.rules` contents in the Firestore Rules tab, or deploy them with the Firebase CLI after selecting this project (`firebase deploy --only firestore:rules`).
5. Download the Android config from **Project settings → General → Your apps** and place it at `app/google-services.json`. This file is ignored by Git, and the Google Services Gradle plugin is applied only when it exists.
6. Put the Google provider's Web client ID from Authentication settings in the ignored root `local.properties` file as `FOCUSMAXXING_GOOGLE_WEB_CLIENT_ID=your-web-client-id.apps.googleusercontent.com`. Keep any existing `sdk.dir` line in that file.
7. Build and run with `./gradlew assembleDebug`.

Use Android Studio's bundled JDK 17 or 21 for Gradle with the pinned Kotlin 2.0.21 toolchain.

The Google Services Gradle plugin is applied only when `app/google-services.json` exists. Never commit Firebase configuration or signing secrets.
