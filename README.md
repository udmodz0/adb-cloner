<div align="center">

# 🌌 CloneSpace

### *Rootless Multi-User Workspace & App Cloner for Android*

<img src="https://readme-typing-svg.demolab.com?font=Fira+Code&weight=700&size=24&duration=3000&pause=1000&color=00F2FF&center=true&vCenter=true&width=700&lines=CloneSpace+by+UDMODZ;Elevated+Shizuku+Shell+Binder+IPC;Zero-APK+Instant+Cloning+via+pm;Isolated+Android+Workspaces+%26+Shortcuts;No+Root+%E2%80%A2+No+PC+Required" alt="Typing SVG" />

<p align="center">
  <a href="https://whatsapp.com/channel/0029Vb5uLwF7z4kgGnpfGU3D">
    <img src="https://img.shields.io/badge/WhatsApp-Channel-25D366?logo=whatsapp&logoColor=white&style=for-the-badge" alt="WhatsApp Channel" />
  </a>
  <a href="https://wa.me/94704638406">
    <img src="https://img.shields.io/badge/WhatsApp-Chat-128C7E?logo=whatsapp&logoColor=white&style=for-the-badge" alt="WhatsApp Contact" />
  </a>
  <a href="https://t.me/udmodz0">
    <img src="https://img.shields.io/badge/Telegram-Channel-24A1DE?logo=telegram&logoColor=white&style=for-the-badge" alt="Telegram Channel" />
  </a>
  <a href="https://udmodz.site">
    <img src="https://img.shields.io/badge/Website-udmodz.site-00F2FF?logo=googlechrome&logoColor=black&style=for-the-badge" alt="UDMODZ Website" />
  </a>
  <a href="https://github.com/udmodz0">
    <img src="https://img.shields.io/badge/GitHub-udmodz0-181717?logo=github&logoColor=white&style=for-the-badge" alt="GitHub" />
  </a>
</p>

<p align="center">
  <a href="https://github.com/udmodz0/adb-cloner/releases/latest">
    <img src="https://img.shields.io/github/v/release/udmodz0/adb-cloner?color=00F2FF&label=Latest%20Release&style=for-the-badge&logo=github" alt="Latest Release" />
  </a>
  <a href="https://github.com/udmodz0/adb-cloner/releases/download/v1.0.0/CloneSpace-v1.0.0.apk">
    <img src="https://img.shields.io/badge/Download-APK%20(v1.0.0)-success?style=for-the-badge&logo=android&logoColor=white" alt="Download APK" />
  </a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white&style=flat-square" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-2024.11.00-4285F4?logo=jetpackcompose&logoColor=white&style=flat-square" alt="Compose" />
  <img src="https://img.shields.io/badge/Material%203-Ready-FF7043?style=flat-square" alt="Material 3" />
  <img src="https://img.shields.io/badge/Android-SDK%2026--35-3DDC84?logo=android&logoColor=white&style=flat-square" alt="Android" />
  <img src="https://img.shields.io/badge/Shizuku%20API-v13.1.5-303F9F?style=flat-square" alt="Shizuku" />
  <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square" alt="License" />
</p>

---

</div>

## 📌 Introduction

**CloneSpace** is a modern, rootless Android application developed by **UDhanika Dissanayaka ([UDMODZ](https://udmodz.site))**. Built using Kotlin, Jetpack Compose, and Material 3, CloneSpace leverages Android's native multi-user architecture via elevated **Shizuku IPC** (`android.uid.shell`).

Run multiple isolated instances of WhatsApp, Telegram, banking apps, or games in sandboxed profiles without third-party virtualization overhead, battery drain, or root access.

---

## ✨ Features

- ⚡ **Rootless & Wireless**: Works standalone via Shizuku without requiring root or computer tethering.
- 🏢 **Managed Work Profiles**: One-click creation of sandboxed work profiles (`pm create-user --profileOf 0 --managed`).
- 👥 **Secondary Workspaces**: Create independent user profiles with separate storage and accounts.
- 📦 **Zero-Copy APK Cloning**: Instant cloning via Android's package manager (`pm install-existing --user <id> <pkg>`). No duplicate APK downloads or storage bloat.
- 🚀 **Direct App Launcher**: Modern launcher using `am start --user <id>` supporting Android 8 to 16.
- 📌 **Home Screen Shortcuts**: Pin 1-tap shortcuts directly onto your primary home screen via `ShortcutManagerCompat`.
- 📊 **Real-time Shell Stream**: Live inspection of ADB commands, stdout, stderr, and execution codes with one-tap clipboard copy.
- 🛡️ **Primary User Safeguard**: Hard-coded safety protection preventing deletion or stopping of User 0.
- 🏎️ **Ultra-Optimized Architecture**: 1-call batch package inspection, 72px downsampled memory-safe icon thumbnails, and largeHeap support.

---

## 📱 OEM Compatibility & Workarounds

| Manufacturer | Problem | Solution |
| :--- | :--- | :--- |
| **ZTE / Nubia** | `pm create-user` returns `Maximum user limit is reached (code 6)` | Use **Work / Managed Profile** (`pm create-user --profileOf 0 --managed`), which operates under a separate quota. |
| **Xiaomi / POCO / Redmi** | `INSTALL_FAILED_USER_RESTRICTED` or `SecurityException` | Go to **Developer Options** ➔ Enable **Install via USB** & **USB Debugging (Security settings)**. |
| **Oppo / OnePlus / Realme** | Max users limit reached | Ensure **Settings ➔ Multiple Users** is toggled ON. ColorOS caps total profiles at 4-5. |
| **Samsung (OneUI)** | Knox MDM restrictions | Works natively. If corporate Knox restricts work profiles, use standard secondary profiles. |

---

## 🛠️ ADB Commands Cheatsheet

```bash
# List all active user workspaces
pm list users

# Create a Managed Work Profile (Recommended)
pm create-user --profileOf 0 --managed "WorkSpace"

# Create a Standard Secondary User
pm create-user "Secondary"

# Start / Spin up a workspace
am start-user <user_id>

# Stop / Freeze a workspace
am stop-user -f <user_id>

# Delete workspace and wipe its data (Guarded against user 0)
pm remove-user <user_id>

# Clone app into target workspace
pm install-existing --user <user_id> <package_name>

# Uninstall cloned app from target workspace only
pm uninstall --user <user_id> <package_name>

# Launch app in target workspace (Android 14/15/16)
am start --user <user_id> -n <package_name>/<activity_name>
```

---

## 📦 How to Build from Source

### Prerequisites
- JDK 17+ or JDK 21 (e.g. from Android Studio JBR).
- Android SDK Platform 35 and Build-Tools 35.0.0.

### Build via Command Line
```bash
# Clone the repository
git clone https://github.com/udmodz0/adb-cloner.git
cd adb-cloner

# Build Debug APK
./gradlew assembleDebug

# Install onto your connected Android device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 👨‍💻 Developer & Community

<div align="center">

Developed with ❤️ by **UDhanika Dissanayaka (UDMODZ)**  
*Full Stack Developer & Software Architect*

<p align="center">
  <a href="https://whatsapp.com/channel/0029Vb5uLwF7z4kgGnpfGU3D">
    <img src="https://img.shields.io/badge/Join-WhatsApp_Channel-25D366?style=for-the-badge&logo=whatsapp&logoColor=white" alt="WhatsApp Channel" />
  </a>
  <a href="https://wa.me/94704638406">
    <img src="https://img.shields.io/badge/Contact-WhatsApp_Direct-128C7E?style=for-the-badge&logo=whatsapp&logoColor=white" alt="WhatsApp Direct" />
  </a>
  <a href="https://t.me/udmodz0">
    <img src="https://img.shields.io/badge/Follow-Telegram_Channel-24A1DE?style=for-the-badge&logo=telegram&logoColor=white" alt="Telegram Channel" />
  </a>
</p>

| Channel | Link |
| :--- | :--- |
| 🌐 **Official Website** | [https://udmodz.site](https://udmodz.site) |
| 📦 **Source Repository** | [github.com/udmodz0/adb-cloner](https://github.com/udmodz0/adb-cloner) |
| 💬 **WhatsApp Channel** | [Follow UDMODZ](https://whatsapp.com/channel/0029Vb5uLwF7z4kgGnpfGU3D) |
| 📱 **WhatsApp Support** | [+94 70 463 8406](https://wa.me/94704638406) |
| 📢 **Telegram Updates** | [@udmodz0](https://t.me/udmodz0) |
| 💬 **Telegram Contact** | [@udmodz](https://t.me/udmodz) |
| 💻 **GitHub Profile** | [github.com/udmodz0](https://github.com/udmodz0) |
| 📺 **YouTube** | [@udmodz](https://youtube.com/@udmodz) |
| ✉️ **Support Email** | `support@udmodz.site` |

</div>

---

## 📜 License
```
Copyright 2026 UDMODZ (UDhanika Dissanayaka)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
