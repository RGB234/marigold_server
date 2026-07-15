param(
  [string]$EnvFile = ".env",
  [string]$HostName,
  [int]$Port,
  [string]$Database,
  [string]$Username,
  [string]$Password,
  [int]$UserCount,
  [int]$PostCount,
  [int]$CommentsPerPost,
  [int]$ChatRoomCount,
  [Int64]$UserIdBase,
  [Int64]$PostIdBase,
  [Int64]$ImageIdBase,
  [Int64]$ChatRoomIdBase,
  [Int64]$RoomParticipantIdBase,
  [string]$EmailPrefix,
  [string]$EmailDomain,
  [string]$NicknamePrefix,
  [string]$ImagePrefix,
  [string]$PasswordHash,
  [string]$MySqlPath,
  [switch]$ConfirmTestDatabase
)

$ErrorActionPreference = "Stop"

function Import-EnvFile([string]$Path) {
  if ([string]::IsNullOrWhiteSpace($Path)) {
    return
  }

  if (-not (Test-Path -LiteralPath $Path)) {
    if ($PSBoundParameters.ContainsKey("EnvFile")) {
      throw "Env file not found: $Path"
    }
    return
  }

  Get-Content -LiteralPath $Path -Encoding UTF8 | ForEach-Object {
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
}

function Resolve-Value([string]$ParameterName, [string]$EnvName, $DefaultValue) {
  if ($PSBoundParameters.ContainsKey($ParameterName)) {
    return Get-Variable -Name $ParameterName -ValueOnly
  }

  $envValue = [Environment]::GetEnvironmentVariable($EnvName, "Process")
  if (-not [string]::IsNullOrWhiteSpace($envValue)) {
    return $envValue
  }

  return $DefaultValue
}

Import-EnvFile $EnvFile

$HostName = Resolve-Value "HostName" "LOAD_TEST_DB_HOST" $null
$Port = [int](Resolve-Value "Port" "LOAD_TEST_DB_PORT" 3306)
$Database = Resolve-Value "Database" "LOAD_TEST_DB_NAME" $null
$Username = Resolve-Value "Username" "LOAD_TEST_DB_USERNAME" $null
$Password = Resolve-Value "Password" "LOAD_TEST_DB_PASSWORD" $null
$UserCount = [int](Resolve-Value "UserCount" "LOAD_TEST_SEED_USER_COUNT" 50)
$PostCount = [int](Resolve-Value "PostCount" "LOAD_TEST_SEED_ADOPTION_POST_COUNT" 5000)
$CommentsPerPost = [int](Resolve-Value "CommentsPerPost" "LOAD_TEST_SEED_COMMENTS_PER_POST" 3)
$ChatRoomCount = [int](Resolve-Value "ChatRoomCount" "LOAD_TEST_SEED_CHAT_ROOM_COUNT" $UserCount)
$UserIdBase = [Int64](Resolve-Value "UserIdBase" "LOAD_TEST_SEED_USER_ID_BASE" 990000000000000000)
$PostIdBase = [Int64](Resolve-Value "PostIdBase" "LOAD_TEST_SEED_POST_ID_BASE" 991000000000000000)
$ImageIdBase = [Int64](Resolve-Value "ImageIdBase" "LOAD_TEST_SEED_IMAGE_ID_BASE" 992000000000000000)
$ChatRoomIdBase = [Int64](Resolve-Value "ChatRoomIdBase" "LOAD_TEST_SEED_CHAT_ROOM_ID_BASE" 993000000000000000)
$RoomParticipantIdBase = [Int64](Resolve-Value "RoomParticipantIdBase" "LOAD_TEST_SEED_ROOM_PARTICIPANT_ID_BASE" 994000000000000000)
$EmailPrefix = Resolve-Value "EmailPrefix" "LOAD_TEST_SEED_EMAIL_PREFIX" "loadtest-user-"
$EmailDomain = Resolve-Value "EmailDomain" "LOAD_TEST_SEED_EMAIL_DOMAIN" "example.test"
$NicknamePrefix = Resolve-Value "NicknamePrefix" "LOAD_TEST_SEED_NICKNAME_PREFIX" "loadtest-user-"
$ImagePrefix = Resolve-Value "ImagePrefix" "LOAD_TEST_SEED_IMAGE_PREFIX" "loadtest/adoption"
$PasswordHash = Resolve-Value "PasswordHash" "LOAD_TEST_PASSWORD_HASH" $null
$MySqlPath = Resolve-Value "MySqlPath" "MYSQL_PATH" "mysql"

if (-not $ConfirmTestDatabase) {
  throw "This script writes test data. Re-run with -ConfirmTestDatabase after confirming the target is not production."
}

foreach ($entry in @(
  @{ Name = "HostName"; Value = $HostName },
  @{ Name = "Database"; Value = $Database },
  @{ Name = "Username"; Value = $Username },
  @{ Name = "Password"; Value = $Password },
  @{ Name = "PasswordHash"; Value = $PasswordHash }
)) {
  if ([string]::IsNullOrWhiteSpace($entry.Value)) {
    throw "$($entry.Name) is required."
  }
}

if ($UserCount -lt 2 -or $UserCount -gt 100000) {
  throw "UserCount must be between 2 and 100000."
}

if ($PostCount -lt 1 -or $PostCount -gt 100000) {
  throw "PostCount must be between 1 and 100000."
}

if ($CommentsPerPost -lt 0 -or $CommentsPerPost -gt 100) {
  throw "CommentsPerPost must be between 0 and 100."
}

if ($ChatRoomCount -lt 1 -or $ChatRoomCount -gt 100000) {
  throw "ChatRoomCount must be between 1 and 100000."
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

$cleanupSqlPath = Join-Path $PSScriptRoot "db\cleanup-adoption-data.sql"
$seedSqlPath = Join-Path $PSScriptRoot "db\seed-adoption-data.sql"
$cleanupSql = Get-Content -LiteralPath $cleanupSqlPath -Encoding UTF8 -Raw
$seedSql = Get-Content -LiteralPath $seedSqlPath -Encoding UTF8 -Raw

$sqlHeader = @"
SET @load_test_user_count := $UserCount;
SET @load_test_post_count := $PostCount;
SET @load_test_comments_per_post := $CommentsPerPost;
SET @load_test_chat_room_count := $ChatRoomCount;
SET @load_test_user_id_base := $UserIdBase;
SET @load_test_post_id_base := $PostIdBase;
SET @load_test_image_id_base := $ImageIdBase;
SET @load_test_chat_room_id_base := $ChatRoomIdBase;
SET @load_test_room_participant_id_base := $RoomParticipantIdBase;
SET @load_test_email_prefix := $(ConvertTo-SqlLiteral $EmailPrefix);
SET @load_test_email_domain := $(ConvertTo-SqlLiteral $EmailDomain);
SET @load_test_nickname_prefix := $(ConvertTo-SqlLiteral $NicknamePrefix);
SET @load_test_image_prefix := $(ConvertTo-SqlLiteral $ImagePrefix);
SET @load_test_password_hash := $(ConvertTo-SqlLiteral $PasswordHash);

"@

Invoke-MySqlSql ($sqlHeader + $cleanupSql)
Invoke-MySqlSql ($sqlHeader + $seedSql)

$pageSize = if ($env:LOAD_TEST_ADOPTION_PAGE_SIZE) { [int]$env:LOAD_TEST_ADOPTION_PAGE_SIZE } else { 10 }
$totalPages = [Math]::Ceiling($PostCount / [double]$pageSize)

Write-Host "Seeded $UserCount users, $PostCount adoption posts, $ChatRoomCount chat rooms, and $($PostCount * $CommentsPerPost) adoption comments into $Database@$HostName."
Write-Host "Set LOAD_TEST_LOGIN_PASSWORD to the plaintext password matching LOAD_TEST_PASSWORD_HASH before running k6."
Write-Host "Set LOAD_TEST_ADOPTION_TOTAL_PAGES=$totalPages before running k6."
