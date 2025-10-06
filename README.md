# Idle Loadshedding

This document provides a comprehensive overview of the Idle Loadshedding project, a mobile game developed with Unity and supported by a .NET backend and Azure SQL for data persistence.

## Introduction

Idle Loadshedding is an interactive mobile game designed to simulate responses to power outages in a fun and engaging way. The project integrates a Unity-based frontend with a robust .NET backend, utilizing Azure SQL for cloud data storage. This README outlines the key features, architecture, and setup of the application.

## Feature 1: Unity game interface

The frontend of the game is built using Unity, offering a rich and interactive user experience. The interface is designed to be intuitive and visually appealing, allowing players to easily navigate through the game's features.

*You can add a screenshot of your Unity game interface here:*
`![Unity Game Interface](path/to/your/game_interface_screenshot.png)`

## Feature 2: Leaderboard System

A competitive leaderboard system is implemented to enhance player engagement. The system ranks players based on their scores, which are submitted to the backend. The backend processes these scores and serves the leaderboard data to the game client.

*You can add a screenshot of the leaderboard here:*
`![Leaderboard Screenshot](path/to/your/leaderboard_screenshot.png)`

## Feature 3: Cloud Save Functionality with Azure SQL

Player progress, including game state, statistics, and achievements, is securely saved to an Azure SQL database. This ensures data persistence across different devices and game sessions, providing a seamless experience for the user.

## Proof of Azure Services

For development and cost-saving purposes, the project currently runs in a local environment using Docker containers. This setup mimics the cloud infrastructure and ensures that the application is ready for deployment to Azure. The `docker-compose.acr.yml` file is configured for a future deployment to Azure Container Registry and other Azure services for the final project submission.

*You can add a screenshot of your Azure portal showing the planned or provisioned resources here:*
`![Azure Services Proof](path/to/your/azure_portal_screenshot.png)`

## Docker container explanation

The project is containerized using Docker to ensure consistency across development, testing, and production environments. The `docker-compose.yml` file orchestrates the local deployment of the backend API and the SQL database.

-   **`IdleLoadsheddingAPI/Dockerfile`**: This file defines the Docker image for the .NET backend API.
-   **`SQL/Dockerfile`**: This file sets up the SQL Server container and initializes the database using the `init-database.sql` script.

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

*You can add your references here.*
