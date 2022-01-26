USE [master]
GO

IF DB_ID('keycloak') IS NOT NULL
  set noexec on -- prevent creation when already exists

CREATE DATABASE [keycloak];
GO

USE [keycloak]
GO

CREATE LOGIN keycloak WITH PASSWORD='Keycloak123';
GO

CREATE USER keycloak FOR LOGIN keycloak;
GO

GRANT ALL ON keycloak TO [keycloak];
GO

EXEC sp_addrolemember 'db_owner', N'keycloak'
GO
