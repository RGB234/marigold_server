param(
  [string]$HostName = $env:LOAD_TEST_DB_HOST,
  [int]$Port = $(if ($env:LOAD_TEST_DB_PORT) { [int]$env:LOAD_TEST_DB_PORT } else { 3306 }),
  [string]$Database = $env:LOAD_TEST_DB_NAME,
  [string]$Username = $env:LOAD_TEST_DB_USERNAME,
  [string]$Password = $env:LOAD_TEST_DB_PASSWORD,
  [int]$UserCount = $(if ($env:LOAD_TEST_SEED_USER_COUNT) { [int]$env:LOAD_TEST_SEED_USER_COUNT } else { 50 }),
  [int]$PostCount = $(if ($env:LOAD_TEST_SEED_ADOPTION_POST_COUNT) { [int]$env:LOAD_TEST_SEED_ADOPTION_POST_COUNT } else { 5000 }),
  [int]$CommentsPerPost = $(if ($env:LOAD_TEST_SEED_COMMENTS_PER_POST) { [int]$env:LOAD_TEST_SEED_COMMENTS_PER_POST } else { 3 }),
  [int]$ChatRoomCount = $(if ($env:LOAD_TEST_SEED_CHAT_ROOM_COUNT) { [int]$env:LOAD_TEST_SEED_CHAT_ROOM_COUNT } else { $UserCount }),
  [Int64]$UserIdBase = $(if ($env:LOAD_TEST_SEED_USER_ID_BASE) { [Int64]$env:LOAD_TEST_SEED_USER_ID_BASE } else { 990000000000000000 }),
  [Int64]$PostIdBase = $(if ($env:LOAD_TEST_SEED_POST_ID_BASE) { [Int64]$env:LOAD_TEST_SEED_POST_ID_BASE } else { 991000000000000000 }),
  [Int64]$ImageIdBase = $(if ($env:LOAD_TEST_SEED_IMAGE_ID_BASE) { [Int64]$env:LOAD_TEST_SEED_IMAGE_ID_BASE } else { 992000000000000000 }),
  [Int64]$ChatRoomIdBase = $(if ($env:LOAD_TEST_SEED_CHAT_ROOM_ID_BASE) { [Int64]$env:LOAD_TEST_SEED_CHAT_ROOM_ID_BASE } else { 993000000000000000 }),
  [Int64]$RoomParticipantIdBase = $(if ($env:LOAD_TEST_SEED_ROOM_PARTICIPANT_ID_BASE) { [Int64]$env:LOAD_TEST_SEED_ROOM_PARTICIPANT_ID_BASE } else { 994000000000000000 }),
  [string]$EmailPrefix = $(if ($env:LOAD_TEST_SEED_EMAIL_PREFIX) { $env:LOAD_TEST_SEED_EMAIL_PREFIX } else { "loadtest-user-" }),
  [string]$EmailDomain = $(if ($env:LOAD_TEST_SEED_EMAIL_DOMAIN) { $env:LOAD_TEST_SEED_EMAIL_DOMAIN } else { "example.test" }),
  [string]$NicknamePrefix = $(if ($env:LOAD_TEST_SEED_NICKNAME_PREFIX) { $env:LOAD_TEST_SEED_NICKNAME_PREFIX } else { "loadtest-user-" }),
  [string]$ImagePrefix = $(if ($env:LOAD_TEST_SEED_IMAGE_PREFIX) { $env:LOAD_TEST_SEED_IMAGE_PREFIX } else { "loadtest/adoption" }),
  [string]$PasswordHash = $env:LOAD_TEST_PASSWORD_HASH,
  [string]$MySqlPath = $(if ($env:MYSQL_PATH) { $env:MYSQL_PATH } else { "mysql" }),
  [switch]$ConfirmTestDatabase
)

$ErrorActionPreference = "Stop"

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
