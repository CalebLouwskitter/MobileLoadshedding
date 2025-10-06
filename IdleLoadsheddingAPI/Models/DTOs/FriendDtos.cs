// File intentionally left blank after simplification.
namespace IdleLoadsheddingAPI.Models.DTOs;

public record FriendsRequestDto(
    string PlayerId,
    IReadOnlyCollection<FriendSubmissionDto> Friends
);

public record FriendSubmissionDto(
    string Id,
    string DisplayName,
    double ContributionPerHour,
    bool IsOnline,
    bool Remove
);

public record FriendsResponseDto(
    IReadOnlyCollection<FriendProfileDto> Friends,
    DateTime TimestampUtc
);

public record FriendProfileDto(
    string Id,
    string DisplayName,
    bool IsOnline,
    double ContributionPerHour,
    DateTime LastInteractionUtc
);
