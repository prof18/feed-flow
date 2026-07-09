# Commits (publishes) the pending Store submission prepared earlier with
# publish-msix-to-store.ps1 -SkipCommit.
param(
    [Parameter(Mandatory = $true)]
    [string] $TenantId,

    [Parameter(Mandatory = $true)]
    [string] $ClientId,

    [Parameter(Mandatory = $true)]
    [string] $ClientSecret,

    [Parameter(Mandatory = $true)]
    [string] $ApplicationId,

    # When set, refuse to publish a draft that does not contain this package,
    # e.g. a stale draft prepared for a different version.
    [string] $ExpectedPackageFileName,

    [int] $StatusPollSeconds = 30,

    [int] $StatusPollAttempts = 20
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "store-submission-common.ps1")

Write-Host "Authenticating with Microsoft Store submission API..."
$script:accessToken = Get-AccessToken

Write-Host "Loading app $ApplicationId..."
$app = Invoke-StoreApi -Method GET -Path "applications/$ApplicationId"

if (-not $app.pendingApplicationSubmission -or -not $app.pendingApplicationSubmission.id) {
    throw "App $ApplicationId has no pending submission to publish. Prepare one first with publish-msix-to-store.ps1 -SkipCommit."
}

$submissionId = $app.pendingApplicationSubmission.id
$submission = Get-AppSubmission -SubmissionId $submissionId

if ($submission.status -ne "PendingCommit") {
    throw "Pending submission $submissionId has status $($submission.status); expected an uncommitted draft (PendingCommit)."
}

$packageFileNames = @($submission.applicationPackages | ForEach-Object { $_.fileName } | Where-Object { $_ })
Write-Host "Pending submission $submissionId contains package(s): $($packageFileNames -join ', ')"

if ($ExpectedPackageFileName -and $packageFileNames -notcontains $ExpectedPackageFileName) {
    throw "Pending submission $submissionId does not contain package '$ExpectedPackageFileName'; refusing to publish it."
}

Write-Host "Committing submission $submissionId..."
Invoke-SubmissionCommit -SubmissionId $submissionId

Wait-ForCommitToStartProcessing -SubmissionId $submissionId
Write-Host "Microsoft Store submission $submissionId committed for publishing."
