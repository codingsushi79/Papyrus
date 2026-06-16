#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 2 ]]; then
  echo "Usage: $0 <mc-version> <git-ref>" >&2
  exit 1
fi

MC_VERSION="$1"
REF="$2"
BRANCH="papyrus/${MC_VERSION}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if ! git rev-parse --verify "$REF" >/dev/null 2>&1; then
  echo "Unknown git ref: $REF" >&2
  exit 1
fi

echo "==> Backporting Papyrus to ${MC_VERSION} from ${REF}"

git checkout -B "$BRANCH" "$REF"
git reset --hard
git clean -fd
git checkout main -- scripts/backport-papyrus-version.sh scripts/merge-papyrus-patch-hooks.py

# Remove leftover mache/build dirs so paper-* renames are not skipped.
rm -rf papyrus-server papyrus-api papyrus-generator 2>/dev/null || true

if [[ -d paper-api ]]; then
  git mv paper-api papyrus-api
fi
if [[ -d paper-server ]]; then
  git mv paper-server papyrus-server
fi
if [[ -d paper-generator ]]; then
  git mv paper-generator papyrus-generator
  if [[ -f paper-generator.settings.gradle.kts && ! -f papyrus-generator.settings.gradle.kts ]]; then
    git mv paper-generator.settings.gradle.kts papyrus-generator.settings.gradle.kts
  fi
fi

git checkout main -- settings.gradle.kts scripts/start.sh SUPPORTED_VERSIONS.txt

PAPYRUS_PATHS=(
  papyrus-api/build.gradle.kts
  papyrus-api/src/main/java/io/papermc/paper/ServerBuildInfo.java
  papyrus-api/src/main/java/io/papermc/paper/event/player/PlayerAnticheatViolationEvent.java
  papyrus-api/src/main/javadoc/overview.html
  papyrus-server/src/main/java/io/papermc/paper/anticheat
  papyrus-server/src/main/java/io/papermc/paper/util/EntityRandomSources.java
  papyrus-server/src/main/java/io/papermc/paper/util/PapyrusPerformance.java
  papyrus-server/src/main/java/io/papermc/paper/configuration/transformation/global/versioned/V32_EntityRandomSource.java
  papyrus-server/src/main/java/io/papermc/paper/configuration/transformation/global/versioned/V33_PapyrusPerformanceOptions.java
  papyrus-server/src/main/java/io/papermc/paper/configuration/transformation/global/versioned/V34_PapyrusAnticheatEngine.java
  papyrus-server/src/main/java/io/papermc/paper/configuration/transformation/global/versioned/V35_PapyrusClientIntegrity.java
  papyrus-server/src/main/java/io/papermc/paper/configuration/transformation/world/versioned/V32_ExperienceOrbOptions.java
  papyrus-server/src/main/java/io/papermc/paper/ServerBuildInfoImpl.java
  papyrus-server/src/main/java/com/destroystokyo/paper/PaperVersionFetcher.java
  papyrus-server/src/main/java/io/papermc/paper/configuration/GlobalConfiguration.java
  papyrus-server/src/main/java/io/papermc/paper/configuration/PaperConfigurations.java
  papyrus-server/src/main/java/io/papermc/paper/configuration/WorldConfiguration.java
  test-plugin/build.gradle.kts
  test-plugin/src/main/resources/paper-plugin.yml
)

git checkout main -- "${PAPYRUS_PATHS[@]}"

PATCH_FILES=(
  net/minecraft/server/network/ServerGamePacketListenerImpl.java.patch
  net/minecraft/server/level/ServerPlayerGameMode.java.patch
  net/minecraft/server/players/PlayerList.java.patch
  net/minecraft/world/entity/Entity.java.patch
  net/minecraft/world/entity/ExperienceOrb.java.patch
)

for patch_file in "${PATCH_FILES[@]}"; do
  main_patch="$(mktemp)"
  git show "main:papyrus-server/patches/sources/${patch_file}" > "$main_patch"
  python3 scripts/merge-papyrus-patch-hooks.py \
    "$main_patch" \
    "papyrus-server/patches/sources/${patch_file}"
  rm -f "$main_patch"
done

rm -f paper-server
ln -s papyrus-server paper-server
git add -f paper-server

python3 - <<'PY'
from pathlib import Path

main_java = Path("papyrus-server/src/main/java/org/bukkit/craftbukkit/Main.java")
text = main_java.read_text()
old = 'if (System.getProperty("jdk.nio.maxCachedBufferSize") == null) System.setProperty("jdk.nio.maxCachedBufferSize", "262144"); // Paper - cap per-thread NIO cache size; https://www.evanjones.ca/java-bytebuffer-leak.html'
new = "io.papermc.paper.util.PapyrusPerformance.applyRuntimeDefaults(); // Papyrus - runtime performance defaults"
if old in text:
    main_java.write_text(text.replace(old, new, 1))
elif new not in text:
    raise SystemExit("Could not apply PapyrusPerformance hook in Main.java")
PY

python3 - <<'PY'
from pathlib import Path

path = Path("papyrus-server/build.gradle.kts")
text = path.read_text()
replacements = {
    '"Implementation-Title" to "Paper"': '"Implementation-Title" to "Papyrus"',
    '"Implementation-Vendor" to date': '"Implementation-Vendor" to "SushiMC"',
    '"Specification-Title" to "Paper"': '"Specification-Title" to "Papyrus"',
    '"Specification-Vendor" to "Paper Team"': '"Specification-Vendor" to "SushiMC"',
    '"Brand-Id" to "papermc:paper"': '"Brand-Id" to "sushimc:papyrus"',
    '"Brand-Name" to "Paper"': '"Brand-Name" to "Papyrus"',
    'project("paper")': 'project("papyrus")',
}
for old, new in replacements.items():
    text = text.replace(old, new)

if "syncPapyrusPaperclipJar" not in text:
    paperclip_task = "createMojmapPaperclipJar" if "createMojmapPaperclipJar" in text else "createPaperclipJar"
    sync_task = f'''
val syncPapyrusPaperclipJar = tasks.register<Copy>("syncPapyrusPaperclipJar") {{
    group = "build"
    description = "Copy the paperclip jar using Papyrus naming"
    val archiveVersion = project.version.toString()
    dependsOn(tasks.{paperclip_task})
    from(tasks.{paperclip_task}.flatMap {{ it.outputZip }})
    into(layout.buildDirectory.dir("distributions"))
    rename {{ "papyrus-paperclip-$archiveVersion.jar" }}
}}
'''
    text = text.rstrip() + "\n" + sync_task

path.write_text(text)
PY

sed -i '' \
  -e "s/^mcVersion=.*/mcVersion=${MC_VERSION}/" \
  -e "s/^apiVersion=.*/apiVersion=${MC_VERSION}/" \
  gradle.properties

git checkout main -- README.md LICENSE.md CONTRIBUTING.md SECURITY.md .editorconfig .gitignore

git add -A
git commit -m "$(cat <<EOF
Backport full Papyrus features to Minecraft ${MC_VERSION}.

Includes Papyrus branding, integrated anticheat, client integrity checks,
performance options, and config migrations on top of Paper ${MC_VERSION}.
EOF
)"

echo "==> Created ${BRANCH}"
