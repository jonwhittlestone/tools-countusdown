#!/usr/bin/env bash
# Usage: scripts/wifi-device.sh [NAME]
#
# Prints the adb serial of a device on the network (phone or TV), connecting
# first if needed. No re-pairing: a paired device remembers this computer's
# adb key, and mDNS tells us its current connect port (which changes every
# time Wireless debugging restarts).
#
# NAME is one of:
#   adb-43211JEKB09467   the device's mDNS name (see `make wifi-devices`); stable across IP/port changes
#   192.168.1.40[:5555]  a plain address, for TVs using classic network adb on port 5555
#   (empty)              the only wireless device, if there's exactly one
set -euo pipefail

ADB="${ADB:-${ANDROID_HOME:-$HOME/Android/Sdk}/platform-tools/adb}"
name="${1:-}"

connected() {  # serials of connected network devices
  "$ADB" devices | awk '$2 == "device" && ($1 ~ /_adb-tls-connect\._tcp$/ || $1 ~ /^[0-9.]+:[0-9]+$/) { print $1 }'
}

fail() {
  echo "$1" >&2
  cat >&2 <<'EOF2'
On the device:
  - be on the same network as this computer
  - turn on Developer options > Wireless debugging (TVs on port 5555: USB debugging)
If it's still not found, it isn't paired with this computer: run `make pair`.
EOF2
  exit 1
}

# Plain address: classic network adb (typically Android TV on port 5555).
if [[ "$name" =~ ^[0-9.]+(:[0-9]+)?$ ]]; then
  [[ "$name" == *:* ]] || name="$name:5555"
  "$ADB" connect "$name" >&2 || true
  state="$("$ADB" -s "$name" get-state 2>&1 || true)"
  case "$state" in
    device) echo "$name"; exit 0 ;;
    *unauthorized*) echo "Accept 'Allow USB debugging?' on the device (tick 'Always allow'), then re-run." >&2; exit 1 ;;
    *) fail "Could not connect to $name: $state" ;;
  esac
fi

# No name: only allowed when exactly one wireless device is connected.
if [[ -z "$name" ]]; then
  mapfile -t serials < <(connected)
  case "${#serials[@]}" in
    1) echo "${serials[0]}"; exit 0 ;;
    0) fail "No wireless device connected." ;;
    *) echo "Several wireless devices are connected; pick one by name (PHONE= / TV=, or in local.mk):" >&2
       printf '  %s\n' "${serials[@]}" >&2; exit 1 ;;
  esac
fi

# mDNS name: already connected under that name?
find_serial() { connected | grep -m1 "^$name" || true; }
serial="$(find_serial)"

if [[ -z "$serial" ]]; then
  addr="$("$ADB" mdns services | awk -v n="$name" '$2 == "_adb-tls-connect._tcp" && index($1, n) == 1 { print $3; exit }')"
  [[ -n "$addr" ]] || fail "$name isn't advertising on the network."
  # Already connected by address (e.g. after a manual `adb connect`)?
  if connected | grep -qx "$addr"; then echo "$addr"; exit 0; fi
  echo "Reconnecting to $name at $addr" >&2
  "$ADB" connect "$addr" >&2 || true
  for _ in {1..10}; do
    serial="$(find_serial)"
    [[ -z "$serial" ]] && serial="$(connected | grep -m1 -x "$addr" || true)"
    [[ -n "$serial" ]] && break
    sleep 1
  done
  [[ -n "$serial" ]] || fail "Could not connect to $name at $addr."
fi

echo "$serial"
