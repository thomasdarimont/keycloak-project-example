using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Api.Controllers;

[ApiController]
[Route("/api/users")]
public class UsersController
{
    
    private readonly ILogger<UsersController> _logger;

    private readonly IHttpContextAccessor _accessor;

    public UsersController(ILogger<UsersController> logger, IHttpContextAccessor accessor)
    {
        _logger = logger;
        _accessor = accessor;
    }
    
    [Authorize]
    [HttpGet]
    [Route("me")]
    public object Me() {

        _logger.LogInformation("### Accessing {}", _accessor.HttpContext?.Request.Path.Value);
        var username = _accessor.HttpContext?.User.FindFirst("preferred_username")?.Value;

        var data = new Dictionary<string,object>
        {
            { "message", "Hello " + username },
            { "backend", "AspNetCore" },
            { "datetime", DateTime.Now }
        };
        return data;
    }

}