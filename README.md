# Sum Ten Board 🧩

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API%2024%2B-green.svg?style=flat&logo=android)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)]()

> **"Find the pair. Make ten. Clear the board."**

**Sum Ten Board** is a minimalist, additive number-puzzle game for Android built with modern **Jetpack Compose** and **Material 3**. The goal is simple yet challenging: match pairs of numbers on a 6×6 grid that add up to 10 and clear all tiles from the board!

---

## 🌟 Key Features

- **🎮 Addictive Gameplay**
  - **6×6 Grid (36 Tiles)** filled with digits from 1 to 9.
  - **Target Sum = 10**: Pair digits like 1+9, 2+8, 3+7, 4+6, or 5+5.
  - **Reshuffle System**: Use up to 3 reshuffles per game to rearrange remaining active tiles when stuck.
  - **No-Moves Detection**: Automatic recognition when no possible sum-10 pairs remain on the board.

- **🏆 Dynamic Scoring System**
  - **Base Score**: 10 points per pair.
  - **Efficiency Bonus**: Earn +2 bonus points per pair for consecutive successful matches without errors.
  - **No-Reshuffle Bonus**: Earn +1 bonus point per pair if no reshuffles were used.
  - **Perfect Clears**: Complete the board with zero mistakes and zero reshuffles.

- **🎨 Modern UI & Personalization**
  - Built 100% with **Jetpack Compose** & **Material 3**.
  - Full **Dark Mode** & **Light Mode** support.
  - **Dynamic Color** themes for devices running Android 12+ (API 31+).
  - Smooth tile selection, removal, and reshuffle animations.

- **🔊 Rich Feedback & Accessibility**
  - **Sound Effects & Haptics**: Tactile vibration and audio feedback for successful matches, invalid selections, and reshuffles.
  - **Match Highlights / Hints**: Optional visual indication highlighting valid target matches for selected numbers.
  - **Grid Coordinates**: Optional spreadsheet-style labels (`A1`–`F6`) for easier board navigation and screen reader accessibility.
  - **TalkBack & Semantics**: Accessible node descriptions and merged heading semantics.

- **📊 Persistence & Detailed Statistics**
  - Local preferences and statistics storage.
  - Track total games played, boards cleared, best score, best accuracy %, total pairs removed, and perfect clears.

---

## 🎮 How to Play

1. **Tap any active number tile** to select it.
2. **Find a second tile** whose value adds up to 10 with the first tile (e.g., `4` and `6`).
3. If valid, both tiles dissolve from the board and you earn points!
4. If invalid, the selection resets and counts as a failed attempt.
5. **Use Reshuffle** when you need a fresh layout of the remaining numbers.
6. **Goal**: Clear all 36 tiles off the board to win!

---

## 🛠️ Tech Stack & Architecture

- **Language**: [Kotlin](https://kotlinlang.org)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material 3](https://m3.material.io/)
- **Navigation**: [Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
- **Architecture**:
  - **Unidirectional Data Flow (UDF)** with `ViewModel` and `StateFlow`.
  - **Pure Game Engine (`SumTenGameEngine`)**: Pure, framework-free domain logic decoupled from Android UI classes.
  - **Repository/Store Layer**: `KeyValueStore`, `SettingsStore`, `StatisticsStore` for local persistence.
- **Asynchronous Flow**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & `StateFlow` / `SharedFlow`
- **Testing**:
  - `JUnit 4` & `kotlinx-coroutines-test` for pure unit tests (Game Engine, Board Generation, Reshuffling, Persistence).
  - `Compose UI Test` & `Espresso` for instrumentation and accessibility verification.

---

## 📂 Project Structure

```text
SumTenBoard/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/sumtenboard/
│   │   │   │   ├── data/          # KeyValueStore, SettingsStore, StatisticsStore
│   │   │   │   ├── game/          # SumTenViewModel, Pure Game Engine & Board Generator
│   │   │   │   │   ├── logic/     # SumTenGameEngine, SumTenBoardGenerator
│   │   │   │   │   └── model/     # GameModels (State, Coordinates, CellState)
│   │   │   │   ├── navigation/    # AppNavigation, Routes, Bottom Bar
│   │   │   │   ├── ui/            # Screens, Components, Theme
│   │   │   │   │   ├── components/# NumberBoard, GamePanels, HowToPlaySheet
│   │   │   │   │   ├── screens/   # Splash, Home, Game, Result, Settings, About
│   │   │   │   │   └── theme/     # Color, Theme, Type
│   │   │   │   └── util/          # Feedback (Sound, Haptics), Accessibility
│   │   │   ├── res/               # Resources (Drawables, Values, Mipmaps)
│   │   │   └── AndroidManifest.xml
│   │   └── test/                  # Comprehensive Unit Test Suite
│   └── build.gradle.kts
├── build.gradle.kts
├── gradle.properties
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio**: Ladybug (2024.2.1) or newer recommended.
- **JDK**: Java 11 or higher.
- **Android SDK**: Compile SDK 35 (or higher), Target SDK 35, Min SDK 24 (Android 7.0 Nougat).

### Build & Run

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/abdullah-ateeq/Sum-Ten-Board.git
   cd Sum-Ten-Board
   ```

2. **Open in Android Studio**:
   Open the project directory in Android Studio and let Gradle sync.

3. **Run the App**:
   Select an emulator or connected physical Android device (API 24+) and click **Run** (`Shift + F10`).

4. **Run Tests**:
   Execute unit tests via Gradle:
   ```bash
   ./gradlew test
   ```

---

## 🧪 Testing Strategy

The project features high unit test coverage on core business logic:
- **`EngineRulesTest`**: Validates pair matching (sums to 10), scoring formulas, and game ending conditions.
- **`EngineSelectionTest`**: Validates tile selection, state transitions, failed attempts, and board resolution.
- **`BoardGeneratorTest`**: Guarantees boards are generated with solvable sum-10 pairs.
- **`ReshuffleTest`**: Verifies coordinate shifting without changing tile availability.
- **`SumTenViewModelTest`**: Verifies reactive state flow and game event emissions.
- **`PersistenceTest`**: Validates saving and restoring settings and statistics.

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
