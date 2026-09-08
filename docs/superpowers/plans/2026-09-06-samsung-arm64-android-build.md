# Samsung ARM64 Android Build Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce FreeRDP 3.28.0 Android APKs specialized for Galaxy S8, S22, and S26 through their shared `arm64-v8a` ABI.

**Architecture:** Use the upstream Gradle/CMake superbuild and override only its documented release properties. Keep the SDK, caches, signing identity, and artifacts local to `/FreeRDP` for a reproducible host setup.

**Tech Stack:** Ubuntu 24.04, OpenJDK 17, Gradle 8.13 wrapper, Android SDK 36, Android Build Tools 36.0.0, Android NDK 29.0.13113456, CMake 4.1.2.

**Spec:** `docs/superpowers/specs/2026-09-06-samsung-arm64-android-build-design.md`

## Global Constraints

- Build only `arm64-v8a`.
- Set minimum API to 24.
- Do not build universal, 32-bit, x86, or RISC-V APKs.
- Do not use or modify a production signing key.
- Preserve the upstream FreeRDP core source at tag `3.28.0`.
- Limit verification to successful assembly, ABI/minimum-SDK inspection, and 16 KB alignment.

---

### Task 1: Android host toolchain

**Files:**
- Create: `/FreeRDP/android-sdk/cmdline-tools/latest/`
- Create: `/FreeRDP/android-sdk/licenses/`

**Interfaces:**
- Consumes: Ubuntu 24.04 package manager and the official Android command-line tools archive.
- Produces: OpenJDK 17 and `/FreeRDP/android-sdk/cmdline-tools/latest/bin/sdkmanager`.

- [x] **Step 1: Install host prerequisites**

Run:

```bash
apt-get update
apt-get install -y openjdk-17-jdk-headless unzip
```

- [x] **Step 2: Download and verify Android command-line tools**

Download `commandlinetools-linux-15859902_latest.zip` from the official Android repository and verify SHA-256 `4e4c464f145a7512b57d088ac6c278c03c9eea610886b35a5e0804e74eedf583`.

- [x] **Step 3: Install the required SDK packages**

Run `sdkmanager --licenses`, then install:

```text
platform-tools
platforms;android-36
build-tools;36.0.0
ndk;29.0.13113456
cmake;4.1.2
```

- [x] **Step 4: Record tool versions**

Run Java, Gradle, sdkmanager, CMake, and NDK version commands and retain their output for the build manifest.

### Task 2: Samsung ARM64 build configuration

**Files:**
- Create: `client/Android/Studio/local.properties`
- Create: `client/Android/Studio/release.properties`
- Create: `/FreeRDP/signing/samsung-arm64-development.jks`

**Interfaces:**
- Consumes: `/FreeRDP/android-sdk` and the upstream Gradle properties read by `client/Android/Studio/build.gradle`.
- Produces: an API 24, ARM64-only Gradle configuration and a non-production signing identity.

- [x] **Step 1: Configure the SDK path**

Write `sdk.dir=/FreeRDP/android-sdk` to `local.properties`.

- [x] **Step 2: Configure the device profile**

Write these exact properties to `release.properties`:

```properties
COMPILE_API=36
TARGET_API=36
MIN_API=24
TOOLS_VERSION=36.0.0
NDK_VERSION=29.0.13113456
CMAKE_VERSION=4.1.2
SPLIT_ENABLED=true
BUILD_UNIVERSAL=false
SPLIT_ARCHITECTURES=arm64-v8a
ABI_FILTERS=arm64-v8a
```

- [x] **Step 3: Create a development signing identity**

Use `keytool` to create `/FreeRDP/signing/samsung-arm64-development.jks`, alias `androiddebugkey`, with the standard Android development-key passwords, and add its absolute path to `release.properties`.

- [x] **Step 4: Confirm Gradle reads the profile**

Run `./gradlew :aFreeRDP:tasks` and confirm its configuration output reports minimum API 24, only `arm64-v8a`, split enabled, and universal disabled.

### Task 3: APK build and minimal verification

**Files:**
- Create: `/FreeRDP/artifacts/aFreeRDP-3.28.0-samsung-arm64-debug.apk`
- Create: `/FreeRDP/artifacts/aFreeRDP-3.28.0-samsung-arm64-release-dev-signed.apk`
- Create: `/FreeRDP/artifacts/build-manifest.txt`
- Create: `/FreeRDP/artifacts/checksums.sha256`

**Interfaces:**
- Consumes: Task 1 toolchain and Task 2 Gradle profile.
- Produces: installable ARM64 APKs and concise build provenance.

- [x] **Step 1: Assemble both APK variants**

Run:

```bash
./gradlew --no-daemon :aFreeRDP:assembleDebug :aFreeRDP:assembleRelease
```

Expected: `BUILD SUCCESSFUL` and two APK files under `aFreeRDP/build/outputs/apk/`.

- [x] **Step 2: Copy and name the artifacts**

Copy the debug and release APKs into `/FreeRDP/artifacts` using the exact names listed above.

- [x] **Step 3: Run the reduced verification set**

Use `apkanalyzer` or `aapt2` to confirm minimum SDK 24, inspect ZIP entries to confirm that every native library is under `lib/arm64-v8a/`, and run `zipalign -c -P 16 -v 4` against both APKs.

- [x] **Step 4: Write provenance and checksums**

Record the Git commit, tag, Java version, Gradle version, SDK packages, ABI, and minimum API in `build-manifest.txt`, then run `sha256sum` over both APKs into `checksums.sha256`.
