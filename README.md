# HaptyApp

*Other languages: [Italiano](README.it.md)*

HaptyApp is the Android application that goes with 3D-printed tactile flowcharts.
The printed board is placed on the tablet as an overlay, and when a blind student touches a node through its opening, the app reads the node aloud.

The app was developed within the [HaptyHub](https://github.com/DavideFantasia/HaptyHub) project.
The boards and the layout files it loads are produced by the pipeline in [tactile-flowcharts](https://github.com/StefanoPea/tactile-flowcharts), the code of the MSc thesis *From Static Visual Content to Accessible Interactive Representations: A Tactile Pipeline Using Open Vision-Language Models* (University of Pisa).

## How it works

- **Layout file.** The app loads a layout file (`app_layout.json`, written by `pipeline/pipeline.py` in tactile-flowcharts) with the nodes, the edges and the centre of every opening of the board, in millimetres on the screen. It places one touch region under each opening; since the openings and the touch regions come from the same data, they line up without any manual measurement.
- **Speech.** Touching a node reads, in Italian, its type, its text and every edge leaving it, with its direction and, for a decision, its Yes (*Sì*) or No label. Operators in the text are read as words (`<=` becomes "minore o uguale di"). On devices with a vibration motor, every touch also gives a short vibration.
- **Menu.** A square opening without a raised rim, in the top-right corner of the board, leaves the menu button reachable.
  - A tap changes how much of each description is read (Expertise Menu): *Principiante* reads the type, the text and the edges; *Intermedio* the text and the edges; *Esperto* only the text.
  - A long press opens the file picker to load the layout file of another board, guided by audio. Without a board on the screen, a long press on the background does the same.
- **Older files.** Layout files without the positions of the openings, like the example diagram in `assets/`, are scaled to the screen by the app itself.

## Requirements

- Android Studio with Android Gradle Plugin 9.1 and Android SDK 36; Gradle runs on JDK 21.
- An Android tablet with Android 7.0 (API 24) or newer and an Italian text-to-speech voice.
- A layout file generated for the screen size of that tablet (`pipeline/devices.json` in tactile-flowcharts).

## Installation

1. Clone the repository:

   ```bash
   git clone git@github.com:StefanoPea/HaptyApp.git
   ```

2. Open the project in Android Studio and sync the Gradle files.
3. Run the app on the tablet.

## Usage

1. Generate the board and its layout file with tactile-flowcharts (`pipeline/pipeline.py`) and print the board.
2. Copy `app_layout.json` to the tablet.
3. Open the app and long-press the menu button to load the file.
4. Place the board on the tablet and explore it by touch.

## Project structure

```plaintext
HaptyApp/
├── app/
│   ├── build.gradle.kts             # build configuration
│   └── src/main/
│       ├── AndroidManifest.xml      # activity and permissions
│       ├── assets/                  # example diagram loaded at startup
│       ├── java/com/example/digitaltwinflowchart/
│       │   └── MainActivity.kt      # touch regions, speech and menu
│       └── res/                     # layout and resources
├── build.gradle.kts
└── settings.gradle.kts
```

## Technical notes

- Millimetres are converted to pixels with the screen density reported by Android (`xdpi`, `ydpi`). If a device reports it inaccurately, the touch regions shift away from the openings: check the alignment on a new tablet before using it with a student.
- The speech rate is set to 1.5 times the default.

## Authors

Davide Fantasia, Stefano Pea and Lorenzo Tinfena.
