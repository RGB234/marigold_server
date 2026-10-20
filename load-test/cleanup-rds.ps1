param(
  [string]$EnvFile,
  [string]$HostName,
  [int]$Port,
  [string]$Database,
  [string]$Username,
  [string]$Password,
  [string]$EmailPrefix,
  [string]$EmailDomain,
  [string]$MySqlPath,
  [switch]$ConfirmTestDatabase
)

$ErrorActionPreference = "Stop"
$script:CleanupParameters = @{} + $PSBoundParameters

if ([string]::IsNullOrWhiteSpace($EnvFile)) {
  throw "-EnvFile is required."
}

if (-not (Test-Path -LiteralPath $EnvFile -PathType Leaf)) {
  throw "Env file not found: $EnvFile"
}

Get-Content -LiteralPath $EnvFile -Encoding UTF8 | ForEach-Object {
  $line = $_.Trim()
  if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
    $parts = $line -split "=", 2
    $name = $parts[0].Trim()
    $value = $parts[1].Trim().Trim('"').Trim("'")
    if ($name) {
      [Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
  }
}

function Resolve-Value([string]$ParameterName, [string]$EnvName, $DefaultValue) {
  if ($script:CleanupParameters.ContainsKey($ParameterName)) {
    return $script:CleanupParameters[$ParameterName]
  }

  $envValue = [Environment]::GetEnvironmentVariable($EnvName, "Process")
  if (-not [string]::IsNullOrWhiteSpace($envValue)) {
    return $envValue
  }

  return $DefaultValue
}

$HostName = Resolve-Value "HostName" "LOAD_TEST_DB_HOST" $null
$Port = [int](Resolve-Value "Port" "LOAD_TEST_DB_PORT" 3306)
$Database = Resolve-Value "Database" "LOAD_TEST_DB_NAME" $null
$Username = Resolve-Value "Username" "LOAD_TEST_DB_USERNAME" $null
$Password = Resolve-Value "Password" "LOAD_TEST_DB_PASSWORD" $null
$EmailPrefix = Resolve-Value "EmailPrefix" "LOAD_TEST_SEED_EMAIL_PREFIX" "loadtest-user-"
$EmailDomain = Resolve-Value "EmailDomain" "LOAD_TEST_SEED_EMAIL_DOMAIN" "example.test"
$MySqlPath = Resolve-Value "MySqlPath" "MYSQL_PATH" "mysql"

if (-not $ConfirmTestDatabase) {
  throw "This script deletes test data. Re-run with -ConfirmTestDatabase after confirming the target is not production."
}

foreach ($entry in @(
  @{ Name = "HostName"; Value = $HostName },
  @{ Name = "Database"; Value = $Database },
  @{ Name = "Username"; Value = $Username },
  @{ Name = "Password"; Value = $Password }
)) {
  if ([string]::IsNullOrWhiteSpace($entry.Value)) {
    throw "$($entry.Name) is required."
  }
}

function ConvertTo-SqlLiteral([string]$Value) {
  if ($null -eq $Value) {
    return "NULL"
  }
  return "'" + $Value.Replace("\", "\\").Replace("'", "''") + "'"
}

function Invoke-MySqlSql([string]$Sql) {
  $previousPassword = $env:MYSQL_PWD
  try {
    $env:MYSQL_PWD = $Password
    $Sql | & $MySqlPath `
      --host=$HostName `
      --port=$Port `
      --user=$Username `
      --database=$Database `
      --default-character-set=utf8mb4 `
      --protocol=tcp

    if ($LASTEXITCODE -ne 0) {
      throw "mysql exited with code $LASTEXITCODE."
    }
  } finally {
    $env:MYSQL_PWD = $previousPassword
  }
}

$sqlPath = Join-Path $PSScriptRoot "db\cleanup-adoption-data.sql"
$cleanupSql = Get-Content -LiteralPath $sqlPath -Encoding UTF8 -Raw

$sqlHeader = @"
SET @load_test_email_prefix := $(ConvertTo-SqlLiteral $EmailPrefix);
SET @load_test_email_domain := $(ConvertTo-SqlLiteral $EmailDomain);

"@

Invoke-MySqlSql ($sqlHeader + $cleanupSql)

Write-Host "Cleaned load-test data from $Database@$HostName."
