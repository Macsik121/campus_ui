# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**CAMPUS** — Android app for managing pedagogical squads (отряды) in a camp setting. Built with Java + XML, following MVVM pattern with Repository layer. Uses OpenAPI Generator (swagger.yaml) to generate API client code.

**Package:** `com.sfedu.campus`  
**Min SDK:** 24 | **Target SDK:** 35 | **Compile SDK:** 36  
**Gradle:** Kotlin DSL (`.kts`) with Version Catalog (`gradle/libs.versions.toml`)

## Key Commands

| Task | Command |
|------|---------|
| Build debug APK | `./gradlew assembleDebug` |
| Run unit tests | `./gradlew test` |
| Run instrumented tests | `./gradlew connectedAndroidTest` |
| Generate API code from swagger | `./gradlew openApiGenerate` (auto-runs on `preBuild`) |
| Lint | `./gradlew lint` |
| Clean build | `./gradlew clean` |

## Architecture

Read ARCHITECTURE.md

### Module Structure
```
app/
├── src/main/java/com/sfedu/campus/
│   ├── auth/           # Login/Register fragments + ViewModels
│   ├── main/           # MainActivity (BottomNavigation host)
│   ├── map/            # MapFragment (placeholder)
│   ├── squad/          # SquadFragment + ChildAdapter + Repositories
│   ├── squad_log/      # SquadLogFragment (placeholder)
│   ├── notifications/  # NotificationFragment + Adapter
│   ├── profile/        # ProfileFragment + Repository
│   ├── helpers/        # ApiClient, ApiProvider, NavigationHelper, PreferencesHelper, ViewUtils
│   ├── data/datasource/ # DataCallback interface
│   └── models/         # Data models + server request/response DTOs
├── src/main/res/       # Layouts, drawables, menus, values
├── build.gradle.kts    # App-level build with OpenAPI Generator config
└── proguard-rules.pro
```

### Core Patterns

**Authentication Flow:**
- `AuthActivity` hosts `LoginFragment` / `RegisterFragment`
- Token stored in `SharedPreferences` via `PreferencesHelper`
- `ApiProvider` creates singleton `ApiClient` with Bearer token interceptor
- `MainActivity` checks token on launch, redirects to `AuthActivity` if missing

**Fragment Navigation (MainActivity):**
- Single Activity with `BottomNavigationView` + `FrameLayout` container
- Fragments cached in `HashMap<Integer, Fragment>` — hidden/shown, not replaced
- Default tab: Squad (`R.id.nav_squad`)

**Repository Pattern:**
- Each feature has a Repository (`UserRepository`, `SquadRepository`, `ProfileRepository`)
- Repositories use generated OpenAPI client (`ApiProvider.getApiClient(context)`)
- Async via `new Thread(() -> { ... }).start()` with `DataCallback<T>` interface

**API Generation:**
- `swagger.yaml` defines the full REST API
- `openApiGenerate` task runs before `preBuild`
- Generated code lands in `build/generated/src/main/java/com/sfedu/campus/generated/`
- Packages: `api`, `invoker`, `model`
- Uses `okhttp-gson` library

## Important Classes

| Class | Purpose |
|-------|---------|
| `ApiProvider` | Singleton OkHttpClient with JWT interceptor + logging |
| `PreferencesHelper` | SharedPreferences wrapper (token, squad cache, children cache, JWT payload decode) |
| `NavigationHelper` | Static helpers for Activity navigation with CLEAR_TASK flags |
| `ViewUtils` | UI utilities: toast (Snackbar), dp→px, TextInputLayout binding to ViewModels |
| `ChildAdapter` | RecyclerView.Adapter with DiffUtil + search filtering + Chip tags |
| `SquadFragment` | Main squad view: loads squad → children (cache + API), search, edit notes dialog |

## Key Implementation Details

### Token Handling
- `PreferencesHelper.saveToken()/getToken()/isTokenSet()`
- `ApiProvider` interceptor reads token fresh on each request
- `ApiClient` (legacy) also exists but `ApiProvider` is preferred for generated API

### Squad/Children Loading (`SquadFragment`)
1. `UserRepository.getUserSquad()` → gets `squadId`
2. If cache exists: render immediately, then `fetchChildrenFromApi(hasCache=true)`
3. If no cache: `fetchChildrenFromApi(hasCache=false)` with loading overlay
4. `DiffUtil` in `ChildAdapter` for efficient list updates
5. Squad title fetched separately via `SquadsApi.getChildrenBySquad()` (returns `GetSquadChildrenResponse` with title)

### Generated Model Usage
- Models in `com.sfedu.campus.generated.model.*` (Child, ChildTag, UserProfile, Notification, Event, etc.)
- Enums use `*Enum` suffix (e.g., `UserProfile.RoleEnum`)
- UUID fields are `String` with `format: "uuid"` in swagger

### UI Conventions
- Material Components (MaterialButton, TextInputLayout, Chip, CircleImageView)
- Snackbar for toasts (with campus logo icon)
- Edge-to-edge with `WindowInsetsCompat` padding
- ViewBinding not used — `findViewById` everywhere

## Common Development Tasks

### Adding a New API Endpoint
1. Update `swagger.yaml`
2. Run `./gradlew openApiGenerate`
3. Use generated classes in `com.sfedu.campus.generated.api.*` and `model.*`
4. Create/update Repository to call the new endpoint

### Adding a New Fragment to Bottom Nav
1. Add menu item to `res/menu/bottom_nav_menu.xml`
2. Add `Fragment` class
3. Add case in `MainActivity.switchFragment()`
4. Add navigation ID to `fragmentMap` logic

### Running Single Test
```bash
# Unit test
./gradlew test --tests "com.sfedu.campus.helpers.PreferencesHelperTest"

# Instrumented test (needs device/emulator)
./gradlew connectedAndroidTest --tests "com.sfedu.campus.ExampleInstrumentedTest"
```

## Known Issues / TODOs

- `ApiClient.isTokenValid()` has a race condition (async call but returns sync boolean) — commented out in favor of `PreferencesHelper.isTokenSet()`
- `MapFragment` and `SquadLogFragment` are placeholders
- Password change in ProfileFragment not implemented
- Base URL hardcoded to `http://localhost:3000/api/v1` in multiple places (`ApiClient`, `ApiProvider`, `ProfileFragment`)

## Testing Notes

- Unit tests: `app/src/test/`
- Instrumented tests: `app/src/androidTest/`
- JUnit 4 + Espresso
- No mocking framework configured (Mockito not in deps)

## Dependencies (from libs.versions.toml)
- AGP 9.0.1
- AndroidX: AppCompat 1.7.1, Material 1.14.0, Activity 1.13.0, ConstraintLayout 2.2.1
- OkHttp 4.10.0 + logging-interceptor
- Gson 2.10.1 + gson-fire 1.8.5
- CircleImageView 3.1.0
- Swagger Core 1.6.9 + javax.ws.rs + javax.annotation
- OpenAPI Generator Gradle Plugin 7.10.0