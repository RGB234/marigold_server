param(
  [string]$HostName = $env:LOAD_TEST_DB_HOST,
  [int]$Port = $(if ($env:LOAD_TEST_DB_PORT) { [int]$env:LOAD_TEST_DB_PORT } else { 3306 }),
  [string]$Database = $env:LOAD_TEST_DB_NAME,
  [string]$Username = $env:LOAD_TEST_DB_USERNAME,
  [string]$Password = $env:LOAD_TEST_DB_PASSWORD,
  [string]$EmailPrefix = $(if ($env:LOAD_TEST_SEED_EMAIL_PREFIX) { $env:LOAD_TEST_SEED_EMAIL_PREFIX } else { "loadtest-user-" }),
  [string]$EmailDomain = $(if ($env:LOAD_TEST_SEED_EMAIL_DOMAIN) { $env:LOAD_TEST_SEED_EMAIL_DOMAIN } else { "example.test" }),
  [string]$MySqlPath = $(if ($env:MYSQL_PATH) { $env:MYSQL_PATH } else { "mysql" }),
  [switch]$ConfirmTestDatabase
)

$ErrorActionPreference = "Stop"

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
