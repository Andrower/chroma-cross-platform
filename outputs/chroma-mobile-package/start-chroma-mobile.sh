#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")"

if ! command -v node >/dev/null 2>&1; then
  echo "Node.js is required."
  echo "On Android Termux, run: pkg install nodejs"
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
echo "Opening: $URL"
echo
(sleep 1; open_url) &
node chroma-control-server.js
