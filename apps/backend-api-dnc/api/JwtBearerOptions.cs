namespace Api;

/// <summary>
/// Options for JWT Bearer authentication.
/// </summary>
public class JwtBearerOptions
{
    /// <summary>
    /// Gets or sets the authority.
    /// </summary>
    /// <value>
    /// The authority.
    /// </value>
    public string Authority { get; set; } = String.Empty;

    /// <summary>
    /// Gets or sets the audience.
    /// </summary>
    /// <value>
    /// The audience.
    /// </value>
    public string Audience { get; set; } = String.Empty;
}