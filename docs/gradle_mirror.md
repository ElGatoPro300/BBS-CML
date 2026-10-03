# Gradle / Fabric Maven mirror (ISP blocks)

Some ISPs (notably in Spain on weekends) block or stall `https://maven.fabricmc.net/`. Gradle then hangs for **~2+ minutes** per failed connect while resolving Loom / yarn / Fabric Loader / Fabric API, which looks like “the game takes forever to start” when running `./gradlew runClient`.

**Symptom check (from this machine):**

| Host | Typical result when blocked |
|------|-----------------------------|
| `maven.fabricmc.net` | Connection timed out |
| `maven2.fabricmc.net` / `maven3.fabricmc.net` | OK (HTTP 200) |
| `services.gradle.org` (wrapper) | Usually OK — not the main bottleneck |

The Gradle wrapper ZIP URL is normally **not** the problem. Loom still injects the **primary** Fabric Maven (`maven.fabricmc.net`) unless you override it. Only swapping `settings.gradle` + adding a secondary `maven3` repo is **not enough**: Loom keeps hitting the blocked host first.

---

## Required pieces (complete workaround)

Apply **all** of the following. Partial setups still time out on yarn/loader.

### 1. `settings.gradle` — plugin repo + Loom ExtraProperties

Loom’s `MirrorUtil` reads **`ExtraProperties`** on Settings/Project (`loom_fabric_repository`). Putting the same keys only in `gradle.properties` is **not reliable** on all setups.

```gradle
pluginManagement {
	repositories {
		maven {
			name = 'Fabric'
			/* TEMP ISP: primary maven.fabricmc.net often times out */
			url = 'https://maven3.fabricmc.net/'
			/* alternative: https://maven2.fabricmc.net/ */
		}
		mavenCentral()
		gradlePluginPortal()
	}
}

/* TEMP ISP: force Loom’s injected Fabric Maven URL */
ext.loom_fabric_repository = 'https://maven3.fabricmc.net/'
ext.loom_experimental_versions = 'https://maven3.fabricmc.net/net/minecraft/experimental_versions.json'

gradle.beforeProject { project ->
	project.ext.loom_fabric_repository = 'https://maven3.fabricmc.net/'
	project.ext.loom_experimental_versions = 'https://maven3.fabricmc.net/net/minecraft/experimental_versions.json'
}

rootProject.name = "BBS mod"
```

### 2. `build.gradle` — rewrite Loom’s Fabric repo + intermediary + fallback maven

**Immediately after** the `plugins { ... }` block (Loom may already have registered `maven.fabricmc.net`):

```gradle
/* TEMP ISP: rewrite any Loom-injected primary Fabric Maven URL */
repositories.withType(org.gradle.api.artifacts.repositories.MavenArtifactRepository).configureEach { repo ->
	String url = repo.url.toString()

	if (url.startsWith("https://maven.fabricmc.net") && !url.startsWith("https://maven3.fabricmc.net"))
	{
		repo.setUrl("https://maven3.fabricmc.net/")
	}
}
```

Inside `repositories { ... }` (fallback for non-Loom lookups):

```gradle
maven {
	name = "FabricMaven3"
	url = "https://maven3.fabricmc.net/"
}
```

Inside `loom { ... }` (intermediary jar download; keep access widener / other loom config unchanged):

```gradle
/* TEMP: ISP block workaround — intermediary download URL */
(it as net.fabricmc.loom.extension.LoomGradleExtensionApiImpl).intermediaryUrl.set(
		"https://maven3.fabricmc.net/net/fabricmc/intermediary/%1\$s/intermediary-%1\$s-v2.jar"
)
```

### 3. `gradle.properties` — optional documentation / reinforcement

```properties
# Also set in settings.gradle ExtraProperties (required for Loom MirrorUtil).
loom_fabric_repository=https://maven3.fabricmc.net/
loom_experimental_versions=https://maven3.fabricmc.net/net/minecraft/experimental_versions.json
```

---

## After applying

1. Stop Gradle daemons if a previous build hung: `./gradlew --stop`
2. Sync / import the project in the IDE, or run `./gradlew help` / `./gradlew runClient`
3. In logs you should see Fabric downloads from **`maven3.fabricmc.net`**, not timeouts on `maven.fabricmc.net`
4. Avoid routine `--refresh-dependencies` (Loom warns it is significantly slower)

If `maven3` fails too, swap every `maven3` URL above for `https://maven2.fabricmc.net/` (and the matching intermediary path).

---

## Reverting when the ISP unblocks

1. Restore pluginManagement / Loom Fabric URL to `https://maven.fabricmc.net/`
2. Remove `ext.loom_fabric_repository` / `loom_experimental_versions` / `beforeProject` from `settings.gradle`
3. Remove the `repositories.withType(...).configureEach` rewrite block from `build.gradle`
4. Remove the `FabricMaven3` maven entry and the `intermediaryUrl.set(...)` block
5. Remove the TEMP mirror lines from `gradle.properties`
6. `./gradlew --stop` and sync again

---

## Why the incomplete workaround felt “always slow”

| Change alone | Result |
|--------------|--------|
| Only `settings.gradle` plugin URL → maven3 | Plugins download OK; **project** deps still use Loom’s `maven.fabricmc.net` → long timeout |
| Only extra `maven { url maven3 }` in `build.gradle` | Gradle still tries blocked host first |
| Only `intermediaryUrl` → maven3 | Intermediary OK; yarn/loader/API still hit primary Fabric Maven |
| **ExtraProperties + URL rewrite + intermediary + plugin repo** | Avoids the blocked host for the full Loom/Fabric resolution path |

Mark all of these blocks with `TEMP ISP` comments so they are easy to find and delete later.
