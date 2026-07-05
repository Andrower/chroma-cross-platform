# Chroma Cross Portable Server

Windows:

Double-click `start-chroma-server.bat`.

Windows uses the bundled `node.exe` in this package.

After startup:

- Launch page opens automatically on the computer.
- Phones scan the QR code shown on the launch page.
- Use the two buttons to enter control or display.
- Direct URLs still work:
  - `http://PHONE_IP:8765/chroma-cross-screen.html?mode=control`
  - `http://PHONE_IP:8765/chroma-cross-screen.html?mode=display`

Keep the terminal window open while using the service. Close it or press Control-C to stop.

Optional Windows exe client:

- `electron-client` contains the desktop shell source for building a portable `.exe`.
- The exe shell starts the same local service, opens the launch page, and uses stronger display locking in Electron.
- While locked it blocks common exit shortcuts such as `Alt+F4`, `Esc`, `F11`, `Ctrl+W`, and `Ctrl+R`.
- Unlock hotkey: `Ctrl + Alt + Shift + L`.

Build on Windows:

```bat
cd electron-client
npm install
npm run build:win
```

The generated portable exe will appear in `electron-client\dist`.

Note: Windows system-level actions such as `Ctrl + Alt + Delete`, the power key, and Task Manager force-ending the process cannot be fully blocked by a normal app.
