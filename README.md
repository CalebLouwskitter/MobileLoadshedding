# Idle Loadshedding

This document provides a comprehensive overview of the Idle Loadshedding project, a mobile game developed with Unity and supported by a .NET backend and Azure SQL for data persistence.

## Introduction

Idle Loadshedding is an interactive mobile game designed to simulate responses to power outages in a fun and engaging way. The project integrates a Unity-based frontend with a robust .NET backend, utilizing Azure SQL for cloud data storage. This README outlines the key features, architecture, and setup of the application.

## Feature 1: Unity game interface

The frontend of the game is built using Unity, offering a rich and interactive user experience. The interface is designed to be intuitive and visually appealing, allowing players to easily navigate through the game's features.


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

## Conclusion

The Idle Loadshedding project is a full-stack application that demonstrates the integration of a Unity game with a .NET backend and a SQL database. The use of Docker for containerization facilitates a smooth development workflow and prepares the application for a scalable cloud deployment on Azure.

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

Firebase (2025) Authenticate with Google on Android. [online] Available at: https://firebase.google.com/docs/auth/android/google-signin
 [Accessed 29 Sept. 2025].

Firebase (2025) Firebase Authentication for Android. [online] Available at: https://firebase.google.com/docs/auth/android/start
 [Accessed 3 Oct. 2025].

Google (2025) Authenticate with Firebase using ID tokens. [online] Available at: https://developers.google.com/identity/gsi/web/guides/firebase-auth
 [Accessed 25 Sept. 2025].

Google (2025) Gson - A Java serialization library. [online] Available at: https://github.com/google/gson
 [Accessed 30 Sept. 2025].

Google (2025) Sign in with Google for Android - Credential Manager. [online] Available at: https://developers.google.com/identity/android-credential-manager
 [Accessed 1 Oct. 2025].

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

Unity Technologies (2025) Unity for Android. [online] Available at: https://docs.unity3d.com/Manual/android.html
 [Accessed 30 Sept. 2025].

Unity Technologies (2025) Unity for Android - Native Plugin Integration. [online] Available at: https://docs.unity3d.com/Manual/android-plugins.html
 [Accessed 3 Oct. 2025].
