# CD Companion — Android App

A second screen companion for [CD Companion](https://github.com/leandrodiogenes/cd-companion), a Crimson Desert mod that displays your real-time position on the MapGenie interactive map.

This repository contains only the Android APK releases.

---

## What the app does

- Opens the MapGenie map fullscreen on your phone or tablet
- Displays a real-time player position marker as you move
- Connects to the CD Companion server on your local network via WebSocket
- Works as a second screen — the PC runs the companion normally

---

## Requirements

- Android 8.0 (Oreo) or higher
- CD Companion running on your PC on the same Wi-Fi network
- [CD Companion for PC](https://www.nexusmods.com/crimsondesert/mods/2125)

---

## Installation

1. Download the APK from the [latest release](../../releases/latest)
2. On Android, enable **"Install from unknown sources"** for your file manager
3. Open the downloaded APK and install it
4. Open the app and go to **⋮ > Settings** to set your PC's IP address

---

## Configuration

In the app menu (three dots in the top-right corner):

- **Settings** — set the IP address and port of the PC running CD Companion
  - Default IP: `10.0.0.9`
  - Default port: `7891`

The app automatically checks for updates on startup and prompts you when a new version is available.

---

## Links

- [CD Companion on NexusMods](https://www.nexusmods.com/crimsondesert/mods/2125)
- [CD Companion repository (PC)](https://github.com/leandrodiogenes/cd-companion)
