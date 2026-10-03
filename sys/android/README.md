
# Build instructions

These instructions are written for a 64-bit Ubuntu installation.
Modifying them for other linux distributions should be little to no
work. If you're running Windows you're on your own.


## Preparations

 - Download and extract Android SDK Command-line Tools [https://developer.android.com/studio/index.html#command-tools]()
 - Install JDK 17. Required by the Android SDK manager and Gradle (e.g. [Temurin](https://adoptium.net/temurin/releases?version=17&os=any&arch=any))
 - Install `bison` and `flex`. Used by the native nethack build.
 - Check out NetHack-Android: `git clone https://github.com/gurrhack/NetHack-Android.git`
 - Create an env variable called `ANDROID_SDK_ROOT` and point it to the android-sdk installation directory. Used by Gradle.

### Install Android build tools

 1. `cd /path/to/android-sdk/tools/bin`
 2. Update the sdk manager: `./sdkmanager --update`. Make sure the env variable `JAVA_HOME` points to JDK 17.
 3. Install the platform tools: `./sdkmanager --install "platforms;android-36" "build-tools;36.0.0"`
 4. Install the NDK: `./sdkmanager --install "ndk;27.3.13750724"`


## Build

### Build the native nethack library

 1. `cd /path/to/NetHack-Android/sys/android`
 2. Open `Makefile.src` and change NDK to the appropriate path.
 3. `sh ./setup.sh`
 4. `cd ../..`
 5. `make install`

This builds for `arm64-v8a`, which works on all current devices. Some newer
devices, e.g. the Galaxy Z Fold6 or Pixel 7 and later, can only run 64-bit code.
To build for another ABI, e.g. older 32-bit devices, pass it to make:
`make install ABI=armeabi-v7a`. The 32-bit ABIs need `gcc-multilib`.

The compiled data files depend on the target's word size, so an APK only
supports the one ABI it was built for. Run `make spotless` before switching
ABI. Saved games from a 32-bit build can't be loaded by a 64-bit build, and
the reverse is also true.

### Build the Android application

 1. `cd /path/to/NetHack-Android/sys/android`
 2. `sh ./gradlew build`
 3. `cd ./app/build/outputs/apk/debug`
 4. Copy the APK file from this directory to your device.
 5. On your device: locate the APK file, install it and run!

---
Happy hacking!
