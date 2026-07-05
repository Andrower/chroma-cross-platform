# Chroma Cross Mobile

Android Termux run:

```sh
unzip chroma-mobile-package.zip
cd chroma-mobile-package
sh start-chroma-mobile.sh
```

The startup script installs Node.js automatically with `pkg install nodejs -y`
when Node.js is missing.

If you run `start-chroma-mobile.sh` from the parent folder, keep the extracted
`chroma-mobile-package` folder under that same parent folder. The script will
search up to 3 levels below its own location.

After startup, the script tries to open the display URL automatically.
If Android does not open the browser, install Termux:API and the Termux:API app, or open the printed LAN URL manually.

Default URL opens as display client.

Control URL:

```text
http://PHONE_IP:8765/chroma-cross-screen.html?mode=control
```

Display URL:

```text
http://PHONE_IP:8765/chroma-cross-screen.html
```

Notes:

- The control page always shows a QR code for fast connection.
- Display clients auto-register and can be locked on each phone.
- The control page can preview and change the selected display client.
