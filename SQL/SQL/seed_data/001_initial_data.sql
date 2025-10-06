USE IdleLoadshedding;
GO

-- Seed data for testing with multiple users, friends, and leaderboard
-- Clear existing data if needed (for testing)
-- DELETE FROM dbo.Friends;
-- DELETE FROM dbo.LeaderboardEntries;
-- DELETE FROM dbo.Upgrades;
-- DELETE FROM dbo.Sections;
-- DELETE FROM dbo.Neighbourhoods;
-- DELETE FROM dbo.Users;
-- DBCC CHECKIDENT ('dbo.Users', RESEED, 0);

-- Insert test users
IF NOT EXISTS (SELECT 1 FROM dbo.Users WHERE FirebaseUid = 'test-user-001')
BEGIN
    INSERT INTO dbo.Users (FirebaseUid, Username, Email, Money, Electricity, PrestigeLevel, CreatedAt, LastSyncAt, LastLoginAt)
    VALUES 
        ('test-user-001', 'PowerMaster', 'powermaster@test.com', 10000.00, 150.50, 2, GETDATE(), GETDATE(), GETDATE()),
        ('test-user-002', 'GridGuru', 'gridguru@test.com', 8500.00, 120.25, 1, GETDATE(), GETDATE(), GETDATE()),
        ('test-user-003', 'ElectricAce', 'electricace@test.com', 12000.00, 200.75, 3, GETDATE(), GETDATE(), GETDATE()),
        ('test-user-004', 'VoltViper', 'voltviper@test.com', 6500.00, 95.00, 1, GETDATE(), GETDATE(), GETDATE()),
        ('test-user-005', 'CurrentKing', 'currentking@test.com', 15000.00, 250.00, 4, GETDATE(), GETDATE(), GETDATE()),
        ('test-user-006', 'WattWizard', 'wattwizard@test.com', 7200.00, 110.50, 1, GETDATE(), GETDATE(), GETDATE()),
        ('test-user-007', 'EnergyElite', 'energyelite@test.com', 9300.00, 140.25, 2, GETDATE(), GETDATE(), GETDATE()),
        ('test-user-008', 'CircuitChamp', 'circuitchamp@test.com', 11500.00, 175.00, 2, GETDATE(), GETDATE(), GETDATE()),
        ('test-user-009', 'LoadLord', 'loadlord@test.com', 5800.00, 85.75, 0, GETDATE(), GETDATE(), GETDATE()),
        ('test-user-010', 'PowerPro', 'powerpro@test.com', 13500.00, 220.50, 3, GETDATE(), GETDATE(), GETDATE());
    
    PRINT 'Inserted 10 test users successfully';
END
ELSE
BEGIN
    PRINT 'Test users already exist, skipping user insertion';
END
GO

-- Insert neighbourhoods for each user
IF NOT EXISTS (SELECT 1 FROM dbo.Neighbourhoods WHERE Name LIKE 'Central Grid%')
BEGIN
    INSERT INTO dbo.Neighbourhoods (UserId, Name, IsActive, PowerConsumption, IncomeRate, Level)
    SELECT UserId, Username + ' Central Grid', 1, 10.00 + (UserId * 2), 5.00 + UserId, 1 + (UserId % 3)
    FROM dbo.Users
    WHERE FirebaseUid LIKE 'test-user-%';
    
    PRINT 'Inserted neighbourhoods for test users';
END
GO

-- Insert sections for neighbourhoods
IF NOT EXISTS (SELECT 1 FROM dbo.Sections WHERE Name = 'Main Transformer')
BEGIN
    INSERT INTO dbo.Sections (NeighbourhoodId, Name, PowerOutput, UpgradeLevel)
    SELECT NeighbourhoodId, 'Main Transformer', 15.00 + (NeighbourhoodId * 3), NeighbourhoodId % 5
    FROM dbo.Neighbourhoods
    WHERE UserId IN (SELECT UserId FROM dbo.Users WHERE FirebaseUid LIKE 'test-user-%');
    
    PRINT 'Inserted sections for neighbourhoods';
END
GO

-- Create friend relationships (bidirectional)
IF NOT EXISTS (SELECT 1 FROM dbo.Friends)
BEGIN
    -- Get user IDs
    DECLARE @User1 INT = (SELECT UserId FROM dbo.Users WHERE FirebaseUid = 'test-user-001');
    DECLARE @User2 INT = (SELECT UserId FROM dbo.Users WHERE FirebaseUid = 'test-user-002');
    DECLARE @User3 INT = (SELECT UserId FROM dbo.Users WHERE FirebaseUid = 'test-user-003');
    DECLARE @User4 INT = (SELECT UserId FROM dbo.Users WHERE FirebaseUid = 'test-user-004');
    DECLARE @User5 INT = (SELECT UserId FROM dbo.Users WHERE FirebaseUid = 'test-user-005');
    DECLARE @User6 INT = (SELECT UserId FROM dbo.Users WHERE FirebaseUid = 'test-user-006');
    DECLARE @User7 INT = (SELECT UserId FROM dbo.Users WHERE FirebaseUid = 'test-user-007');
    DECLARE @User8 INT = (SELECT UserId FROM dbo.Users WHERE FirebaseUid = 'test-user-008');

    -- Create friend pairs (bidirectional)
    INSERT INTO dbo.Friends (UserId, FriendUserId, CreatedAt)
    VALUES 
        -- User 1 friends
        (@User1, @User2, GETDATE()),
        (@User2, @User1, GETDATE()),
        (@User1, @User3, GETDATE()),
        (@User3, @User1, GETDATE()),
        (@User1, @User5, GETDATE()),
        (@User5, @User1, GETDATE()),
        -- User 2 friends
        (@User2, @User4, GETDATE()),
        (@User4, @User2, GETDATE()),
        (@User2, @User6, GETDATE()),
        (@User6, @User2, GETDATE()),
        -- User 3 friends
        (@User3, @User7, GETDATE()),
        (@User7, @User3, GETDATE()),
        (@User3, @User8, GETDATE()),
        (@User8, @User3, GETDATE()),
        -- User 5 friends
        (@User5, @User6, GETDATE()),
        (@User6, @User5, GETDATE()),
        (@User5, @User7, GETDATE()),
        (@User7, @User5, GETDATE());
    
    PRINT 'Created friend relationships';
END
GO

-- Create leaderboard entries with rankings
IF NOT EXISTS (SELECT 1 FROM dbo.LeaderboardEntries)
BEGIN
    -- Insert leaderboard entries based on user stats
    INSERT INTO dbo.LeaderboardEntries (UserId, Username, Score, Rank, RecordedAt)
    SELECT 
        UserId,
        Username,
        Money + (Electricity * 10) AS Score,
        ROW_NUMBER() OVER (ORDER BY Money + (Electricity * 10) DESC) AS Rank,
        GETDATE()
    FROM dbo.Users
    WHERE FirebaseUid LIKE 'test-user-%';
    
    PRINT 'Created leaderboard entries with rankings';
END
GO

-- Add some sample upgrades for testing
IF NOT EXISTS (SELECT 1 FROM dbo.Upgrades)
BEGIN
    INSERT INTO dbo.Upgrades (UserId, Name, Cost, ElectricityBoost, PurchasedAt)
    SELECT TOP 5
        UserId,
        'Solar Panel Tier ' + CAST(ROW_NUMBER() OVER (PARTITION BY UserId ORDER BY UserId) AS NVARCHAR(10)),
        500.00 * ROW_NUMBER() OVER (PARTITION BY UserId ORDER BY UserId),
        25.00 * ROW_NUMBER() OVER (PARTITION BY UserId ORDER BY UserId),
        DATEADD(DAY, -ROW_NUMBER() OVER (PARTITION BY UserId ORDER BY UserId), GETDATE())
    FROM dbo.Users
    WHERE FirebaseUid LIKE 'test-user-%'
    ORDER BY UserId;
    
    PRINT 'Created sample upgrades';
END
GO

-- Display summary
SELECT 
    'Summary' AS Info,
    (SELECT COUNT(*) FROM dbo.Users WHERE FirebaseUid LIKE 'test-user-%') AS TestUsers,
    (SELECT COUNT(*) FROM dbo.Neighbourhoods) AS Neighbourhoods,
    (SELECT COUNT(*) FROM dbo.Sections) AS Sections,
    (SELECT COUNT(*) FROM dbo.Friends) AS FriendRelations,
    (SELECT COUNT(*) FROM dbo.LeaderboardEntries) AS LeaderboardEntries,
    (SELECT COUNT(*) FROM dbo.Upgrades) AS Upgrades;
GO

