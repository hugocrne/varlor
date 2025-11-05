using System;

namespace product_api.Exceptions;

public class DuplicateResourceException : Exception
{
    public DuplicateResourceException() : base()
    {
    }

    public DuplicateResourceException(string message) : base(message)
    {
    }

    public DuplicateResourceException(string message, Exception innerException) : base(message, innerException)
    {
    }
}