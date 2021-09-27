To build Meditation Assistant from source:

# Download

```git clone https://gitlab.com/tslocum/meditationassistant.git```

# Import

Open [Android Studio](https://developer.android.com/studio/), select *Import
project* and choose the folder downloaded above.

## Import Without Android Studio (IntelliJ IDEA 2021.x)

On 2021-09-15 I built this with the following tools:

- Gradle 7.0.2
- Java SDK 11.0.12-zulu

I had to downgrade the Android/Gradle build tool version to 4.2.2 to build this on my machine. I needed to do this only to import the project into IDEA; I was able to build it with Gradle outside IDEA as I originally cloned the project.

# Build

Connect an Android device to your PC using
[ADB](https://developer.android.com/studio/command-line/adb) or start an
[emulated device](https://developer.android.com/studio/run/emulator).

Open the *Build Variants* panel and choose a variant.
Debug variants build quickly, while release variants are optimized.

Variants:

- opensource: F-Droid
- free: Amazon/Google free version
- full: Amazon/Google paid version

Click the green play button labeled *Run selected configuration* to build and
install the application.
