# Focusmaxxing

A gamified study app for solo reading sessions and friend lobbies.

## Features

- Email/password, Google, and guest sign-in with unique usernames
- Home stats, public lobbies, and six-character private/public study rooms
- Solo study sessions from one PDF or TXT material, with timed reading and recall checkpoints
- AI-assisted recall estimates, segment scoring, rank progress, and recent session history
- Friend requests, read-only profiles, and an XP leaderboard
- Firebase-backed repositories with in-memory development fakes when Firebase/backend config is absent

## Stack

- Kotlin, Jetpack Compose, Material 3, MVVM, StateFlow, repositories, and a small `AppContainer`
- Firebase Authentication and Cloud Firestore for accounts, rooms, friends, and saved stats
- Android `PdfRenderer` for PDFs and app-private local material/session storage
- Python, FastAPI, pypdf, Firebase Admin token verification, and Groq for recall grading

## Android setup

The app uses in-memory repositories and a clearly marked fake recall grader when Firebase config or backend URL is missing.

1. Create a Firebase project and register an Android app with package `com.topdawg.focusmaxxing`.
2. Add your debug signing SHA-1 from `./gradlew signingReport` in Firebase project settings.
3. In **Authentication → Sign-in method**, enable **Email/Password**, **Google**, and **Anonymous**.
4. Create a Native mode Firestore database and publish the root `firestore.rules` contents, or deploy with the Firebase CLI.
5. Download Android config to `app/google-services.json` (ignored by Git).
6. Add the Google provider Web client ID and, when using the backend, the base URL to ignored `local.properties`:

```properties
FOCUSMAXXING_GOOGLE_WEB_CLIENT_ID=your-web-client-id.apps.googleusercontent.com
FOCUSMAXXING_BACKEND_URL=https://your-service.up.railway.app
```

Keep existing `sdk.dir` in `local.properties`. The Google Services plugin activates only when `app/google-services.json` exists. Use Android Studio's JDK 17 or 21 with the pinned Kotlin 2.0.21 toolchain.

## Recall backend

See [backend/README.md](backend/README.md) for local setup, environment variables, Firebase Admin configuration, and Railway deployment. The client computes points for now; **TODO: move scoring and XP/rank updates server-side before using public rankings**, since client-side scoring can be tampered with.

## Scoring and data notes

Tunable Android scoring values live in `SoloConstants.kt`. Segment scores are estimates, not an exact measurement of study quality. Material text on the backend is held in memory for three hours and uploaded again by Android after a backend restart. Never commit Firebase configuration, service-account keys, `.env`, or other secrets.
