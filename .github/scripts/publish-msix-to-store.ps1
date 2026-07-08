param(
    [Parameter(Mandatory = $true)]
    [string] $TenantId,

    [Parameter(Mandatory = $true)]
    [string] $ClientId,

    [Parameter(Mandatory = $true)]
    [string] $ClientSecret,

    [Parameter(Mandatory = $true)]
    [string] $ApplicationId,

    [Parameter(Mandatory = $true)]
    [string] $MsixPath,

    [double] $RolloutPercentage = 90.0,

    [string] $ReleaseNotesPath = "assets/storecopy/microsoft-store-release-notes.json",

    [string] $TargetPublishMode = "Immediate",

    # Prepare the submission (package upload, rollout, release notes) but leave
    # it as an uncommitted draft in Partner Center instead of publishing it.
    [switch] $SkipCommit,

    # Delete an existing uncommitted draft submission instead of failing, e.g.
    # the draft left behind by a previous -SkipCommit run.
    [switch] $ReplacePendingSubmission,

    [int] $StatusPollSeconds = 30,

    [int] $StatusPollAttempts = 20,

    # How long to keep retrying submission creation while Partner Center is
    # still republishing the app after a rollout finalization. Must fit in the
    # deploy job's timeout-minutes budget together with the build itself.
    [int] $CreateSubmissionWaitMinutes = 40
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "store-submission-common.ps1")

function Finalize-PreviousRolloutIfNeeded {
    param([Parameter(Mandatory = $true)] $App)

    if (-not $App.lastPublishedApplicationSubmission -or -not $App.lastPublishedApplicationSubmission.id) {
        Write-Host "No previously published submission found; skipping rollout finalization."
        return
    }

    $submissionId = $App.lastPublishedApplicationSubmission.id
    Write-Host "Checking previous submission rollout: $submissionId"

    $rollout = Get-PackageRollout -SubmissionId $submissionId
    Write-Host "Previous rollout status: $($rollout.packageRolloutStatus), percentage: $($rollout.packageRolloutPercentage)"

    if ($rollout.isPackageRollout -and $rollout.packageRolloutStatus -eq "PackageRolloutInProgress") {
        Write-Host "Finalizing previous rollout before creating the new submission..."

        # The finalizepackagerollout endpoint is slow and regularly answers with a
        # gateway timeout even though the operation may still complete server-side,
        # so verify the rollout status between attempts instead of trusting the
        # response alone.
        $maxAttempts = 4
        for ($attempt = 1; $attempt -le $maxAttempts; $attempt++) {
            try {
                Invoke-StoreApi -Method POST -Path "applications/$ApplicationId/submissions/$submissionId/finalizepackagerollout" | Out-Null
                Write-Host "Previous rollout finalized."
                return
            } catch {
                Write-Warning "Finalize attempt $attempt of ${maxAttempts} failed: $_"
            }

            Start-Sleep -Seconds 30
            $rollout = Get-PackageRollout -SubmissionId $submissionId
            if ($rollout.packageRolloutStatus -ne "PackageRolloutInProgress") {
                Write-Host "Previous rollout status is now $($rollout.packageRolloutStatus); finalization no longer needed."
                return
            }
        }

        throw "Could not finalize the package rollout of previous submission $submissionId after $maxAttempts attempts."
    } else {
        Write-Host "No in-progress previous rollout to finalize."
    }
}

function Convert-ReleaseNoteValue {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Locale,

        [Parameter(Mandatory = $true)]
        $Value
    )

    if ($Value -is [string]) {
        $note = $Value.Trim()
    } elseif ($Value -is [System.Collections.IEnumerable]) {
        $lines = @()
        foreach ($line in $Value) {
            if ($null -ne $line) {
                $lines += ([string]$line).Trim()
            }
        }
        $note = ($lines | Where-Object { $_ }) -join "`r`n"
    } else {
        throw "Release notes for '$Locale' must be a string or an array of strings."
    }

    if ([string]::IsNullOrWhiteSpace($note)) {
        throw "Release notes for '$Locale' are empty."
    }

    return $note
}

function Read-ReleaseNotes {
    param([Parameter(Mandatory = $true)] [string] $Path)

    if (-not (Test-Path $Path)) {
        throw "Release notes file not found at '$Path'. Create it with notes for every Microsoft Store listing locale."
    }

    try {
        $json = Get-Content -Path $Path -Raw | ConvertFrom-Json
    } catch {
        throw "Release notes file '$Path' is not valid JSON: $_"
    }

    if ($json.PSObject.Properties.Name -contains "notes") {
        return $json.notes
    }

    if ($json.PSObject.Properties.Name -contains "releaseNotes") {
        return $json.releaseNotes
    }

    return $json
}

function Apply-ReleaseNotes {
    param(
        [Parameter(Mandatory = $true)] $Submission,
        [Parameter(Mandatory = $true)] [string] $Path
    )

    if (-not $Submission.listings) {
        throw "The Store submission does not contain any listings; cannot apply release notes."
    }

    $notesObject = Read-ReleaseNotes -Path $Path
    $notesByLocale = @{}
    foreach ($property in $notesObject.PSObject.Properties) {
        $notesByLocale[$property.Name.ToLowerInvariant()] = @{
            Locale = $property.Name
            Value = $property.Value
        }
    }

    $listingLocales = @($Submission.listings.PSObject.Properties.Name)
    $missingLocales = @()

    foreach ($locale in $listingLocales) {
        $localeKey = $locale.ToLowerInvariant()
        if (-not $notesByLocale.ContainsKey($localeKey)) {
            $missingLocales += $locale
            continue
        }

        $listing = $Submission.listings.PSObject.Properties[$locale].Value
        if (-not $listing.baseListing) {
            throw "Store listing '$locale' does not contain a baseListing; cannot apply release notes."
        }

        $note = Convert-ReleaseNoteValue -Locale $locale -Value $notesByLocale[$localeKey].Value
        if ($listing.baseListing.PSObject.Properties.Name -contains "releaseNotes") {
            $listing.baseListing.releaseNotes = $note
        } else {
            $listing.baseListing | Add-Member -MemberType NoteProperty -Name releaseNotes -Value $note
        }
    }

    if ($missingLocales.Count -gt 0) {
        throw "Release notes file '$Path' is missing notes for Store listing locale(s): $($missingLocales -join ', ')"
    }

    $listingLocaleKeys = @{}
    foreach ($locale in $listingLocales) {
        $listingLocaleKeys[$locale.ToLowerInvariant()] = $true
    }

    foreach ($localeKey in $notesByLocale.Keys) {
        if (-not $listingLocaleKeys.ContainsKey($localeKey)) {
            Write-Warning "Release notes file '$Path' contains unused locale '$($notesByLocale[$localeKey].Locale)'."
        }
    }

    Write-Host "Applied release notes for Store listing locale(s): $($listingLocales -join ', ')"
}

function New-SubmissionRequestBody {
    param(
        [Parameter(Mandatory = $true)] $Submission,
        [Parameter(Mandatory = $true)] [string] $PackageFileName
    )

    $packageDeliveryOptions = $Submission.packageDeliveryOptions
    if (-not $packageDeliveryOptions) {
        $packageDeliveryOptions = [ordered]@{
            packageRollout = [ordered]@{}
            isMandatoryUpdate = $false
            mandatoryUpdateEffectiveDate = "1601-01-01T00:00:00.0000000Z"
        }
    }

    if (-not $packageDeliveryOptions.packageRollout) {
        $packageDeliveryOptions | Add-Member -MemberType NoteProperty -Name packageRollout -Value ([ordered]@{}) -Force
    }

    $packageDeliveryOptions.packageRollout.isPackageRollout = $true
    $packageDeliveryOptions.packageRollout.packageRolloutPercentage = $RolloutPercentage

    $body = [ordered]@{}
    $propertyNames = @(
        "applicationCategory",
        "pricing",
        "visibility",
        "targetPublishDate",
        "listings",
        "hardwarePreferences",
        "automaticBackupEnabled",
        "canInstallOnRemovableMedia",
        "isGameDvrEnabled",
        "gamingOptions",
        "hasExternalInAppProducts",
        "meetAccessibilityGuidelines",
        "notesForCertification",
        "enterpriseLicensing",
        "allowMicrosoftDecideAppAvailabilityToFutureDeviceFamilies",
        "allowTargetFutureDeviceFamilies",
        "trailers"
    )

    foreach ($propertyName in $propertyNames) {
        if ($Submission.PSObject.Properties.Name -contains $propertyName) {
            $body[$propertyName] = $Submission.$propertyName
        }
    }

    $body["targetPublishMode"] = $TargetPublishMode

    # Keep the newest already-published package so customers outside the
    # gradual rollout group still get a valid package; delete anything older.
    $packages = @()
    $existingPackages = @($Submission.applicationPackages | Where-Object { $_ })
    if ($existingPackages.Count -gt 0) {
        $sortedPackages = @($existingPackages | Sort-Object -Property { [version]$_.version } -Descending)
        for ($i = 0; $i -lt $sortedPackages.Count; $i++) {
            $package = $sortedPackages[$i]
            if ($i -gt 0) {
                $package.fileStatus = "PendingDelete"
            }
            $packages += $package
        }
    }

    $packages += [ordered]@{
        fileName = $PackageFileName
        fileStatus = "PendingUpload"
        minimumDirectXVersion = "None"
        minimumSystemRam = "None"
    }

    $body["applicationPackages"] = @($packages)
    $body["packageDeliveryOptions"] = $packageDeliveryOptions

    return $body
}

function New-StoreSubmission {
    # Like finalizepackagerollout and commit, the create endpoint can time out
    # at the gateway while the submission is still created server-side, so on
    # failure check whether a pending submission appeared and adopt it instead
    # of retrying blindly.
    #
    # After a rollout finalization Partner Center republishes the app, and until
    # that republish completes the create endpoint rejects requests with
    # InvalidParameterValue "The size of Listings must be 1 or more", even
    # while the published submission reads back fine with all of its listings,
    # so reading it is no readiness signal. The republish regularly takes tens
    # of minutes, so failures with that signature keep retrying until
    # $CreateSubmissionWaitMinutes elapses instead of giving up after a few
    # attempts.
    $republishSignature = "The size of Listings must be 1 or more"
    $deadline = (Get-Date).AddMinutes($CreateSubmissionWaitMinutes)
    $shortAttemptLimit = 4
    $sawRepublish = $false
    $lastError = $null

    for ($attempt = 1; ; $attempt++) {
        try {
            return Invoke-StoreApi -Method POST -Path "applications/$ApplicationId/submissions"
        } catch {
            $lastError = "$_"
            Write-Warning "Create submission attempt $attempt failed: $_"
        }

        if ($lastError -match $republishSignature) {
            $sawRepublish = $true
        }

        Start-Sleep -Seconds 30

        try {
            $app = Invoke-StoreApi -Method GET -Path "applications/$ApplicationId"
            if ($app.pendingApplicationSubmission -and $app.pendingApplicationSubmission.id) {
                $pendingId = $app.pendingApplicationSubmission.id
                Write-Host "Found pending submission $pendingId created by an earlier attempt; using it."
                return Get-AppSubmission -SubmissionId $pendingId
            }
        } catch {
            Write-Warning "Could not check for a pending submission after the failed create: $_"
        }

        if ($sawRepublish) {
            if ((Get-Date) -ge $deadline) {
                break
            }
            Write-Host "Partner Center is still republishing the app after the rollout finalization; retrying until $($deadline.ToString('HH:mm:ss'))."
            Start-Sleep -Seconds 90
        } elseif ($attempt -ge $shortAttemptLimit) {
            break
        }
    }

    $hint = ""
    if ($sawRepublish) {
        $hint = " Partner Center did not finish republishing the app after the rollout finalization within $CreateSubmissionWaitMinutes minutes." +
            " Wait until the app leaves the 'Publishing' state in Partner Center, then re-run this workflow."
    }
    throw "Could not create a new Store submission. Last error: $lastError$hint"
}

if (-not (Test-Path $MsixPath)) {
    throw "MSIX package not found at $MsixPath"
}

if ($RolloutPercentage -le 0 -or $RolloutPercentage -gt 100) {
    throw "RolloutPercentage must be greater than 0 and less than or equal to 100."
}

$resolvedMsix = (Resolve-Path $MsixPath).Path
$packageFileName = Split-Path -Path $resolvedMsix -Leaf
$zipPath = Join-Path -Path ([System.IO.Path]::GetTempPath()) -ChildPath ("store-submission-{0}.zip" -f ([guid]::NewGuid()))

Write-Host "Authenticating with Microsoft Store submission API..."
$script:accessToken = Get-AccessToken

Write-Host "Loading app $ApplicationId..."
$app = Invoke-StoreApi -Method GET -Path "applications/$ApplicationId"

Finalize-PreviousRolloutIfNeeded -App $app

$app = Invoke-StoreApi -Method GET -Path "applications/$ApplicationId"
if ($app.pendingApplicationSubmission -and $app.pendingApplicationSubmission.id) {
    $pendingId = $app.pendingApplicationSubmission.id
    $pending = Get-AppSubmission -SubmissionId $pendingId

    if (-not $ReplacePendingSubmission) {
        throw "App already has pending submission $pendingId with status $($pending.status). Resolve or delete that draft before creating a new Store submission, or pass -ReplacePendingSubmission."
    }

    if ($pending.status -ne "PendingCommit") {
        throw "App already has pending submission $pendingId with status $($pending.status); only an uncommitted draft (PendingCommit) can be replaced automatically. Resolve it in Partner Center first."
    }

    Write-Host "Deleting existing draft submission $pendingId before creating a new one..."

    # The delete can also time out at the gateway while still succeeding
    # server-side, so verify the draft is gone instead of trusting the
    # response; a stale draft surviving here would be mistaken for our own
    # submission by the create step's recovery logic.
    $maxAttempts = 3
    $draftDeleted = $false
    for ($attempt = 1; $attempt -le $maxAttempts; $attempt++) {
        try {
            Invoke-StoreApi -Method DELETE -Path "applications/$ApplicationId/submissions/$pendingId" | Out-Null
        } catch {
            Write-Warning "Delete draft attempt $attempt of ${maxAttempts} failed: $_"
        }

        $app = Invoke-StoreApi -Method GET -Path "applications/$ApplicationId"
        if (-not ($app.pendingApplicationSubmission -and $app.pendingApplicationSubmission.id)) {
            $draftDeleted = $true
            break
        }

        Start-Sleep -Seconds 15
    }

    if (-not $draftDeleted) {
        throw "Could not delete existing draft submission $pendingId after $maxAttempts attempts."
    }
}

Write-Host "Creating new Store submission..."
$submission = New-StoreSubmission
$submissionId = $submission.id
$commitStarted = $false
Write-Host "Created submission $submissionId"

try {
    Write-Host "Creating submission ZIP with $packageFileName..."
    Compress-Archive -Path $resolvedMsix -DestinationPath $zipPath -Force

    Apply-ReleaseNotes -Submission $submission -Path $ReleaseNotesPath

    $requestBody = New-SubmissionRequestBody -Submission $submission -PackageFileName $packageFileName

    Write-Host "Updating submission $submissionId with package $packageFileName and $RolloutPercentage% rollout..."
    $submission = Invoke-StoreApi -Method PUT -Path "applications/$ApplicationId/submissions/$submissionId" -Body $requestBody

    Write-Host "Uploading package ZIP to Microsoft Store ingestion storage..."
    $maxUploadAttempts = 3
    for ($attempt = 1; $attempt -le $maxUploadAttempts; $attempt++) {
        try {
            Invoke-RestMethod `
                -Method Put `
                -Uri $submission.fileUploadUrl `
                -InFile $zipPath `
                -ContentType "application/zip" `
                -Headers @{ "x-ms-blob-type" = "BlockBlob" } | Out-Null
            break
        } catch {
            if ($attempt -eq $maxUploadAttempts) {
                throw
            }
            Write-Warning "Package upload attempt $attempt of ${maxUploadAttempts} failed: $_"
            Start-Sleep -Seconds (30 * $attempt)
        }
    }

    if ($SkipCommit) {
        Write-Host "Submission $submissionId is prepared as an uncommitted draft with $RolloutPercentage% package rollout; the currently published version stays live."
        Write-Host "Publish it with .github/scripts/commit-store-submission.ps1 or from a release tag."
    } else {
        Write-Host "Committing submission $submissionId..."
        Invoke-SubmissionCommit -SubmissionId $submissionId
        $commitStarted = $true

        Wait-ForCommitToStartProcessing -SubmissionId $submissionId
        Write-Host "Microsoft Store submission $submissionId created with $RolloutPercentage% package rollout."
    }
} catch {
    if ($submissionId -and -not $commitStarted) {
        try {
            Write-Warning "Deleting uncommitted Store submission $submissionId after failure."
            Invoke-StoreApi -Method DELETE -Path "applications/$ApplicationId/submissions/$submissionId" | Out-Null
        } catch {
            Write-Warning "Could not delete uncommitted Store submission $submissionId. Resolve it manually before the next release. Error: $_"
        }
    }
    throw
} finally {
    if (Test-Path $zipPath) {
        Remove-Item -Path $zipPath -Force
    }
}
