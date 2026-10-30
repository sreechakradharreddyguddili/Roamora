$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location (Join-Path $root "backend")
$env:JAVA_TOOL_OPTIONS = "-Duser.timezone=UTC"
if (-not $env:DATABASE_URL) { $env:DATABASE_URL = "jdbc:postgresql://localhost:5432/weatherwise" }
if (-not $env:DATABASE_USERNAME) { $env:DATABASE_USERNAME = "weatherwise" }
if (-not $env:DATABASE_PASSWORD) { $env:DATABASE_PASSWORD = "weatherwise_dev_password" }
.\mvnw.cmd spring-boot:run
