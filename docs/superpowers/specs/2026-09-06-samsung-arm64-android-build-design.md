# Samsung ARM64 Android Build Design

## Goal

Build FreeRDP 3.28.0 as an Android APK for Samsung Galaxy S8, S22, and S26 only.

## Target profile

- Source: FreeRDP tag `3.28.0`, upstream commit `5370fb26fbf034ecd11d3026b6ad639b5fff493f`.
- ABI: `arm64-v8a` only. All three device families use the same Android ABI.
- Minimum API: 24, matching the Galaxy S8 launch platform while remaining compatible with newer devices.
- Compile and target API: 36, matching the upstream 3.28.0 Android project.
- NDK: `29.0.13113456`, matching the upstream project.
- CMake: `4.1.2`, matching the upstream project.
- Native features: keep the upstream release set: FFmpeg, OpenH264, Opus, WebP, JPEG, PNG, cJSON, and OpenSSL.
- Output: one ARM64 debug APK and one ARM64 release APK. No 32-bit, x86, RISC-V, or universal APKs.

## Host environment

Use OpenJDK 17 on Ubuntu 24.04. Install the Android command-line tools under `/FreeRDP/android-sdk`, then install platform 36, build-tools 36.0.0, NDK 29.0.13113456, and CMake 4.1.2 with `sdkmanager`. Keep Gradle caches under `/FreeRDP/.gradle` so the environment remains local to this workspace.

## Project configuration

Create `client/Android/Studio/release.properties` with the exact target profile. Enable ABI splits with only `arm64-v8a` and disable the universal APK; this also filters extra ABIs carried by prebuilt AAR dependencies. Create `client/Android/Studio/local.properties` pointing Gradle at `/FreeRDP/android-sdk`.

The upstream release build expects a keystore. Use a workspace-local development keystore only for this build and label the resulting release artifact as development-signed; do not create or modify a production signing identity.

## Build and artifacts

Run the Gradle wrapper from `client/Android/Studio` with `assembleDebug assembleRelease`. Copy the resulting APKs into `/FreeRDP/artifacts` with device-profile names, alongside a small manifest recording source and toolchain versions and SHA-256 checksums.

## Minimal verification

1. Both Gradle assembly tasks complete successfully.
2. APK native library paths contain only `lib/arm64-v8a/`.
3. APK metadata reports minimum SDK 24, and packaged native libraries pass the Android build-tools 16 KB alignment check.
