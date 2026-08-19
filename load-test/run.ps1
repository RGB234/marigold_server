param(
  [ValidateSet("smoke", "load", "stress", "spike", "main", "db-read", "db-write", "db-mixed", "storage-upload", "storage-mixed")]
  [string]$Profile = "smoke",

  [string]$EnvFile = ".env",

  [switch]$PrometheusRemoteWrite
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $EnvFile)) {
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

$script = if ($Profile -eq "main") { "main.js" } else { "tests/$Profile.js" }
[Environment]::SetEnvironmentVariable("LOAD_TEST_PROFILE", $Profile, "Process")

$k6Args = @("run")

if ($PrometheusRemoteWrite) {
  if (-not $env:K6_PROMETHEUS_RW_SERVER_URL) {
    throw "K6_PROMETHEUS_RW_SERVER_URL is required when -PrometheusRemoteWrite is used."
  }

  if (-not $env:K6_PROMETHEUS_RW_TREND_STATS) {
    [Environment]::SetEnvironmentVariable(
      "K6_PROMETHEUS_RW_TREND_STATS",
      "p(90),p(95),p(99),avg,min,max",
      "Process"
    )
  }

  $k6Args += @("-o", "experimental-prometheus-rw")
}

$k6Args += $script
k6 @k6Args
