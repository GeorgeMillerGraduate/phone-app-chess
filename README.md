# KingFischer Mobile — Android build 0.2

A native Java Android app built around George Miller’s KingFischer2 engine, with a touch chessboard, offline engine play, offline game analysis, and private online games with invitation links.

## Install on your phone

The supplied **KingFischer-Mobile-debug.apk** installs directly on Android 8.0 or newer. Copy it to your phone, open it in Files, and allow that app to install it when Android asks. On Samsung, Auto Blocker may need disabling temporarily for this installation. A debug APK is for personal testing; it is not a Play Store release. This new app installs separately from the barcode scanner. If Android Studio later signs a build with a different debug key, uninstall this supplied test build before installing that one; export any games you want to keep first.

The bundled neural network makes the download approximately 100 MB and requires substantial memory. The first search loads the network in the background; later searches reuse it. Older phones may be slow. No network connection is required for engine play or analysis. Rating labels such as 1200 are approximate difficulty settings, not measured Elo ratings. The 400–3000 range controls search depth, time, node budget and the chance of choosing an alternative among the engine’s leading moves; no measured human-rating calibration has been performed.

## Open in Android Studio on Windows

1. Extract **KingFischerMobile-source.zip**.
2. In Android Studio choose **Open** and select the **KingFischerMobile** folder containing `settings.gradle`.
3. Let Gradle sync, using Android Studio’s Java 17 or newer compatible JDK. Install Android SDK 36 and Build Tools 35.0.0 if prompted.
4. Connect your phone with USB debugging enabled, choose it in the device selector, and click **Run**.
5. After installation you can unplug the phone. Engine play and analysis stay on the phone.

Command line, from this project folder:

```bat
gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The APK is then in `app\build\outputs\apk\debug\app-debug.apk`. The Gradle wrapper downloads Gradle 8.13. SDK location is specific to your computer; do not copy someone else’s `local.properties`.

## Play and analyse

- **Home:** choose engine play, a friend game, engine versus engine, or analysis. Continue your saved engine game from Home. The Home button and Android Back return to this screen.
- **Play engine:** enter a rating, such as 1200, and choose White or Black. Tap a piece, then one of its legal destinations. Promotion offers queen, rook, bishop or knight. Undo removes your last turn. Games are saved after each move.
- **Engine versus engine:** enter separate White and Black ratings and watch your Java engine play both sides. Start/resume and pause controls are provided. Leaving the screen or app pauses the match, and the moves are saved. You can share its PGN or open it in analysis.
- **Move display:** the board has no last-move colour shading, and piece images always render at full opacity. The numbered move list has separate White and Black columns, piece images for non-pawn moves, and algebraic coordinates, captures, check markers and castling. For example, a knight image followed by `a3` represents standard SAN `Na3`. In analysis, tap a move to visit that position.
- **Analyse:** opening this tab copies the game you were viewing. Import a standard-start PGN or UCI move list. Main-line PGN comments and variations are handled; custom FEN-start games are explicitly unsupported in this version. Move through the game using buttons or the slider. Analyse one position or every position; stop whenever needed. White-positive evaluations favour White. Mate scores are labelled separately. Share a PGN or the analysis report using the Android share sheet.
- **Play friend:** create a room on your configured room server and share the private link through WhatsApp, Discord, or another app. You play White; your friend joins as Black. Both phones need this app. The invite opens a web page with an **Open KingFischer** button; if an in-app browser blocks it, paste the link into **Join invitation**. Invitations grant one seat and should be shared privately.

Friend games are untimed, casual, and for two players. Moves refresh roughly every 1.4 seconds while the friend screen is open. The room token is saved in private app storage; resume to reconnect. Leaving the room removes this phone’s stored access; share its PGN first. There are no accounts, ratings, push notifications, spectator mode, draw offers, matchmaking, ads or purchases in this first chess build. Rooms expire after 72 hours without a move, join or resignation.

Games automatically end on checkmate, stalemate, insufficient material, threefold repetition or the 50-move rule. The last two are automatic casual-game adjudications rather than FIDE’s claim procedure.

## Test a friend game on your Wi-Fi

The supplied server uses Node.js 18+ and Java 17+. It has no npm dependencies. A precompiled `server/rules.jar` is included; it validates moves using the same Java rules as the phone.

On Windows, open PowerShell in the `server` folder:

```powershell
java -version
node --version
$env:PORT="8080"
node server.mjs
```

In another PowerShell window use `ipconfig` to find your computer’s Wi-Fi IPv4 address, for example `192.168.1.10`. Both phones must be on the same Wi-Fi. In **Play friend → Create room**, enter:

```text
http://192.168.1.10:8080
```

Allow Node through Windows Firewall for your private network if Windows asks. The computer must stay on and the server terminal must remain running. Use the debug APK for local HTTP testing; release builds allow HTTPS only. Local Wi-Fi links will not work for a friend outside that network.

Run server tests from the server folder:

```powershell
node test.mjs
```

Rebuild its rules jar after changing the engine’s rules:

```bat
build-rules.bat
```

## Internet games need hosting

No public server has been deployed or configured for you. To play from different networks, deploy the included server to an always-running host with Java 17 and Node 18+, persistent storage, and an HTTPS address. Hosting and a domain are separate services. Your existing basic PHP/WordPress hosting may not support a persistent Node/Java service; verify its capabilities before choosing it.

Set `PUBLIC_URL` to the actual externally reachable HTTPS origin so invitations open the correct app link, and `ROOM_DATA` to a persistent writable path. Forward HTTPS to port 8080 through your host’s reverse proxy. Do not expose the plain HTTP test port directly on the internet. Room data contains player access tokens; keep it private and do not commit it. Back it up only to trusted storage.

A Dockerfile is included. Build from the **project root**, not from the server folder:

```sh
docker build -f server/Dockerfile -t kingfischer-rooms .
docker run -d --name kingfischer-rooms -p 127.0.0.1:8080:8080 \
  -e PUBLIC_URL=https://chess.example.com -e TRUST_PROXY=1 \
  -v kingfischer-rooms:/app/data kingfischer-rooms
```

Replace the example address with your real host. Configure TLS on that host and enter that HTTPS address in the phone app. When `TRUST_PROXY=1`, the reverse proxy must overwrite `X-Forwarded-For`, and the Node port must be reachable only through that trusted proxy; this keeps rate limits separate for different players. Leave `TRUST_PROXY` unset for direct local Wi-Fi tests. This Docker configuration is supplied as a deployment template; it has not been deployed or container-tested here.

The server authenticates each player with a random bearer token, checks invitations separately, serialises per-room writes, rejects illegal/out-of-turn/stale moves, supports retrying a move after a lost response, and persists rooms after each accepted mutation. Basic request, room, body and validation-concurrency limits are included. This is a first casual-game server, not a production service with monitoring, account recovery or guaranteed availability.

## Source and licence

GPL-3.0-or-later. `COPYING.txt`, `engine-provenance/NOTICE.md` and `engine-provenance/STOCKFISH-AUTHORS` contain licence and attribution details for the original engine and its Stockfish-derived work. The supplied source contains the actual NNUE network and Java engine implementation. A native Stockfish executable is not used. The artwork consists of the piece PNGs supplied in the desktop project; confirm their origin and redistribution licence before a public store release.

Core engine code is copied from the uploaded KingFischer2 project. Android adaptations expose `Network.fromStream`, replace `readNBytes` with a complete-read loop, and remove desktop/file-system network discovery. Search logic, evaluation, trained weights and legal move generation are preserved. `GameModel` uses standard SAN and adds a one-move undo helper. The mobile UI, PGN IO and room service are new.

This first build has automated JVM and room-server validation and an Android build/lint check. Physical-device UI, battery, long-game memory use and internet hosting should be tested before public release. Installation, ad SDKs, payment billing and Play Store publication are separate future steps.
