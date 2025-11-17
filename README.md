# Idle Loadshedding

This document provides a comprehensive overview of the Idle Loadshedding project, a mobile game developed with Unity and supported by a .NET backend and Azure SQL for data persistence.

## Introduction

Idle Loadshedding is an interactive mobile game designed to simulate responses to power outages in a fun and engaging way. The project integrates a Unity-based frontend with a robust .NET backend, utilizing Azure SQL for cloud data storage. This README outlines the key features, architecture, and setup of the application.

## Feature 1: Unity game interface

The frontend of the game is built using Unity, offering a rich and interactive user experience. The interface is designed to be intuitive and visually appealing, allowing players to easily navigate through the game's features.

### Built with Unity's 3D capabilities:

- Camera Controls: Smooth pan and zoom with touch, edge panning for mobile
- Clickable Objects: Interactive generators and buildings with visual feedback
- Light System: Dynamic lighting that responds to touch
- Visual Effects: Animations and material changes for state transitions

<img width="506" height="1145" alt="image" src="https://github.com/user-attachments/assets/27d20d26-32a3-4c2a-9865-16ee9a3ddc8d" />


## Feature 2: Leaderboard System

A competitive leaderboard system is implemented to enhance player engagement. The system ranks players based on their scores, which are submitted to the backend. The backend processes these scores and serves the leaderboard data to the game client.

<img width="509" height="1144" alt="image" src="https://github.com/user-attachments/assets/8582fa07-3db9-4678-9fbe-eedb2c032b75" />


## Feature 3: Cloud Save Functionality with Azure SQL

Player progress, including game state, statistics, and achievements, is securely saved to an Azure SQL database. This ensures data persistence across different devices and game sessions, providing a seamless experience for the user.

<img width="535" height="390" alt="image" src="https://github.com/user-attachments/assets/e4b99401-df25-4504-9777-33bfaeea4972" />


## Proof of Azure Services

For development and cost-saving purposes, the project currently runs in a local environment using Docker containers. This setup mimics the cloud infrastructure and ensures that the application is ready for deployment to Azure. The `docker-compose.acr.yml` file is configured for a future deployment to Azure Container Registry and other Azure services for the final project submission.

<img width="2055" height="494" alt="image" src="https://github.com/user-attachments/assets/935ef629-396f-46ba-b684-6573b4e05a92" />

<img width="2189" height="573" alt="image" src="https://github.com/user-attachments/assets/7813ac8e-4650-4c83-bb48-3d6335bd1073" />

<img width="2541" height="1016" alt="image" src="https://github.com/user-attachments/assets/afbfc746-f394-491d-9e48-e244791e852f" />


## Docker container explanation

The project is containerized using Docker to ensure consistency across development, testing, and production environments. The `docker-compose.yml` file orchestrates the local deployment of the backend API and the SQL database.

-   **`IdleLoadsheddingAPI/Dockerfile`**: This file defines the Docker image for the .NET backend API.
-   **`SQL/Dockerfile`**: This file sets up the SQL Server container and initializes the database using the `init-database.sql` script.

<img width="1122" height="94" alt="image" src="https://github.com/user-attachments/assets/b7563545-37df-427d-941a-45d0e6c473d4" />


This containerized approach simplifies the setup process and makes the application easily portable.

## Folder structure

The project is organized into the following main directories:

-   **`IdleLoadsheddingAPI/`**: Contains the source code for the .NET backend service.
-   **`IdleLoadsheddingAPI.Tests/`**: Includes unit and integration tests for the backend API.
-   **`MobileLoadshedding/`**: The Unity project for the mobile application, structured as a Gradle project.
-   **`SQL/`**: Contains the Docker configuration and initialization scripts for the SQL Server database.

# Unity Game Systems - In-Depth Documentation
This section provides comprehensive documentation for the Unity game systems implemented in Idle Loadshedding.

## Architecture Overview

Manager-Based Architecture

The game uses a singleton manager pattern with carefully ordered initialization to ensure dependencies are available when needed.

### Managers

- IdleStatsManager
- IdleCurrencyManager
- IdleSectionManager
- IdleEventManager
- IdleSaveManager

### Game Loop

Player Action → Manager Processing → State Update → UI Refresh → Save Sync

## Core Manager Systems

<img width="406" height="233" alt="image" src="https://github.com/user-attachments/assets/16321cd8-97bd-4b48-887a-9258e8f2d7de" />

### 1. IdleStatsManager

Purpose: Central hub for all energy-related statistics and production calculations.

Location: Assets/Scripts/Core/IdleStatsManager.cs
#### Key Responsibilities

- Track current energy and total energy generated
- Manage energy production from all generators
- Apply event multipliers to production rates
- Handle manual energy generation (tap mechanic)
- Calculate offline progress
- Sync stats to Android every 5 seconds
- Auto-save state on pause/quit

### 2. IdleEventManager

Purpose: Manages random gameplay events that modify production rates.

Location: Assets/Scripts/Core/IdleEventManager.cs
#### Key Responsibilities

- Schedule random events at intervals
- Trigger timed events (load shedding, power surges)
- Apply production multipliers
- Notify Android of event state changes
- Track active event duration

### 3. IdleCurrencyManager

Purpose: Manages the in-game money economy.

Location: Assets/Scripts/Core/IdleCurrencyManager.cs
#### Key Responsibilities

- Track current money balance
- Track total money earned (lifetime)
- Validate spending transactions
- Persist currency state
- Integrate with save system

### 4. IdleSectionManager
Purpose: Manages all city sections (districts) in the game.

Location: Assets/Scripts/Core/IdleSectionManager.cs
#### Key Responsibilities

- Track all sections in the game
- Auto-discover sections in scene
- Organize sections by tier
- Coordinate section saves/loads
- Notify when sections change state

### 5. IdleSaveManager
Purpose: Coordinates all save/load operations across the game.

Location: Assets/Scripts/Core/IdleSaveManager.cs
#### Key Responsibilities

- Combine data from all managers
- Save to local PlayerPrefs
- Sync to Android every 30 seconds
- Handle generator states
- Auto-save on pause/quit
- Load saved state on startup

## Gameplay Components

<img width="398" height="325" alt="image" src="https://github.com/user-attachments/assets/382fbe4e-2523-452c-93db-6d2484fef9fa" />

### ResourceGenerator
Purpose: Individual energy generators (solar panels, wind turbines, etc.)

Location: Assets/Scripts/Gameplay/ResourceGenerator.cs
#### Key Features

- Configurable production rates
- Upgrade system with exponential costs
- Auto-registers with IdleStatsManager
- Optional tap-for-energy mechanic
- Max level system

### SectionData
Purpose: Data container for city section configuration.

Location: Assets/Scripts/Gameplay/SectionData.cs

### SectionController
Purpose: Controls individual section behavior and production.

Location: Assets/Scripts/Gameplay/SectionController.cs
#### Key Responsibilities

- Execute production cycle (every second)
- Consume energy, produce money
- Handle unlock logic
- Handle upgrade logic
- Auto-register with IdleSectionManager

## UI Systems (Hidden on the android side as this is used to ensure data is being read correctly)

<img width="298" height="440" alt="image" src="https://github.com/user-attachments/assets/efa84fcf-6fb8-4eda-a934-be3a42bdbb75" />

### StatsDisplayUI

<img width="421" height="933" alt="image" src="https://github.com/user-attachments/assets/2a4a0434-957b-4160-81b4-06274b867eb6" />

Purpose: Real-time display of all game statistics.

Location: Assets/Scripts/UI/StatsDisplayUI.cs
Displayed Information
#### Energy Stats:

- Current Energy
- Total Energy Generated
- Energy Per Second

#### Currency Stats:

- Current Money
- Total Money Earned

#### Generator Stats:

- Active Generator Count
- Total Generator Levels

#### Section Stats:

- Active Section Count
- Total Section Levels
- Money Per Second

#### Event Stats:

- Production Multiplier
- Active Event Status

### SectionUIButton

Purpose: Interactive UI button for section unlock/upgrade.

Location: Assets/Scripts/UI/SectionUIButton.cs
#### Key Features

- Auto-finds linked section by ID
- Shows current level and max level
- Displays unlock cost or upgrade cost
- Shows production and energy consumption
- Color-coded affordability
- Auto-disables when maxed or unaffordable

### EnergyTapButton
Purpose: Manual energy generation button with visual feedback.

Location: Assets/Scripts/UI/EnergyTapButton.cs
#### Key Features

- Adds burst energy on tap
- Scale punch animation
- Configurable energy amount
- Integrates with IdleStatsManager

## Input & Interaction

### CameraController

https://github.com/user-attachments/assets/7201ff1b-3a8a-489f-a765-b847d29c5bf6

Purpose: Mobile-friendly camera controls for scene navigation.

Location: Assets/Scripts/Input/CameraController.cs
### Features
#### Touch/Mouse Controls:

- Drag to pan camera
- Edge panning on mobile

#### Boundary System:

- Configurable min/max bounds
- Smooth clamping

#### Performance:

- Target frame rate for mobile
- Periodic boundary checks
- Efficient input handling

### ClickDetector
Purpose: Handles all click/tap detection with drag prevention.

Location: Assets/Scripts/Input/ClickDetector.cs
#### Features

- Drag Detection: Prevents accidental clicks during camera pan
- Time Validation: Ignores long presses
- Distance Threshold: Configurable drag sensitivity
- Layer Filtering: Click only on specific layers
- IClickable Interface: Standard interaction protocol

### ClickableObject
Purpose: Base implementation of IClickable for simple objects.

Location: Assets/Scripts/Input/ClickableObject.cs
#### Features

- Toggle state (on/off)
- Material swapping
- Color changes
- Audio feedback
- Virtual OnClickCustomBehavior for extensions

## Conclusion

The Idle Loadshedding project is a full-stack application that demonstrates the integration of a Unity game with a .NET backend and a SQL database. The use of Docker for containerization facilitates a smooth development workflow and prepares the application for a scalable cloud deployment on Azure.

### LightController

Purpose: Advanced light control system for buildings/districts.

Location: Assets/Scripts/Input/LightController.cs
#### Features

- Multi-Light Control: Control arrays of light objects
- Material Swapping: Change materials on/off
- Emission Control: Adjust emission intensity
- Animation Support: Animate light transitions
- Switch Visibility: Optional visible/hidden switch
- Auto-Discovery: Find lights by name/tag

##Checkins for version 1 
<img width="721" height="289" alt="image" src="https://github.com/user-attachments/assets/098748c2-0a4a-4e39-b501-9d7303903ebd" />

## Git pushes for Version 2
<img width="1281" height="710" alt="image" src="https://github.com/user-attachments/assets/7c54b39d-504e-4332-9bdf-b722ed5715a3" />

## Contributors

- Caleb (ST10275378)
- Logan (ST10268888)
- Kyle (ST10085208)
- Michael (ST10085212)

## Reference list:

Android (2025) Android Developers - Activity. [online] Available at: https://developer.android.com/reference/android/app/Activity
 [Accessed 30 Sept. 2025].

Android (2025) Android Developers - BuildConfig. [online] Available at: https://developer.android.com/reference/tools/gradle-api/com/android/build/api/variant/BuildConfigField
 [Accessed 2 Oct. 2025].

Android (2025) Android Developers Documentation. [online] Available at: https://developer.android.com/
 [Accessed 29 Sept. 2025].

Android (2025) Android Developers - Intent. [online] Available at: https://developer.android.com/reference/android/content/Intent
 [Accessed 28 Sept. 2025].

Android (2025) Android Developers - Log. [online] Available at: https://developer.android.com/reference/android/util/Log
 [Accessed 3 Oct. 2025].

Android (2025) Credential Manager. [online] Available at: https://developer.android.com/jetpack/androidx/releases/credentials
 [Accessed 1 Oct. 2025].

AndroidX (2025) AndroidX AppCompat Library. [online] Available at: https://developer.android.com/jetpack/androidx/releases/appcompat
 [Accessed 30 Sept. 2025].

AndroidX (2025) AndroidX Core Library. [online] Available at: https://developer.android.com/jetpack/androidx/releases/core
 [Accessed 28 Sept. 2025].

AndroidX (2025) AndroidX Navigation Component. [online] Available at: https://developer.android.com/guide/navigation
 [Accessed 2 Oct. 2025].

Coding-Meet (2023). Google-Admob-Integration-App. [online] GitHub. Available at: https://github.com/Coding-Meet/Google-Admob-Integration-App.git 
 [Accessed 25 Oct. 2023].

Firebase (2025) Authenticate with Google on Android. [online] Available at: https://firebase.google.com/docs/auth/android/google-signin
 [Accessed 29 Sept. 2025].

Firebase (2025) Firebase Authentication for Android. [online] Available at: https://firebase.google.com/docs/auth/android/start
 [Accessed 3 Oct. 2025].

Google (2025) Authenticate with Firebase using ID tokens. [online] Available at: https://developers.google.com/identity/gsi/web/guides/firebase-auth
 [Accessed 25 Sept. 2025].

Google (2025). Get Started | Android - AdMob. [online] Available at: https://developers.google.com/admob/android/quick-start
[Accessed 15 Nov. 2025].

Google (2025). googleads-mobile-android-examples. [online] GitHub. Available at: https://github.com/googleads/googleads-mobile-android-examples.kt
[Accessed 15 Nov. 2025].

Google (2025) Gson - A Java serialization library. [online] Available at: https://github.com/google/gson
 [Accessed 30 Sept. 2025].

Google (2025). Rewarded Ads | Android - AdMob. [online] Available at: https://developers.google.com/admob/android/rewarded#kotlin_1
[Accessed 15 Nov. 2025].

Google (2025) Sign in with Google for Android - Credential Manager. [online] Available at: https://developers.google.com/identity/android-credential-manager
 [Accessed 1 Oct. 2025].

hall9zeha (2022). AdvertisementsApp. [online] GitHub. Available at: https://github.com/hall9zeha/AdvertisementsApp.git 
 [Accessed 15 Nov. 2025].


Kotlin (2025) Kotlin Coroutines Guide. [online] Available at: https://kotlinlang.org/docs/coroutines-guide.html
 [Accessed 28 Sept. 2025].

Kotlin (2025) Kotlin Documentation - Java Interop. [online] Available at: https://kotlinlang.org/docs/java-interop.html
 [Accessed 29 Sept. 2025].

Kotlin (2025) Kotlin Language Documentation - Delegated Properties. [online] Available at: https://kotlinlang.org/docs/delegated-properties.html
 [Accessed 3 Oct. 2025].

Kotlin (2025) Kotlin Language Documentation - Inline Functions. [online] Available at: https://kotlinlang.org/docs/inline-functions.html
 [Accessed 2 Oct. 2025].

Kotlin (2025) Kotlin Standard Library. [online] Available at: https://kotlinlang.org/api/latest/jvm/stdlib/
 [Accessed 1 Oct. 2025].

Material Design (2025) Material Components for Android. [online] Available at: https://material.io/develop/android
 [Accessed 30 Sept. 2025].

Microsoft, 2025. ASP.NET Core Documentation. [online] Microsoft Learn. Available at: https://learn.microsoft.com/en-us/aspnet/core/
 [Accessed 5 Oct. 2025].

Microsoft, 2025. C# Language Documentation. [online] Microsoft Learn. Available at: https://learn.microsoft.com/en-us/dotnet/csharp/
 [Accessed 5 Oct. 2025].

Docker Inc., 2025. Docker Documentation - Get Started. [online] Available at: https://docs.docker.com/get-started/
 [Accessed 5 Oct. 2025].

Microsoft, 2025. Entity Framework Core Documentation. [online] Microsoft Learn. Available at: https://learn.microsoft.com/en-us/ef/core/
 [Accessed 5 Oct. 2025].

Google, 2025. Firebase Authentication Documentation. [online] Google Firebase. Available at: https://firebase.google.com/docs/auth
 [Accessed 5 Oct. 2025].

Microsoft, 2025. ASP.NET Core Authentication - JWT Bearer. [online] Microsoft Learn. Available at: https://learn.microsoft.com/en-us/aspnet/core/security/authentication/
 [Accessed 5 Oct. 2025].

Microsoft, 2025. .NET 9 Documentation. [online] Microsoft Learn. Available at: https://learn.microsoft.com/en-us/dotnet/core/whats-new/dotnet-9/overview
 [Accessed 5 Oct. 2025].

Microsoft, 2025. .NET Documentation. [online] Microsoft Learn. Available at: https://learn.microsoft.com/en-us/dotnet/
 [Accessed 5 Oct. 2025].

Microsoft, 2025. JSON Serialization in .NET. [online] Microsoft Learn. Available at: https://learn.microsoft.com/en-us/dotnet/standard/serialization/system-text-json/
 [Accessed 5 Oct. 2025].

Oracle (2025) Java Platform Standard Edition Documentation. [online] Available at: https://docs.oracle.com/en/java/javase/
 [Accessed 28 Sept. 2025].

Square (2025) OkHttp - An HTTP client for Android and Java. [online] Available at: https://square.github.io/okhttp/
 [Accessed 2 Oct. 2025].

Square (2025) Retrofit - Type-safe HTTP client for Android and Java. [online] Available at: https://square.github.io/retrofit/
 [Accessed 29 Sept. 2025].

Unity Technologies (2025) Best Practice - Understanding Performance in Unity. [online] Unity Manual. Available at: https://docs.unity3d.com/Manual/BestPracticeUnderstandingPerformanceInUnity.html
 [Accessed 7 Oct. 2025].

Unity Technologies (2025) JsonUtility. [online] Unity Script Reference. Available at: https://docs.unity3d.com/ScriptReference/JsonUtility.html
 [Accessed 7 Oct. 2025].

Unity Technologies (2025) Mobile Optimization - Practical Guide. [online] Unity Manual. Available at: https://docs.unity3d.com/Manual/MobileOptimizationPracticalGuide.html
 [Accessed 7 Oct. 2025].

Unity Technologies (2025) PlayerPrefs. [online] Unity Script Reference. Available at: https://docs.unity3d.com/ScriptReference/PlayerPrefs.html
 [Accessed 7 Oct. 2025].

Unity Technologies (2025) TextMesh Pro. [online] Unity Manual. Available at: https://docs.unity3d.com/Manual/com.unity.textmeshpro.html
 [Accessed 7 Oct. 2025].

Unity Technologies (2025) Unity for Android. [online] Unity Manual. Available at: https://docs.unity3d.com/Manual/android.html
 [Accessed 30 Sept. 2025].

Unity Technologies (2025) Unity for Android - Native Plugin Integration. [online] Unity Manual. Available at: https://docs.unity3d.com/Manual/android-plugins.html
 [Accessed 3 Oct. 2025].

Unity Technologies (2025) Unity Input System. [online] Unity Package Documentation. Available at: https://docs.unity3d.com/Packages/com.unity.inputsystem@1.5
 [Accessed 7 Oct. 2025].
