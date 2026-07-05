# Chroma Cross Mobile

Android Termux run:

```sh
pkg install nodejs
unzip chroma-mobile-package.zip
cd chroma-mobile-package
sh start-chroma-mobile.sh
```

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
