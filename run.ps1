$ErrorActionPreference = "Stop"

if (-not (Test-Path "out\classes\com\fakereviewdetector\FakeReviewDetectorApp.class")) {
    & "$PSScriptRoot\build.ps1"
}

java -cp "out\classes" com.fakereviewdetector.FakeReviewDetectorApp 8080
