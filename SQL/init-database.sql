USE master;
GO

IF DB_ID(N'IdleLoadshedding') IS NULL
BEGIN
    CREATE DATABASE IdleLoadshedding;
END
GO

USE IdleLoadshedding;
GO

-- Core tables for simplified 10-user deployment
IF OBJECT_ID(N'dbo.Users', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Users
    (
        UserId INT IDENTITY(1,1) PRIMARY KEY,
        FirebaseUid NVARCHAR(128) NOT NULL UNIQUE,
        Username NVARCHAR(50) NOT NULL,
        Email NVARCHAR(100) NOT NULL,
        Money DECIMAL(18,2) NOT NULL CONSTRAINT DF_Users_Money DEFAULT (0),
        Electricity DECIMAL(18,2) NOT NULL CONSTRAINT DF_Users_Electricity DEFAULT (0),
        PrestigeLevel INT NOT NULL CONSTRAINT DF_Users_Prestige DEFAULT (0),
        CreatedAt DATETIME NOT NULL CONSTRAINT DF_Users_CreatedAt DEFAULT GETDATE(),
        LastSyncAt DATETIME NOT NULL CONSTRAINT DF_Users_LastSync DEFAULT GETDATE(),
        LastLoginAt DATETIME NULL
    );
END
GO

IF OBJECT_ID(N'dbo.Neighbourhoods', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Neighbourhoods
    (
        NeighbourhoodId INT IDENTITY(1,1) PRIMARY KEY,
        UserId INT NOT NULL,
        Name NVARCHAR(50) NOT NULL,
        IsActive BIT NOT NULL CONSTRAINT DF_Neighbourhoods_IsActive DEFAULT (1),
        PowerConsumption DECIMAL(10,2) NOT NULL CONSTRAINT DF_Neighbourhoods_Power DEFAULT (0),
        IncomeRate DECIMAL(10,2) NOT NULL CONSTRAINT DF_Neighbourhoods_Income DEFAULT (0),
        Level INT NOT NULL CONSTRAINT DF_Neighbourhoods_Level DEFAULT (1),
        CONSTRAINT FK_Neighbourhoods_Users FOREIGN KEY (UserId) REFERENCES dbo.Users(UserId) ON DELETE CASCADE
    );
END
GO

IF OBJECT_ID(N'dbo.Sections', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Sections
    (
        SectionId INT IDENTITY(1,1) PRIMARY KEY,
        NeighbourhoodId INT NOT NULL,
        Name NVARCHAR(50) NOT NULL,
        PowerOutput DECIMAL(10,2) NOT NULL CONSTRAINT DF_Sections_Power DEFAULT (0),
        UpgradeLevel INT NOT NULL CONSTRAINT DF_Sections_Upgrade DEFAULT (0),
        CONSTRAINT FK_Sections_Neighbourhoods FOREIGN KEY (NeighbourhoodId) REFERENCES dbo.Neighbourhoods(NeighbourhoodId) ON DELETE CASCADE
    );
END
GO

IF OBJECT_ID(N'dbo.Upgrades', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Upgrades
    (
        UpgradeId INT IDENTITY(1,1) PRIMARY KEY,
        UserId INT NOT NULL,
        Name NVARCHAR(100) NOT NULL,
        Cost DECIMAL(18,2) NOT NULL,
        ElectricityBoost DECIMAL(18,2) NOT NULL,
        PurchasedAt DATETIME NOT NULL CONSTRAINT DF_Upgrades_PurchasedAt DEFAULT GETDATE(),
        CONSTRAINT FK_Upgrades_Users FOREIGN KEY (UserId) REFERENCES dbo.Users(UserId) ON DELETE CASCADE
    );
END
GO

IF OBJECT_ID(N'dbo.Friends', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Friends
    (
        FriendId INT IDENTITY(1,1) PRIMARY KEY,
        UserId INT NOT NULL,
        FriendUserId INT NOT NULL,
        CreatedAt DATETIME NOT NULL CONSTRAINT DF_Friends_CreatedAt DEFAULT GETDATE(),
        CONSTRAINT FK_Friends_Users FOREIGN KEY (UserId) REFERENCES dbo.Users(UserId) ON DELETE CASCADE,
        CONSTRAINT FK_Friends_FriendUser FOREIGN KEY (FriendUserId) REFERENCES dbo.Users(UserId) ON DELETE NO ACTION,
        CONSTRAINT UX_Friends UNIQUE (UserId, FriendUserId)
    );
END
GO

IF OBJECT_ID(N'dbo.LeaderboardEntries', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.LeaderboardEntries
    (
        LeaderboardEntryId INT IDENTITY(1,1) PRIMARY KEY,
        UserId INT NOT NULL,
        Username NVARCHAR(50) NOT NULL,
        Score DECIMAL(18,2) NOT NULL,
        Rank INT NOT NULL,
        RecordedAt DATETIME NOT NULL CONSTRAINT DF_LeaderboardEntries_RecordedAt DEFAULT GETDATE(),
        CONSTRAINT FK_LeaderboardEntries_Users FOREIGN KEY (UserId) REFERENCES dbo.Users(UserId) ON DELETE CASCADE
    );
END
GO

