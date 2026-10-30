$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendScript = Join-Path $root "START_BACKEND.ps1"
$frontendScript = Join-Path $root "START_FRONTEND.ps1"
if (-not (Test-Path $backendScript) -or -not (Test-Path $frontendScript)) {
  throw "Roamora startup scripts are missing. Re-extract the complete project ZIP."
}
Start-Process -FilePath "powershell.exe" -ArgumentList "-NoExit -ExecutionPolicy Bypass -File `"$backendScript`""
Start-Process -FilePath "powershell.exe" -ArgumentList "-NoExit -ExecutionPolicy Bypass -File `"$frontendScript`""
Write-Host "Roamora startup terminals opened." -ForegroundColor Green
Write-Host "Make sure PostgreSQL and Redis are running before using the app." -ForegroundColor Yellow
Write-Host "Frontend: http://localhost:5173  |  Backend: http://localhost:8080" -ForegroundColor Cyan
