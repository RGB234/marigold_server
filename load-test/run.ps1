param(
  [ValidateSet("smoke", "load", "stress", "spike", "main")]
  [string]$Profile = "smoke",

  [string]$EnvFile = ".env"
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

k6 run $script
