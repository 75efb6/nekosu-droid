# nekosu!droid

> **Note:** nekosu!droid is a **heavily modified fork** of [osu!droid](https://github.com/osudroid/osu-droid) targeting version **1.7.2**, intended for players who want to play on the legacy client. The vast majority of the code belongs to the osu!droid team and contributors — this fork only adds personal modifications on top of their work.

nekosu!droid is based on osu!droid, a free-to-play circle clicking rhythm game for Android devices, originally hatched by the [osu!](https://osu.ppy.sh/home) community.

## Upstream

This project is a fork of the official osu!droid repository:
**https://github.com/osudroid/osu-droid**

All credit for the original codebase goes to the osu!droid developers and contributors.

### Cloning

```sh
git clone https://github.com/75efb6/nekosu-droid
```

Open the folder in Android Studio.

### Building

In Android Studio you can `Build` a debug release to test your changes. The output `.apk` is inside `build/output`.

Or on Linux via command line:

```sh
chmod +x gradlew
./gradlew assembleDebug
```

Make sure you are using Java 17.

## License

osu!droid (and by extension this fork) is licensed under the [Apache License 2.0](https://opensource.org/licenses/Apache-2.0).
