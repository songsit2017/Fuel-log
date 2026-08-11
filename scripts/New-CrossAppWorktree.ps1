[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-z0-9][a-z0-9-]*$')]
    [string]$FeatureSlug,

    [ValidateSet('agent', 'feature')]
    [string]$BranchPrefix = 'agent',

    [string]$PuPocketRepository = 'D:\App Projects\PU Pocket',
    [string]$FuelLogRepository = 'D:\App Projects\Fuel-log',
    [string]$WorktreeRoot = 'D:\App Projects\worktrees',

    [switch]$PlanOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Invoke-Git {
    param(
        [Parameter(Mandatory = $true)][string]$Repository,
        [Parameter(Mandatory = $true)][string[]]$Arguments
    )

    $output = & git -C $Repository @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "git -C '$Repository' $($Arguments -join ' ') failed:`n$($output -join [Environment]::NewLine)"
    }
    return $output
}

function Test-GitRef {
    param(
        [Parameter(Mandatory = $true)][string]$Repository,
        [Parameter(Mandatory = $true)][string]$Ref
    )

    & git -C $Repository show-ref --verify --quiet $Ref
    $exitCode = $LASTEXITCODE
    if ($exitCode -eq 0) { return $true }
    if ($exitCode -eq 1) { return $false }
    throw "Could not inspect Git ref '$Ref' in '$Repository'."
}

function Test-GitIgnored {
    param(
        [Parameter(Mandatory = $true)][string]$Repository,
        [Parameter(Mandatory = $true)][string]$RelativePath
    )

    & git -C $Repository check-ignore --quiet -- $RelativePath
    $exitCode = $LASTEXITCODE
    if ($exitCode -eq 0) { return $true }
    if ($exitCode -eq 1) { return $false }
    throw "Could not inspect ignore rules for '$RelativePath' in '$Repository'."
}

$branch = "$BranchPrefix/$FeatureSlug"
$featureRoot = Join-Path ([IO.Path]::GetFullPath($WorktreeRoot)) $FeatureSlug
$repositories = @(
    [pscustomobject]@{
        Name = 'PU-Pocket'
        Repository = [IO.Path]::GetFullPath($PuPocketRepository)
        Target = Join-Path $featureRoot 'PU-Pocket'
        LocalBuildFiles = @('local.properties')
    },
    [pscustomobject]@{
        Name = 'Fuel-log'
        Repository = [IO.Path]::GetFullPath($FuelLogRepository)
        Target = Join-Path $featureRoot 'Fuel-log'
        LocalBuildFiles = @('native-kotlin/app/google-services.json')
    }
)

# Validate both repositories completely before creating either worktree. This
# avoids leaving half of a cross-app feature behind after a predictable error.
foreach ($item in $repositories) {
    if (-not (Test-Path -LiteralPath $item.Repository -PathType Container)) {
        throw "$($item.Name) repository was not found at '$($item.Repository)'."
    }
    $insideWorktree = @(Invoke-Git -Repository $item.Repository -Arguments @('rev-parse', '--is-inside-work-tree'))
    if (($insideWorktree -join '').Trim() -ne 'true') {
        throw "'$($item.Repository)' is not a Git worktree."
    }
    $currentBranch = @(Invoke-Git -Repository $item.Repository -Arguments @('branch', '--show-current'))
    if (($currentBranch -join '').Trim() -ne 'develop') {
        throw "$($item.Name) control checkout must be on 'develop' before creating a feature worktree."
    }
    $status = @(Invoke-Git -Repository $item.Repository -Arguments @('status', '--porcelain'))
    if ($status.Count -gt 0) {
        throw "$($item.Name) control checkout is not clean. Commit or move its changes before creating a feature worktree."
    }

    Invoke-Git -Repository $item.Repository -Arguments @('fetch', 'origin', '--prune') | Out-Null
    if (-not (Test-GitRef -Repository $item.Repository -Ref 'refs/remotes/origin/develop')) {
        throw "$($item.Name) has no origin/develop integration branch."
    }
    if (Test-GitRef -Repository $item.Repository -Ref "refs/heads/$branch") {
        throw "$($item.Name) already has local branch '$branch'. Use its existing worktree or choose another slug."
    }
    if (Test-GitRef -Repository $item.Repository -Ref "refs/remotes/origin/$branch") {
        throw "$($item.Name) already has remote branch '$branch'. Resume that feature instead of creating a duplicate."
    }
    if (Test-Path -LiteralPath $item.Target) {
        throw "Target path already exists: '$($item.Target)'."
    }
    foreach ($relativePath in $item.LocalBuildFiles) {
        $source = Join-Path $item.Repository $relativePath
        if (-not (Test-Path -LiteralPath $source -PathType Leaf)) {
            throw "$($item.Name) requires local build file '$relativePath' before creating a worktree."
        }
        if (-not (Test-GitIgnored -Repository $item.Repository -RelativePath $relativePath)) {
            throw "Refusing to copy '$relativePath' because it is not ignored by Git in $($item.Name)."
        }
    }
}

$plan = $repositories | Select-Object Name, Repository, @{Name = 'Branch'; Expression = { $branch }}, Target
if ($PlanOnly) {
    $plan | Format-Table -AutoSize
    return
}

New-Item -ItemType Directory -Path $featureRoot -Force | Out-Null
foreach ($item in $repositories) {
    Invoke-Git -Repository $item.Repository -Arguments @(
        'worktree', 'add', '-b', $branch, $item.Target, 'origin/develop'
    ) | Out-Host
    foreach ($relativePath in $item.LocalBuildFiles) {
        $source = Join-Path $item.Repository $relativePath
        $destination = Join-Path $item.Target $relativePath
        New-Item -ItemType Directory -Path (Split-Path -Parent $destination) -Force | Out-Null
        Copy-Item -LiteralPath $source -Destination $destination
    }
}

$plan | Format-Table -AutoSize
Write-Host "Created paired worktrees. Open the two Target folders above for feature '$FeatureSlug'."
