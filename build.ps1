$ErrorActionPreference = "Stop"

$sourceFiles = Get-ChildItem -Path "src\main\java" -Filter "*.java" -Recurse
if ($sourceFiles.Count -eq 0) {
    throw "No Java source files found."
}

New-Item -ItemType Directory -Force -Path "out\classes" | Out-Null
javac -encoding UTF-8 -d "out\classes" $sourceFiles.FullName
Copy-Item -Path "src\main\resources\*" -Destination "out\classes" -Recurse -Force

Write-Host "Build completed. Run with: .\run.ps1"
