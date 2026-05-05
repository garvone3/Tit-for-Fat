# WeightTracker — Android App

A polished personal weight tracking app built with Kotlin, Jetpack Compose, Room (KSP), and MPAndroidChart.

---

## File Placement Guide

Place every file in your Android Studio project exactly as shown:

```
WeightTracker/                            ← project root
├── build.gradle.kts                      ← PROJECT-level gradle (replace existing)
├── settings.gradle.kts                   ← replace existing (adds JitPack repo)
└── app/
    ├── build.gradle.kts                  ← APP-level gradle (replace existing)
    └── src/
        └── main/
            ├── AndroidManifest.xml       ← replace existing
            ├── res/
            │   └── values/
            │       ├── strings.xml
            │       └── themes.xml
            └── java/com/weighttracker/
                ├── MainActivity.kt
                ├── data/
                │   ├── WeightEntry.kt
                │   ├── WeightDao.kt
                │   └── AppDatabase.kt
                ├── util/
                │   └── DataStoreExt.kt
                ├── viewmodel/
                │   └── WeightViewModel.kt
                └── ui/
                    ├── theme/
                    │   └── Theme.kt
                    ├── components/
                    │   └── WeightInputDialog.kt
                    └── screens/
                        ├── DashboardScreen.kt
                        └── GraphScreen.kt
```

---

## Setup Steps

### 1. Create a new project
- Open Android Studio → New Project → **Empty Activity**
- Language: **Kotlin**
- Min SDK: **26**
- Package name: `com.weighttracker`

### 2. Replace build files
Copy `settings.gradle.kts`, `build.gradle.kts` (project), and `app/build.gradle.kts`
into the correct locations. The key additions are:
- KSP plugin (`com.google.devtools.ksp`)
- Room + KSP compiler
- MPAndroidChart via JitPack
- DataStore, Navigation, ViewModel

### 3. Create the package structure
In Android Studio's Project view, create these packages under `com.weighttracker`:
- `data`
- `util`
- `viewmodel`
- `ui/theme`
- `ui/screens`
- `ui/components`

### 4. Copy all `.kt` files into their respective packages

### 5. Sync Gradle → Run

---

## Features

| Feature | Details |
|---------|---------|
| **Weight logging** | Tap ＋ FAB, enter weight (kg), optional height update |
| **BMI** | Auto-calculated from weight + height; colour-coded category bar |
| **Trends** | Weekly and monthly delta shown on dashboard |
| **Insights** | Rate of change (kg/week, kg/month), ETA to target, spike detection |
| **Target weight** | Set via Settings ⚙ icon; progress bar + ETA displayed |
| **Graph** | MPAndroidChart with smooth cubic-bezier line + gradient fill |
| **Filters** | 7 days / 30 days / This week / By week / By month / All time |
| **Aggregation** | Week-by-week and Month-by-month views average data automatically |
| **Storage** | 100% local — Room database + DataStore preferences |
| **Theme** | Dark-only, modern palette, custom typography |

---

## Key Technical Choices

- **KSP** (not kapt) for Room — faster build times
- **Coroutines + Dispatchers.IO** for all DB writes
- **StateFlow** + `collectAsStateWithLifecycle` for reactive UI
- **AndroidView** wrapper for MPAndroidChart inside Compose
- **DataStore Preferences** for height and target weight persistence
- **Single ViewModel** shared across screens via `viewModel()`

---

## Troubleshooting

**Build error: `Unresolved reference: dataStore`**  
→ Make sure `DataStoreExt.kt` is in `com.weighttracker.util` and the import
  `import com.weighttracker.util.dataStore` is present in `WeightViewModel.kt`.

**MPAndroidChart not found**  
→ Verify `maven { url = uri("https://jitpack.io") }` is in `settings.gradle.kts`
  under `dependencyResolutionManagement.repositories`, then **Sync Project**.

**Room compile error**  
→ Confirm `ksp("androidx.room:room-compiler:2.6.1")` (not `kapt`) is in
  `app/build.gradle.kts` and the KSP plugin is applied.

**`Theme.WeightTracker` not found**  
→ Make sure `app/src/main/res/values/themes.xml` exists with the style defined.
