#!/usr/bin/env sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
SERVER_DIR="$SCRIPT_DIR"

if [ ! -f "$SERVER_DIR/chroma-control-server.js" ]; then
  SERVER_FILE="$(find "$SCRIPT_DIR" -maxdepth 3 -type f -name chroma-control-server.js 2>/dev/null | head -n 1 || true)"
  if [ -n "$SERVER_FILE" ]; then
    SERVER_DIR="$(dirname "$SERVER_FILE")"
  fi
fi

if [ ! -f "$SERVER_DIR/chroma-control-server.js" ]; then
  echo "Cannot find chroma-control-server.js."
  echo "Run this script inside the extracted chroma-mobile-package folder,"
  echo "or keep the full chroma-mobile-package folder under the script location."
  exit 1
fi

cd "$SERVER_DIR"

if ! command -v node >/dev/null 2>&1; then
  if command -v pkg >/dev/null 2>&1; then
    echo "Node.js not found. Installing with Termux pkg ..."
    pkg install nodejs -y
  else
    echo "Node.js is required."
    echo "On Android Termux, run: pkg install nodejs"
    exit 1
  fi
fi

if ! command -v node >/dev/null 2>&1; then
  echo "Node.js install did not complete. Please run: pkg install nodejs"
  exit 1
fi

PORT="${PORT:-8765}"
export PORT

LAN_IP="$(node -e 'const os=require("os"); for (const list of Object.values(os.networkInterfaces())) for (const item of list || []) if (item.family==="IPv4" && !item.internal) { console.log(item.address); process.exit(0); } console.log("127.0.0.1");')"
URL="http://$LAN_IP:$PORT/chroma-cross-screen.html"

open_url() {
  if command -v termux-open-url >/dev/null 2>&1; then
    termux-open-url "$URL" >/dev/null 2>&1 || true
    return
  fi
  if command -v open >/dev/null 2>&1; then
    open "$URL" >/dev/null 2>&1 || true
    return
  fi
  if command -v xdg-open >/dev/null 2>&1; then
    xdg-open "$URL" >/dev/null 2>&1 || true
  fi
}

echo "Starting chroma screen server on port $PORT ..."
echo "Using files from: $SERVER_DIR"
echo "Opening: $URL"
echo
(sleep 1; open_url) &
node chroma-control-server.js
