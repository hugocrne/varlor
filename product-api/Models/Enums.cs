namespace product_api.Models;

public enum ClientType
{
    INDIVIDUAL,
    COMPANY
}

public enum ClientStatus
{
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    PENDING
}

public enum UserRole
{
    OWNER,
    ADMIN,
    MEMBER,
    SERVICE
}

public enum UserStatus
{
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    PENDING
}

public enum Theme
{
    LIGHT,
    DARK,
    SYSTEM
}