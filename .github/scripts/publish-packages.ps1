$ErrorActionPreference = "Stop"

$tasks = @(
    ":html-dom-logging:publishMavenJavaPublicationToGitHubPackagesRepository",
    ":html-dom-core:publishMavenJavaPublicationToGitHubPackagesRepository",
    ":html-dom-fonts:publishMavenJavaPublicationToGitHubPackagesRepository",
    ":html-dom-icons-fontawesome:publishMavenJavaPublicationToGitHubPackagesRepository",
    ":html-dom-scripting-lua:publishMavenJavaPublicationToGitHubPackagesRepository",
    ":html-dom-devtools:publishMavenJavaPublicationToGitHubPackagesRepository",
    ":html-dom-desktop:publishMavenJavaPublicationToGitHubPackagesRepository",
    ":html-dom-desktop:publishAioPublicationToGitHubPackagesRepository"
)

foreach ($task in $tasks) {
    Write-Host "Publishing $task"
    $output = & .\gradlew.bat $task --console=plain --no-daemon 2>&1
    $exitCode = $LASTEXITCODE
    $output | ForEach-Object { Write-Host $_ }

    if ($exitCode -eq 0) {
        Write-Host "Published $task"
        continue
    }

    $text = ($output | Out-String)
    if ($text -match "status code 409" -or $text -match "Received status code 409" -or $text -match "Conflict") {
        Write-Host "Package version already exists for $task; skipping immutable GitHub Packages upload."
        continue
    }

    throw "Publishing failed for $task with exit code $exitCode"
}
