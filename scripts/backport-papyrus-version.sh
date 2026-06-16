#!/usr/bin/env bash
# Deprecated: use prepare-papyrus-version.sh (all versions build from main in CI).
set -euo pipefail
echo "backport-papyrus-version.sh is deprecated; use scripts/prepare-papyrus-version.sh" >&2
exec "$(dirname "$0")/prepare-papyrus-version.sh" "$@"
