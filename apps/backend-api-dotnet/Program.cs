using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Authentication;
using Microsoft.Extensions.Configuration;
using System.Security.Claims;
using Microsoft.OpenApi.Models;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;

namespace backend_api_dotnet;

public class Program
    {
        public static void Main(string[] args)
        {
            var loggerFactory = LoggerFactory
            .Create(builder =>
            {
                builder.ClearProviders();
                builder.AddConsole();
            });
            var builder = CreateHostBuilder(args, loggerFactory);
            loggerFactory.CreateLogger<Program>().LogInformation("Hello!");

            builder.Build().Run();
        }

        public static IHostBuilder CreateHostBuilder(string[] args, ILoggerFactory loggerFactory) =>
            Host.CreateDefaultBuilder(args)
                .ConfigureWebHostDefaults(webBuilder => 
                    webBuilder.UseStartup(context => new Startup(context.Configuration, loggerFactory))
                );
    }
