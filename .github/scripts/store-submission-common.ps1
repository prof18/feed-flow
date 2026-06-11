# Shared helpers for the Microsoft Store submission API scripts.
# Dot-source this file from a script that defines $TenantId, $ClientId,
# $ClientSecret and $ApplicationId; Wait-ForCommitToStartProcessing also
# expects $StatusPollSeconds and $StatusPollAttempts.

$baseUri = "https://manage.devcenter.microsoft.com/v1.0/my"

function ConvertTo-JsonBody {
    param([Parameter(Mandatory = $true)] $Body)

    return ($Body | ConvertTo-Json -Depth 100 -Compress)
}

function Read-ErrorResponseBody {
    param([Parameter(Mandatory = $true)] $ErrorRecord)

    if ($ErrorRecord.ErrorDetails -and $ErrorRecord.ErrorDetails.Message) {
        return $ErrorRecord.ErrorDetails.Message
    }

    $exception = $ErrorRecord.Exception
    if (-not $exception.Response) {
        return $exception.Message
    }

    try {
        $stream = $exception.Response.GetResponseStream()
        $reader = [System.IO.StreamReader]::new($stream)
        return $reader.ReadToEnd()
    } catch {
        return $exception.Message
    }
}

function Test-TransientStoreApiError {
    param([Parameter(Mandatory = $true)] $ErrorRecord)

    $response = $ErrorRecord.Exception.Response
    if ($response -and $response.StatusCode) {
        $statusCode = [int]$response.StatusCode
        return $statusCode -eq 408 -or $statusCode -eq 429 -or $statusCode -ge 500
    }

    # No HTTP response usually means a network-level failure or timeout.
    return $true
}

function Invoke-StoreApi {
    param(
        [Parameter(Mandatory = $true)]
        [ValidateSet("GET", "POST", "PUT", "DELETE")]
        [string] $Method,

        [Parameter(Mandatory = $true)]
        [string] $Path,

        $Body = $null,

        [int] $MaxAttempts = 0,

        [int] $RetryDelaySeconds = 20
    )

    if ($MaxAttempts -lt 1) {
        # GET and PUT are safe to repeat blindly; POST/DELETE callers that need
        # retries must verify server-side state between attempts themselves.
        $MaxAttempts = if ($Method -eq "GET" -or $Method -eq "PUT") { 3 } else { 1 }
    }

    $headers = @{
        Authorization = "Bearer $script:accessToken"
        Accept = "application/json"
    }
    $uri = "$baseUri/$Path"

    # The Ingestion API rejects bodyless non-GET requests with
    # InvalidParameterValue "Only JSON content is accepted" (target: mediaType),
    # but sending an empty JSON object is not safe either: the create-submission
    # endpoint validates it as submission data and rejects it with "The size of
    # Listings must be 1 or more". Send an EMPTY body with a JSON content type,
    # which satisfies the media-type check without submitting any content.
    $sendEmptyJsonBody = ($null -eq $Body -and $Method -ne "GET")

    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        try {
            if ($sendEmptyJsonBody) {
                return Invoke-RestMethod `
                    -Method $Method `
                    -Uri $uri `
                    -Headers $headers `
                    -ContentType "application/json" `
                    -Body "" `
                    -TimeoutSec 300
            }

            if ($null -eq $Body) {
                return Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers -TimeoutSec 300
            }

            return Invoke-RestMethod `
                -Method $Method `
                -Uri $uri `
                -Headers $headers `
                -ContentType "application/json" `
                -Body (ConvertTo-JsonBody $Body) `
                -TimeoutSec 300
        } catch {
            $responseBody = Read-ErrorResponseBody $_
            $message = "Microsoft Store API $Method $uri failed: $responseBody"

            if ($attempt -lt $MaxAttempts -and (Test-TransientStoreApiError $_)) {
                Write-Warning "$message (attempt $attempt of $MaxAttempts, retrying in $($RetryDelaySeconds * $attempt) seconds)"
                Start-Sleep -Seconds ($RetryDelaySeconds * $attempt)
                continue
            }

            throw $message
        }
    }
}

function Get-AccessToken {
    $body = @{
        grant_type = "client_credentials"
        client_id = $ClientId
        client_secret = $ClientSecret
        resource = "https://manage.devcenter.microsoft.com"
    }

    try {
        $response = Invoke-RestMethod `
            -Method Post `
            -Uri "https://login.microsoftonline.com/$TenantId/oauth2/token" `
            -ContentType "application/x-www-form-urlencoded" `
            -Body $body

        return $response.access_token
    } catch {
        $responseBody = Read-ErrorResponseBody $_
        throw "Failed to obtain Microsoft Store access token: $responseBody"
    }
}

function Get-AppSubmission {
    param([Parameter(Mandatory = $true)] [string] $SubmissionId)

    return Invoke-StoreApi -Method GET -Path "applications/$ApplicationId/submissions/$SubmissionId"
}

function Get-SubmissionStatus {
    param([Parameter(Mandatory = $true)] [string] $SubmissionId)

    return Invoke-StoreApi -Method GET -Path "applications/$ApplicationId/submissions/$SubmissionId/status"
}

function Get-PackageRollout {
    param([Parameter(Mandatory = $true)] [string] $SubmissionId)

    return Invoke-StoreApi -Method GET -Path "applications/$ApplicationId/submissions/$SubmissionId/packagerollout"
}

function Invoke-SubmissionCommit {
    param([Parameter(Mandatory = $true)] [string] $SubmissionId)

    # Like finalizepackagerollout, the commit endpoint can time out at the gateway
    # while the commit still goes through, so check the submission status between
    # attempts before treating a failure as real.
    $maxAttempts = 3
    for ($attempt = 1; $attempt -le $maxAttempts; $attempt++) {
        try {
            Invoke-StoreApi -Method POST -Path "applications/$ApplicationId/submissions/$SubmissionId/commit" | Out-Null
            return
        } catch {
            Write-Warning "Commit attempt $attempt of ${maxAttempts} failed: $_"
        }

        Start-Sleep -Seconds 20
        $statusResponse = Get-SubmissionStatus -SubmissionId $SubmissionId
        if ($statusResponse.status -ne "PendingCommit") {
            Write-Host "Submission $SubmissionId status is $($statusResponse.status); commit was accepted."
            return
        }
    }

    throw "Could not commit Store submission $SubmissionId after $maxAttempts attempts."
}

function Wait-ForCommitToStartProcessing {
    param([Parameter(Mandatory = $true)] [string] $SubmissionId)

    $failureStatuses = @("CommitFailed", "PreProcessingFailed")
    $transientStatuses = @("PendingCommit", "CommitStarted")

    for ($attempt = 1; $attempt -le $StatusPollAttempts; $attempt++) {
        $statusResponse = Get-SubmissionStatus -SubmissionId $SubmissionId
        $status = $statusResponse.status
        Write-Host "Submission $SubmissionId status: $status"

        if ($failureStatuses -contains $status) {
            $details = ConvertTo-JsonBody $statusResponse.statusDetails
            throw "Submission $SubmissionId failed with status $status. Details: $details"
        }

        if ($transientStatuses -notcontains $status) {
            Write-Host "Submission $SubmissionId has moved past commit startup."
            return
        }

        if ($attempt -lt $StatusPollAttempts) {
            Start-Sleep -Seconds $StatusPollSeconds
        }
    }

    Write-Warning "Submission $SubmissionId is still in commit startup after polling. Check Partner Center for final status."
}
