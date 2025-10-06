# ============================================================================
# DOCKERFILE FOR IDLELOADSHEDDINGAPI
# ============================================================================
# Multi-stage Docker build for .NET 9 application (Docker Inc., 2025; Microsoft, 2025)
# Uses official Microsoft .NET SDK and ASP.NET runtime images (Microsoft, 2025)
# ============================================================================

###########
# Build stage
###########
# (Microsoft, 2025) - Official .NET 9 SDK image for building applications
FROM mcr.microsoft.com/dotnet/sdk:9.0 AS build
WORKDIR /src

# Copy project file and restore dependencies (explicitly target the csproj)
COPY IdleLoadsheddingAPI.csproj ./
RUN dotnet restore IdleLoadsheddingAPI.csproj

# Copy everything else and build only the web api project (avoid solution to skip tests)
COPY . .
RUN dotnet build IdleLoadsheddingAPI.csproj -c Release -o /app/build

############
# Publish stage
############
FROM build AS publish
RUN dotnet publish IdleLoadsheddingAPI.csproj -c Release -o /app/publish

############
# Runtime stage
############
# (Microsoft, 2025) - Official ASP.NET Core 9 runtime image for production deployment
FROM mcr.microsoft.com/dotnet/aspnet:9.0 AS runtime
WORKDIR /app
COPY --from=publish /app/publish .

# Expose port
EXPOSE 8080

# Set environment variables
ENV ASPNETCORE_URLS=http://+:8080
ENV ASPNETCORE_ENVIRONMENT=Development

# Note: Healthcheck removed to keep image minimal; use external monitoring instead.

ENTRYPOINT ["dotnet", "IdleLoadsheddingAPI.dll"]
