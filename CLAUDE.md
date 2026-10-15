# HawkerApp

## Overview
Two-sided marketplace Android app connecting street vendors (Hawkers) with customers. Hawkers register, manage inventory, and receive customer requests. Customers browse nearby hawkers on a map, search items, and send service requests.

## Tech Stack
- **Language:** Kotlin 1.9.0
- **Architecture:** MVVM with Repository pattern
- **Min SDK:** 24, **Target/Compile SDK:** 34
- **Networking:** Retrofit 2.9.0 + Gson
- **Database:** Room 2.6.1
- **Maps:** Google Maps + Mapbox 11.2.0
- **Auth:** OTP-based phone authentication
- **Push Notifications:** Firebase Cloud Messaging (FCM)
- **Image Loading:** Glide 4.16.0
- **DI:** Manual (no Hilt/Dagger)

## Build
```bash
# Requires JDK 17 (JDK 25 is incompatible with Gradle 8.5)
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.20.1/libexec/openjdk.jdk/Contents/Home
./gradlew assembleDebug
```

## Required Config Files (not in git)
- `app/google-services.json` — Firebase config (download from Firebase Console)
- `secrets.properties` — API keys and backend credentials:
  ```
  BASE_URL=<backend_url>
  API_USERNAME=<basic_auth_username>
  API_PASSWORD=<basic_auth_password>
  MAP_API_KEY=<google_maps_api_key>
  ```

## Project Structure
```
app/src/main/java/com/hawkerapp/app/
├── views/          # Activities & Fragments (UI layer)
├── viewmodels/     # ViewModels (MVVM)
├── models/         # Data classes (HawkerInfo, UserData, Item, etc.)
├── network/        # Retrofit API interface + RetrofitHelper
├── database/       # Room DB, DAOs, Converters
├── repositories/   # Data repositories
├── managers/       # Business logic (HawkerManager)
├── adapters/       # RecyclerView adapters
├── utils/          # Location, permissions, notifications, map utils
├── store/          # SessionManager (SharedPreferences)
├── MyFirebaseMessagingService.kt
└── SMSReceiver.kt
```

## Key Screens
- **SplashActivity** — Entry point, routes to hawker or user flow
- **HawkerOtpActivity** — Phone OTP authentication
- **HawkerFormActivity** — Multi-step hawker registration (2 fragments)
- **HawkerViewActivity** — Hawker dashboard with map showing customer requests
- **UserViewActivity** — Customer map view with nearby hawker discovery
- **ProfileViewActivity** — Hawker profile editing
- **ManageItemsActivity** — Hawker inventory CRUD

## Known Issues
- Hardcoded Basic Auth credentials in `HawkersAPI.kt` headers — should use BuildConfig values
- `cleartextTrafficPermitted="true"` in network security config — restrict for production
- Glide annotationProcessor should be `kapt` instead
- `HawkerManager.deleteItem()` is declared but not implemented
- No unit or UI tests
- Proguard/R8 minification disabled for release builds
