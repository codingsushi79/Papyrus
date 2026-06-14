# Papyrus

Papyrus is a Minecraft server software fork of [Paper](https://github.com/PaperMC/Paper). It keeps full compatibility with the Paper plugin ecosystem (`io.papermc.paper` API, Paper plugins, and existing configs) while adding first-class options for vanilla parity and performance tuning.

Repository: [github.com/codingsushi79/Papyrus](https://github.com/codingsushi79/Papyrus)

Documentation: [docs.sushii.dev/papyrus](https://docs.sushii.dev/papyrus/)

---

## Table of contents

- [What Papyrus adds](#what-papyrus-adds)
- [Requirements](#requirements)
- [Quick start](#quick-start)
- [Running in production](#running-in-production)
- [Configuration](#configuration)
- [Papyrus-specific options](#papyrus-specific-options)
- [Performance tuning](#performance-tuning)
- [Vanilla compatibility preset](#vanilla-compatibility-preset)
- [Redstone presets](#redstone-presets)
- [Building from source](#building-from-source)
- [Project structure](#project-structure)
- [Plugin development](#plugin-development)
- [Continuous integration](#continuous-integration)
- [Contributing](#contributing)
- [License](#license)
- [Credits](#credits)
- [FAQ](#faq)

---

## What Papyrus adds

Paper optimizes Minecraft in ways that sometimes diverge from vanilla behavior. Papyrus makes those trade-offs explicit and configurable instead of fixed.

| Area | Paper behavior | Papyrus change |
|------|----------------|----------------|
| Entity RNG | Shared random source across all entities (faster) | Configurable: `SHARED` or `VANILLA` per-entity RNG |
| Redstone | Vanilla by default; optional fast engines | Same engines, documented presets for vanilla vs tech servers |
| Experience orbs | Paper defaults | Configurable despawn, pickup radius, merge radius, and merge disable |
| Performance defaults | Paper defaults | Tuned defaults for chunk I/O, explosions, hoppers, idle worlds, JVM/Netty |
| Update checker | Checks PaperMC | Disabled by default (fork-specific builds) |

Everything else — Moonrise chunk system, incremental saves, hopper optimizations, plugin API, and the patch-based build — comes from upstream Paper unchanged.

---

## Requirements

### Running a server

- **Java 21+** to run the server jar (Java 25 recommended)
- **2 GB RAM minimum** for small servers; 8 GB+ recommended for production
- A machine capable of running a Paper-based server (same as Paper)

### Building from source

- **Git** — you must clone the repository; zip downloads will not build
- **Java 25 JDK** to compile (Gradle can auto-provision this via toolchains if you only have JRE 21+)
- **~4 GB RAM** for Gradle during compilation

Current Minecraft version: **26.1.2** (see `gradle.properties`).

---

## Quick start

### 1. Get the jar

**From releases:** Download `Papyrus-<mcVersion>.jar` (e.g. `Papyrus-26.1.2.jar`) from [GitHub Releases](https://github.com/codingsushi79/Papyrus/releases) or see [Documentation → Download](https://docs.sushii.dev/papyrus/).

**From CI:** Download the `papyrus-server` artifact from the latest successful [GitHub Actions](https://github.com/codingsushi79/Papyrus/actions) build.

**From source:**

```bash
git clone https://github.com/codingsushi79/Papyrus.git
cd Papyrus
./gradlew applyPatches createPaperclipJar syncPapyrusPaperclipJar
```

The runnable jar is at:

```
papyrus-server/build/distributions/papyrus-paperclip-<version>.jar
```

### 2. First run

```bash
mkdir papyrus-server && cd papyrus-server
cp ../Papyrus/papyrus-server/build/distributions/papyrus-paperclip-*.jar .
java -jar papyrus-paperclip-*.jar
```

Accept the EULA by editing `eula.txt`, then start again. Papyrus generates configs on first boot:

```
config/
  paper-global.yml          # Global server settings
  paper-world-defaults.yml  # Defaults for all worlds
world/
  paper-world.yml           # Per-world overrides (optional)
spigot.yml                  # Spigot settings (activation ranges, etc.)
bukkit.yml
server.properties
```

---

## Running in production

### Start script

Papyrus includes a production start script with recommended G1GC flags:

```bash
# Copy the jar into your server directory first
cp /path/to/papyrus-paperclip-*.jar ./papyrus-paperclip.jar

# From the repo (or copy scripts/start.sh into your server dir)
JAR=papyrus-paperclip.jar ./scripts/start.sh
```

Environment variables:

| Variable | Default | Purpose |
|----------|---------|---------|
| `JAVA` | `java` | Path to Java binary |
| `JAR` | `papyrus-paperclip.jar` | Server jar filename |

Edit `-Xms8G -Xmx8G` in the script to match your host. **Always set `-Xms` equal to `-Xmx`** to avoid heap resize pauses during gameplay.

### Manual JVM flags

If you prefer not to use the script:

```bash
java -Xms8G -Xmx8G \
  -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 \
  -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch \
  -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 \
  -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 \
  -XX:InitiatingHeapOccupancyPercent=15 -XX:MaxTenuringThreshold=1 \
  -jar papyrus-paperclip.jar nogui
```

Paper's [Aikar flags documentation](https://docs.papermc.io/paper/aikars-flags) remains a good reference for tuning beyond these defaults.

### Moonrise system properties

Optional JVM flags for advanced tuning:

| Property | Example | Effect |
|----------|---------|--------|
| `Papyrus.WorkerThreadCount` | `-DPapyrus.WorkerThreadCount=4` | Override Moonrise worker thread count |
| `Papyrus.NumaScheduling` | `-DPapyrus.NumaScheduling=true` | NUMA-aware threading on multi-socket hosts |
| `Papyrus.MaxViewDistance` | `-DPapyrus.MaxViewDistance=32` | Hard cap on view distance |

---

## Configuration

Papyrus uses Paper's layered config system. Files are YAML; keys use kebab-case.

| File | Scope |
|------|-------|
| `config/paper-global.yml` | Server-wide settings |
| `config/paper-world-defaults.yml` | Defaults applied to every world |
| `<world>/paper-world.yml` | Overrides for a specific world |
| `spigot.yml` | Entity activation/tracking ranges, hopper rates, Netty threads |
| `bukkit.yml` | Spawn limits, chunk settings |
| `server.properties` | Port, gamemode, difficulty, etc. |

After editing configs, restart the server or use `/paper reload` where supported (some settings require a full restart).

For Papyrus-specific options and presets, see [docs.sushii.dev/papyrus](https://docs.sushii.dev/papyrus/). For the full Paper config reference, see [docs.papermc.io](https://docs.papermc.io/paper/reference/configuration/). The sections below cover **Papyrus-specific** behavior and recommended values.

---

## Papyrus-specific options

### Entity random source

**File:** `config/paper-global.yml`

```yaml
performance:
  entity-random-source: SHARED   # or VANILLA
  apply-runtime-jvm-defaults: true # Netty buffer caps, JNA nosys
  netty-threads: -1                # -1 = auto (4–8 based on CPU)
```

| Value | Behavior | Use when |
|-------|----------|----------|
| `SHARED` | All entities share one random source (Paper default, faster) | Performance matters; enchantment seed manipulation is not needed |
| `VANILLA` | Each entity gets `RandomSource.create()` like vanilla | Speedrunners, enchantment seed manipulation, or any vanilla-like RNG dependency |

### Experience orbs

**File:** `config/paper-world-defaults.yml` or `<world>/paper-world.yml`

```yaml
environment:
  experience-orb-despawn-rate: 6000   # ticks (vanilla: 6000)
  experience-orb-pickup-radius: 8.0   # blocks (vanilla: 8)

entities:
  spawning:
    experience-orb-merge-radius: 1.0    # merge search radius in blocks
  behavior:
    disable-experience-orb-merge: false
```

Lower `experience-orb-pickup-radius` to reduce orb lag on grinder servers. Set `disable-experience-orb-merge: true` if you want orbs to stay separate.

### Redstone implementation

**File:** `config/paper-world-defaults.yml` or `<world>/paper-world.yml`

```yaml
misc:
  redstone-implementation: VANILLA   # VANILLA | EIGENCRAFT | ALTERNATE_CURRENT
  alternate-current-update-order: HORIZONTAL_FIRST_OUTWARD   # only for ALTERNATE_CURRENT
```

| Engine | Speed | Vanilla parity |
|--------|-------|----------------|
| `VANILLA` | Baseline | Full — same update order as Minecraft |
| `EIGENCRAFT` | ~95% fewer wire updates on depower | Different update order; fixes MC-11193 |
| `ALTERNATE_CURRENT` | Fastest in most benchmarks | Different update order; configurable direction |

Default is `VANILLA`. Switch to `EIGENCRAFT` or `ALTERNATE_CURRENT` only on redstone-heavy technical servers where speed matters more than vanilla timing.

### Chunk system I/O threads

**File:** `config/paper-global.yml`

```yaml
chunk-system:
  io-threads: -1      # auto-detect from CPU count (recommended)
  worker-threads: -1  # auto-detect from CPU count (recommended)
```

Papyrus auto-scales I/O threads to `min(4, max(1, cpu_cores / 4))` when set to `-1` or `0`. Paper previously pinned this to a single I/O thread.

---

## Performance tuning

### Papyrus performance defaults

These defaults apply to **new** config files. Existing servers keep their saved values until you change them manually.

#### Global (`config/paper-global.yml`)

| Key | Papyrus default | Effect |
|-----|-----------------|--------|
| `chunk-system.io-threads` | auto | Scales chunk load/save throughput with CPU |
| `performance.entity-random-source` | `SHARED` | Fast entity RNG |
| `performance.apply-runtime-jvm-defaults` | `true` | Sets Netty buffer caps and `jna.nosys` at startup |
| `performance.netty-threads` | `-1` (auto) | Scales Netty event-loop threads (4–8) when not set in `spigot.yml` |
| `spark.enabled` | `false` | No Spark profiler overhead |
| `misc.region-file-cache-size` | `512` | Larger region file cache (uses more RAM) |
| `update-checker.enabled` | `false` | No PaperMC update checks |

#### World (`config/paper-world-defaults.yml`)

| Key | Papyrus default | Effect |
|-----|-----------------|--------|
| `misc.update-pathfinding-on-block-update` | `false` | Mobs don't repath on every nearby block change |
| `environment.optimize-explosions` | `true` | Faster TNT/creeper blast processing |
| `environment.experience-orb-despawn-rate` | `6000` | Ticks until XP orbs despawn (vanilla: 6000) |
| `environment.experience-orb-pickup-radius` | `8.0` | Player pickup range in blocks |
| `entities.spawning.experience-orb-merge-radius` | `1.0` | Radius for orb merge search |
| `entities.behavior.disable-experience-orb-merge` | `false` | When true, orbs never merge |
| `unsupported-settings.disable-world-ticking-when-empty` | `true` | Worlds with no players stop ticking |
| `hopper.ignore-occluding-blocks` | `true` | Hoppers skip entity scans under solid blocks |
| `entities.armor-stands.tick` | `false` | Armor stands don't tick (display/map servers) |
| `entities.markers.tick` | `false` | Marker entities don't tick |
| `scoreboards.allow-non-player-entities-on-scoreboards` | `false` | Skips scoreboard team lookups for non-players |
| `chunks.entity-per-chunk-save-limit.*` | capped | Limits arrow/orb/pearl buildup per chunk |

#### Spigot (`spigot.yml`)

| Key | Papyrus default | Effect |
|-----|-----------------|--------|
| `commands.log` | `false` | No disk I/O logging every command |
| `settings.netty-threads` | auto | Scales Netty I/O threads with CPU when unset |

### Spigot entity ranges (manual tuning)

For mob-heavy servers, lower tracking and activation ranges in `spigot.yml`:

```yaml
world-settings:
  default:
    entity-tracking-range:
      animals: 48
      monsters: 48
      misc: 32
    entity-activation-range:
      animals: 24
      monsters: 24
      water: 8
      villagers: 16
```

Tracking controls network packets; activation controls server-side AI ticks. Lower both before touching game rules.

---

## Vanilla compatibility preset

Use this when vanilla behavior matters more than peak performance (speedrunning, strict survival, enchantment seed work):

```yaml
# config/paper-global.yml
performance:
  entity-random-source: VANILLA

# config/paper-world-defaults.yml
misc:
  redstone-implementation: VANILLA
  update-pathfinding-on-block-update: true   # restore Paper-like mob repathing
environment:
  optimize-explosions: false
entities:
  armor-stands:
    tick: true
  markers:
    tick: true
```

Also set `entity-random-source: VANILLA` if players use enchantment table seed manipulation — this is the setting that restores per-entity RNG.

---

## Redstone presets

### Vanilla survival (default)

```yaml
misc:
  redstone-implementation: VANILLA
```

### Technical / farm server

```yaml
misc:
  redstone-implementation: EIGENCRAFT
```

### Maximum wire performance

```yaml
misc:
  redstone-implementation: ALTERNATE_CURRENT
  alternate-current-update-order: HORIZONTAL_FIRST_OUTWARD
```

Test contraptions after switching engines. Timing and update order will differ from vanilla.

---

## Building from source

```bash
git clone https://github.com/codingsushi79/Papyrus.git
cd Papyrus

# Apply Minecraft patches and compile
./gradlew applyPatches build

# Create the runnable paperclip jar (copied to papyrus-paperclip-*.jar)
./gradlew createPaperclipJar syncPapyrusPaperclipJar
```

Common Gradle tasks:

| Task | Purpose |
|------|---------|
| `./gradlew applyPatches` | Apply all Minecraft source patches |
| `./gradlew build` | Compile and run tests |
| `./gradlew createPaperclipJar` | Build the downloadable server jar |
| `./gradlew syncPapyrusPaperclipJar` | Copy paperclip output to `papyrus-paperclip-*.jar` |
| `./gradlew rebuildPatches` | Regenerate patch files after editing `papyrus-server/src/minecraft` |
| `./gradlew fixupSourcePatches` | Fix patch context after manual edits |

Windows: use `gradlew` instead of `./gradlew`.

Development requires cloning with Git — the build system applies patches against a generated git tree inside `papyrus-server/src/minecraft`. Downloading a zip from GitHub will not work.

See [CONTRIBUTING.md](CONTRIBUTING.md) for patch workflow details (inherited from Paper).

---

## Project structure

```
Papyrus/
├── papyrus-api/          Public Bukkit/Paper plugin API (Gradle project :paper-api)
├── papyrus-server/       Server implementation (Gradle project :paper-server)
├── paper-server/         Symlink → papyrus-server (required by Paperweight patch tooling)
│   ├── patches/          Minecraft source patches (sources/, features/, resources/)
│   └── src/main/java/    Papyrus/Paper Java code (io.papermc.paper.*)
├── build-data/           Access wideners and mapping data
├── scripts/
│   └── start.sh          Production JVM start script
├── gradle.properties     MC version, apiVersion, Maven group
└── .github/workflows/    CI build, tests, and release uploads
```

Gradle project names stay `:paper-api` and `:paper-server` so upstream Paperweight and plugin examples keep working. Physical directories use the `papyrus-*` prefix.

Maven coordinates remain `io.papermc.paper:paper-api` for plugin compatibility. The API jar embeds `apiVersioning.json` (from `apiVersion` in `gradle.properties`) for runtime version checks.

---

## Plugin development

Papyrus is API-compatible with Paper plugins. Use the same dependency and plugin metadata format.

### Dependencies

**Gradle (Kotlin DSL):**

```kotlin
repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
```

**Maven:**

```xml
<repository>
    <id>papermc</id>
    <url>https://repo.papermc.io/repository/maven-public/</url>
</repository>

<dependency>
    <groupId>io.papermc.paper</groupId>
    <artifactId>paper-api</artifactId>
    <version>26.1.2-R0.1-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

Replace the version with the current `apiVersion` from [`gradle.properties`](gradle.properties). Papyrus does not publish its own Maven repository; the API matches Paper's published artifact.

### `paper-plugin.yml`

```yaml
name: MyPlugin
version: 1.0.0
main: com.example.myplugin.MyPlugin
api-version: '26.1.2'   # must match gradle.properties apiVersion
```

Use `api-version` matching `gradle.properties`. Paper plugins use `paper-plugin.yml`; legacy Bukkit plugins use `plugin.yml` instead.

### Detecting Papyrus at runtime

```java
import io.papermc.paper.ServerBuildInfo;
import net.kyori.adventure.key.Key;

ServerBuildInfo info = ServerBuildInfo.buildInfo();
if (info.brandId().equals(ServerBuildInfo.BRAND_PAPYRUS_ID)) {
    // Running on Papyrus (sushimc:papyrus)
}
if (info.isBrandCompatible(ServerBuildInfo.BRAND_PAPER_ID)) {
    // true on both Paper and Papyrus — use for generic Paper-targeted plugins
}
```

### Local development against this repo

```kotlin
dependencies {
    compileOnly(project(":paper-api"))
}
```

Run `./gradlew publishToMavenLocal` to install `paper-api` locally, then add `mavenLocal()` above the PaperMC repository in your plugin project. See [CONTRIBUTING.md](CONTRIBUTING.md#using-the-test-plugin) for running the included `test-plugin` module.

More plugin guides: [docs.sushii.dev/papyrus](https://docs.sushii.dev/papyrus/) and [docs.papermc.io/paper/dev](https://docs.papermc.io/paper/dev/).

---

## Continuous integration

GitHub Actions builds on every push and pull request to `main`:

1. Apply Minecraft patches
2. Compile and run tests
3. Build the paperclip jar
4. Upload CI artifacts (`papyrus-server` jar + test results)

**Releases:** Pushing a version tag (e.g. `v1.0.1`) creates a [GitHub Release](https://github.com/codingsushi79/Papyrus/releases) with `Papyrus-<mcVersion>.jar` attached automatically. No external secrets are required.

Download CI builds from the Actions tab on [github.com/codingsushi79/Papyrus](https://github.com/codingsushi79/Papyrus/actions).

---

## Contributing

Papyrus inherits Paper's patch-based development workflow. To contribute:

1. Fork [codingsushi79/Papyrus](https://github.com/codingsushi79/Papyrus)
2. Clone your fork and create a feature branch from `main`
3. Make changes — Java code in `papyrus-server/src/main/java/` directly, Minecraft changes via the patch system
4. Run `./gradlew build` to verify
5. Open a pull request against `main`

See [CONTRIBUTING.md](CONTRIBUTING.md) for the full patch workflow. [SECURITY.md](SECURITY.md) covers vulnerability reporting.

---

## License

Papyrus inherits licensing from Paper, Spigot, and CraftBukkit. See [LICENSE.md](LICENSE.md) for details.

---

## Credits

- [Paper](https://github.com/PaperMC/Paper) by [PaperMC](https://papermc.io) — upstream server software
- [Spigot](https://www.spigotmc.org/) / [CraftBukkit](https://bukkit.org/) — original Bukkit implementation

---

## FAQ

**Is Papyrus compatible with Paper plugins?**  
Yes. Same API package (`io.papermc.paper`), same Maven artifact (`io.papermc.paper:paper-api`), same config file names, same `paper-plugin.yml` format. Gradle modules are named `:paper-api` / `:paper-server` but live in `papyrus-*` directories. Use `ServerBuildInfo.BRAND_PAPYRUS_ID` only if you need Papyrus-specific behavior.

**What's the difference between `performance.netty-threads` and `spigot.yml` Netty settings?**  
`performance.netty-threads` in `paper-global.yml` sets `io.netty.eventLoopThreads` when `spigot.yml` does not override Netty thread count. Set either one, not both, unless you know you need different values.

**Will my existing Paper config work?**  
Yes. Drop in your existing `config/`, `spigot.yml`, and worlds. Papyrus-specific defaults only apply to keys that aren't already set.

**How do I restore enchantment seed manipulation?**  
Set `performance.entity-random-source: VANILLA` in `config/paper-global.yml` and restart.

**How do I get vanilla redstone behavior?**  
Set `misc.redstone-implementation: VANILLA` in your world config. This is already the default.

**Why is the jar named `papyrus-paperclip`?**  
The bootstrap tool (`paperclip`) comes from upstream Paper. Papyrus copies the build output to `papyrus-paperclip-*.jar`.

**Where do I report bugs?**  
Open an issue at [github.com/codingsushi79/Papyrus/issues](https://github.com/codingsushi79/Papyrus/issues). Specify whether the bug exists in upstream Paper or is Papyrus-specific.

**Can I redistribute the built jar?**  
Yes, under the terms of the GPL/MIT licenses described in [LICENSE.md](LICENSE.md).
