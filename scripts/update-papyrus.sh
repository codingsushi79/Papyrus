#!/usr/bin/env bash
# Download a newer Papyrus release jar for this server's Minecraft version, if available.
# Writes the download next to the running jar as <jarname>.update for start.sh to apply.

set -euo pipefail

JAR="${1:?Usage: $0 <papyrus-jar>}"
REPO="codingsushi79/Papyrus"
API="https://api.github.com/repos/${REPO}"

if [[ ! -f "$JAR" ]]; then
  echo "Jar not found: $JAR" >&2
  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required for Papyrus auto-update" >&2
  exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required for Papyrus auto-update" >&2
  exit 1
fi

if ! command -v unzip >/dev/null 2>&1; then
  echo "unzip is required for Papyrus auto-update" >&2
  exit 1
fi

manifest="$(unzip -p "$JAR" META-INF/MANIFEST.MF 2>/dev/null || true)"
if [[ -z "$manifest" ]]; then
  echo "Could not read manifest from $JAR" >&2
  exit 1
fi

implementation_version="$(printf '%s\n' "$manifest" | awk -F': ' '/^Implementation-Version:/ {print $2; exit}' | tr -d '\r')"
git_commit="$(printf '%s\n' "$manifest" | awk -F': ' '/^Git-Commit:/ {print $2; exit}' | tr -d '\r')"
mc_version="${implementation_version%%-*}"

if [[ -z "$mc_version" || -z "$git_commit" ]]; then
  echo "Jar is missing Implementation-Version or Git-Commit metadata; skipping update check." >&2
  exit 0
fi

asset_name="Papyrus-${mc_version}.jar"
pending="${JAR}.update"
user_agent="Papyrus-startup/${implementation_version} (auto-update)"

if [[ -f "$pending" ]]; then
  echo "Pending Papyrus update already present at ${pending}; restart via scripts/start.sh to apply."
  exit 0
fi

release_json="$(curl -fsSL \
  -H "Accept: application/vnd.github+json" \
  -H "User-Agent: ${user_agent}" \
  "${API}/releases/latest")"

tag_name="$(jq -r '.tag_name' <<<"$release_json")"
download_url="$(jq -r --arg name "$asset_name" '.assets[] | select(.name == $name) | .browser_download_url' <<<"$release_json")"
expected_size="$(jq -r --arg name "$asset_name" '.assets[] | select(.name == $name) | .size' <<<"$release_json")"

if [[ -z "$download_url" || "$download_url" == "null" ]]; then
  echo "No release asset ${asset_name} found in ${tag_name}; skipping update."
  exit 0
fi

compare_json="$(curl -fsSL \
  -H "Accept: application/vnd.github+json" \
  -H "User-Agent: ${user_agent}" \
  "${API}/compare/${git_commit}...${tag_name}")"

status="$(jq -r '.status' <<<"$compare_json")"
behind_by="$(jq -r '.behind_by // 0' <<<"$compare_json")"

case "$status" in
  identical)
    echo "Papyrus is up to date (${tag_name})."
    exit 0
    ;;
  ahead)
    echo "Running a build ahead of release ${tag_name}; skipping update."
    exit 0
    ;;
  behind)
    if [[ "$behind_by" -le 0 ]]; then
      echo "Papyrus is up to date (${tag_name})."
      exit 0
    fi
    ;;
  *)
    echo "Could not compare to release ${tag_name}; skipping update." >&2
    exit 0
    ;;
esac

echo "Downloading Papyrus ${tag_name} for Minecraft ${mc_version} (${behind_by} commit(s) newer)..."
tmp="${pending}.part"
curl -fsSL \
  -H "Accept: application/octet-stream" \
  -H "User-Agent: ${user_agent}" \
  -o "$tmp" \
  "$download_url"

if [[ "$expected_size" != "null" && -n "$expected_size" ]]; then
  actual_size="$(wc -c < "$tmp" | tr -d ' ')"
  if [[ "$actual_size" != "$expected_size" ]]; then
    rm -f "$tmp"
    echo "Download size mismatch for ${tag_name}" >&2
    exit 1
  fi
fi

mv -f "$tmp" "$pending"
echo "Downloaded ${tag_name} to ${pending}. Restart the server to apply the update."
