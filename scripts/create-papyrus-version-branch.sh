#!/usr/bin/env bash
# Create or refresh papyrus/<mc-version> with a full prepared Papyrus tree.
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <mc-version> [paper-ref] [papyrus-source-ref]" >&2
  exit 1
fi

MC_VERSION="$1"
PAPER_REF="${2:-$1}"
PAPYRUS_SOURCE_REF="${3:-origin/main}"
BRANCH="papyrus/${MC_VERSION}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

git fetch origin main
git fetch --depth 1 https://github.com/PaperMC/Paper.git "refs/tags/${PAPER_REF}:refs/tags/${PAPER_REF}" 2>/dev/null || true

SOURCE_SHA="$(git rev-parse "$PAPYRUS_SOURCE_REF")"
echo "==> Creating ${BRANCH} from Paper ${PAPER_REF} (overlay ${SOURCE_SHA})"

git checkout -B "$BRANCH" "$PAPYRUS_SOURCE_REF"
./scripts/prepare-papyrus-version.sh "$MC_VERSION" "$PAPER_REF" "$SOURCE_SHA"

git add -A
if git diff --cached --quiet; then
  echo "No changes to commit on ${BRANCH}"
else
  git commit -m "$(cat <<EOF
Papyrus ${MC_VERSION} full overlay from main.

Prepared from Paper ${PAPER_REF} with Papyrus branding, anticheat, and config.
EOF
)"
fi

git push -u origin "$BRANCH" --force-with-lease
git checkout main
echo "==> ${BRANCH} pushed"
