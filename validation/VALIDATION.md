# Validation for Android build 0.2

- `:app:assembleDebug`: successful; application id `com.jengacode.kingfischer`, Android API 26 minimum, API 36 target, version 0.2.
- `:app:testDebugUnitTest`: 10 tests, 0 failures, 0 errors. Tests include start-position perft depth 4 (197281 nodes), PGN round trips, comments/variations, rejected illegal moves, immutable history snapshots, undo, checkmate, repetition, castling, promotion and an actual legal depth-2 search with the bundled NNUE, rating validation and effort progression, and eight legal half-moves between engines set to 800 and 1800 with PGN export/replay.
- `:app:lintDebug`: successful; 0 errors, 2 advisory warnings (a newer AndroidX Activity version is available, and the numeric rating input is formatted as plain digits).
- `node server/test.mjs`: successful. Covers room creation, private access, joining and retrying a join after a lost response, rejecting another join, turn enforcement, illegal/stale moves, idempotent move retry, simultaneous requests, server restart and reconnect, resignation, invitation page and checkmate.
- APK signature verification succeeded; APK Signature Scheme v2 signing valid (supported by the minimum Android version).
- Network SHA-256: `1a298aa575a085434d29027978dc36867fe9c5bcea9376654b7a8eba1e52dfc2`, unchanged from supplied engine.

Not tested here: physical phone/emulator UI and touch interactions, extended mobile performance/battery use, public internet deployment, Docker deployment, Play Store publication. Server integration tests use real HTTP and the compiled Java rules jar on localhost. No public room server has been deployed.
