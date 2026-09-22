[CmdletBinding()]
param(
  [string]$Image = 'family-kitchen:latest',
  [string]$ContainerName = 'family-kitchen',
  [string]$EnvFile = '.\family-kitchen.env',
  [string]$DataVolume = 'family-kitchen-data',
  [string]$BindAddress = '127.0.0.1',
  [ValidateRange(1, 65535)]
  [int]$Port = 8080
)

$ErrorActionPreference = 'Stop'

function Assert-NotBlank {
  param([string]$Name, [string]$Value)
  if ([string]::IsNullOrWhiteSpace($Value)) {
    throw "$Name cannot be empty."
  }
}

Assert-NotBlank -Name 'Image' -Value $Image
Assert-NotBlank -Name 'ContainerName' -Value $ContainerName
Assert-NotBlank -Name 'DataVolume' -Value $DataVolume
Assert-NotBlank -Name 'BindAddress' -Value $BindAddress

$docker = Get-Command docker -ErrorAction SilentlyContinue
if (-not $docker) {
  throw 'Docker CLI was not found on PATH.'
}

& $docker.Source info *> $null
if ($LASTEXITCODE -ne 0) {
  throw 'Docker daemon is unavailable.'
}

$resolvedEnvFile = (Resolve-Path -LiteralPath $EnvFile -ErrorAction Stop).Path
$existingContainer = & $docker.Source container ls -a --filter "name=^/${ContainerName}$" --format '{{.Names}}'
if ($existingContainer -eq $ContainerName) {
  throw "Container '$ContainerName' already exists. Remove or rename it before deployment."
}

& $docker.Source run -d `
  --name $ContainerName `
  --restart unless-stopped `
  --stop-timeout 90 `
  --publish "${BindAddress}:${Port}:8080" `
  --env-file $resolvedEnvFile `
  --mount "type=volume,source=${DataVolume},target=/data" `
  $Image

if ($LASTEXITCODE -ne 0) {
  throw "Container startup failed with exit code $LASTEXITCODE."
}

Write-Host "Container: $ContainerName"
Write-Host "Address:   http://${BindAddress}:${Port}/"
Write-Host "Data:      Docker volume '$DataVolume' mounted at /data"
Write-Host "Logs:      docker logs -f $ContainerName"
