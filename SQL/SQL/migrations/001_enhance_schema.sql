-- ============================================
-- Idle Loadshedding Database Schema
-- Single migration file for 10-user setup
-- ============================================

USE IdleLoadshedding;
GO

-- Users Table: Stores player accounts
IF OBJECT_ID('dbo.Users', 'U') IS NULL
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
        LastSyncAt DATETIME NOT NULL CONSTRAINT DF_Users_LastSyncAt DEFAULT GETDATE(),
        LastLoginAt DATETIME NULL
    );
END
GO

-- Neighbourhoods Table: City sections player manages
IF OBJECT_ID('dbo.Neighbourhoods', 'U') IS NULL
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

-- Sections Table: Sub-areas within a neighbourhood
IF OBJECT_ID('dbo.Sections', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Sections
    (
        SectionId INT IDENTITY(1,1) PRIMARY KEY,
        NeighbourhoodId INT NOT NULL,
        Name NVARCHAR(50) NOT NULL,
        PowerOutput DECIMAL(10,2) NOT NULL CONSTRAINT DF_Sections_PowerOutput DEFAULT (0),
        UpgradeLevel INT NOT NULL CONSTRAINT DF_Sections_UpgradeLevel DEFAULT (0),
        CONSTRAINT FK_Sections_Neighbourhoods FOREIGN KEY (NeighbourhoodId) REFERENCES dbo.Neighbourhoods(NeighbourhoodId) ON DELETE CASCADE
    );
END
GO

-- Upgrades Table: Tracks upgrades purchased by a user
IF OBJECT_ID('dbo.Upgrades', 'U') IS NULL
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

-- Friends Table: Two-way friend relationships
IF OBJECT_ID('dbo.Friends', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Friends
    (
        FriendId INT IDENTITY(1,1) PRIMARY KEY,
        UserId INT NOT NULL,
        FriendUserId INT NOT NULL,
        CreatedAt DATETIME NOT NULL CONSTRAINT DF_Friends_CreatedAt DEFAULT GETDATE(),
    CONSTRAINT FK_Friends_Users FOREIGN KEY (UserId) REFERENCES dbo.Users(UserId) ON DELETE CASCADE,
    -- Avoid multiple cascade paths by not cascading on FriendUser delete
    CONSTRAINT FK_Friends_FriendUser FOREIGN KEY (FriendUserId) REFERENCES dbo.Users(UserId) ON DELETE NO ACTION,
        CONSTRAINT UX_Friends UNIQUE (UserId, FriendUserId)
    );
END
GO

-- LeaderboardEntries Table: High score snapshots
IF OBJECT_ID('dbo.LeaderboardEntries', 'U') IS NULL
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

-- Modifications: Keep schema readable and updated
IF COL_LENGTH('dbo.Users', 'LastLoginAt') IS NULL
BEGIN
    ALTER TABLE dbo.Users ADD LastLoginAt DATETIME NULL;
END
GO

IF COL_LENGTH('dbo.Neighbourhoods', 'Level') IS NULL
BEGIN
    ALTER TABLE dbo.Neighbourhoods ADD Level INT NOT NULL CONSTRAINT DF_Neighbourhoods_Level_Update DEFAULT (1);
END
GO
