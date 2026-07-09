param(
    [Parameter(Mandatory = $true)]
    [string] $TenantId,

    [Parameter(Mandatory = $true)]
    [string] $ClientId,

    [Parameter(Mandatory = $true)]
    [string] $ClientSecret,

    [Parameter(Mandatory = $true)]
    [string] $ApplicationId,

    # Attempt to create a Store submission and delete it right away, to probe
    # whether the create endpoint works at all for this app.
    [switch] $TryCreate
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "store-submission-common.ps1")

function Write-Section {
    param([Parameter(Mandatory = $true)] [string] $Title)
    Write-Host ""
    Write-Host "=== $Title ==="
}

Write-Host "Authenticating with Microsoft Store submission API..."
$script:accessToken = Get-AccessToken

Write-Section "Application"
$app = Invoke-StoreApi -Method GET -Path "applications/$ApplicationId"
foreach ($property in $app.PSObject.Properties) {
    if ($property.Value -is [string] -or $property.Value -is [bool] -or $property.Value -is [int] -or $property.Value -is [long] -or $property.Value -is [datetime]) {
        Write-Host "$($property.Name): $($property.Value)"
    }
}

$pendingId = $null
if ($app.pendingApplicationSubmission -and $app.pendingApplicationSubmission.id) {
    $pendingId = $app.pendingApplicationSubmission.id
}
Write-Host "pendingApplicationSubmission: $(if ($pendingId) { $pendingId } else { 'none' })"

$publishedId = $null
if ($app.lastPublishedApplicationSubmission -and $app.lastPublishedApplicationSubmission.id) {
    $publishedId = $app.lastPublishedApplicationSubmission.id
}
Write-Host "lastPublishedApplicationSubmission: $(if ($publishedId) { $publishedId } else { 'none' })"

if ($pendingId) {
    Write-Section "Pending submission $pendingId"
    try {
        $pendingStatus = Get-SubmissionStatus -SubmissionId $pendingId
        Write-Host "status: $($pendingStatus.status)"
        Write-Host "statusDetails: $(ConvertTo-JsonBody $pendingStatus.statusDetails)"
    } catch {
        Write-Warning "Could not read pending submission status: $_"
    }

    Write-Section "Pending submission $pendingId content"
    try {
        $pending = Get-AppSubmission -SubmissionId $pendingId
        Write-Host "targetPublishMode: $($pending.targetPublishMode)"
        Write-Host "packageRollout: $(ConvertTo-JsonBody $pending.packageDeliveryOptions.packageRollout)"

        $packages = @($pending.applicationPackages)
        Write-Host "applicationPackages count: $($packages.Count)"
        foreach ($package in $packages) {
            Write-Host "  fileName=$($package.fileName), version=$($package.version), fileStatus=$($package.fileStatus)"
        }

        $listingProperties = @($pending.listings.PSObject.Properties)
        Write-Host "listing count: $($listingProperties.Count)"
        $enListing = $pending.listings.PSObject.Properties["en-us"]
        if ($enListing -and $enListing.Value.baseListing) {
            $notes = [string]$enListing.Value.baseListing.releaseNotes
            $snippet = if ($notes.Length -gt 200) { $notes.Substring(0, 200) + "..." } else { $notes }
            Write-Host "en-us releaseNotes: $snippet"
        }
    } catch {
        Write-Warning "Could not read pending submission content: $_"
    }
}

if ($publishedId) {
    Write-Section "Published submission $publishedId status"
    try {
        $publishedStatus = Get-SubmissionStatus -SubmissionId $publishedId
        Write-Host "status: $($publishedStatus.status)"
        Write-Host "statusDetails: $(ConvertTo-JsonBody $publishedStatus.statusDetails)"
    } catch {
        Write-Warning "Could not read published submission status: $_"
    }

    Write-Section "Published submission $publishedId package rollout"
    try {
        $rollout = Get-PackageRollout -SubmissionId $publishedId
        Write-Host (ConvertTo-JsonBody $rollout)
    } catch {
        Write-Warning "Could not read package rollout: $_"
    }

    Write-Section "Published submission $publishedId content"
    try {
        $published = Get-AppSubmission -SubmissionId $publishedId
        Write-Host "status: $($published.status)"
        Write-Host "targetPublishMode: $($published.targetPublishMode)"

        $listingProperties = @($published.listings.PSObject.Properties)
        Write-Host "listing count: $($listingProperties.Count)"
        foreach ($listingProperty in $listingProperties) {
            $listing = $listingProperty.Value
            $hasBaseListing = [bool]$listing.baseListing
            $imageCount = 0
            if ($hasBaseListing -and $listing.baseListing.images) {
                $imageCount = @($listing.baseListing.images).Count
            }
            Write-Host "  $($listingProperty.Name): baseListing=$hasBaseListing, images=$imageCount"
        }

        $packages = @($published.applicationPackages)
        Write-Host "applicationPackages count: $($packages.Count)"
        foreach ($package in $packages) {
            Write-Host "  version=$($package.version), fileStatus=$($package.fileStatus), architecture=$($package.architecture)"
        }
    } catch {
        Write-Warning "Could not read published submission content: $_"
    }
}

function Invoke-CreateProbe {
    param(
        [Parameter(Mandatory = $true)] [string] $Label,
        [Parameter(Mandatory = $true)] [scriptblock] $Request
    )

    Write-Host "--- Probe: $Label"
    try {
        $submission = & $Request
        $listingCount = @($submission.listings.PSObject.Properties).Count
        Write-Host "Create SUCCEEDED ($Label): submission $($submission.id) with $listingCount listing(s)."
        Write-Host "Deleting the probe submission again..."
        Invoke-StoreApi -Method DELETE -Path "applications/$ApplicationId/submissions/$($submission.id)" | Out-Null
        Write-Host "Probe submission deleted."
        return $true
    } catch {
        Write-Warning "Create FAILED ($Label): $_"
        return $false
    }
}

if ($TryCreate -and $pendingId) {
    Write-Section "Create submission probe"
    Write-Host "Skipped: a pending submission ($pendingId) already exists and must not be disturbed."
} elseif ($TryCreate) {
    Write-Section "Create submission probe"

    # Exercise the same code path the release workflow uses, so this doubles
    # as a pre-flight check for publish-msix-to-store.ps1.
    $created = Invoke-CreateProbe -Label "Invoke-StoreApi" {
        Invoke-StoreApi -Method POST -Path "applications/$ApplicationId/submissions"
    }

    if (-not $created) {
        Write-Warning "Create probe failed."
    }
}

Write-Host ""
Write-Host "Diagnostics finished."
