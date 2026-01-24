<div align="center">
   
# Shizuku

An Android app that allows other apps to use system-level APIs that require ADB/root privileges.

[![Stars](https://img.shields.io/github/stars/thedjchi/Shizuku?style=for-the-badge&color=bfb330&labelColor=807820&logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHRpdGxlPnN0YXI8L3RpdGxlPjxwYXRoIGQ9Ik0xMiwxNy4yN0wxOC4xOCwyMUwxNi41NCwxMy45N0wyMiw5LjI0TDE0LjgxLDguNjJMMTIsMkw5LjE5LDguNjJMMiw5LjI0TDcuNDUsMTMuOTdMNS44MiwyMUwxMiwxNy4yN1oiIGZpbGw9IndoaXRlIiAvPjwvc3ZnPg==)](https://github.com/thedjchi/Shizuku/stargazers)
[![Downloads](https://img.shields.io/github/downloads/thedjchi/Shizuku/total?style=for-the-badge&color=bf7830&labelColor=805020&logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHRpdGxlPmRvd25sb2FkPC90aXRsZT48cGF0aCBkPSJNNSwyMEgxOVYxOEg1TTE5LDlIMTVWM0g5VjlINUwxMiwxNkwxOSw5WiIgZmlsbD0id2hpdGUiIC8+PC9zdmc+)](https://github.com/thedjchi/Shizuku/releases)
[![Latest Version](https://img.shields.io/github/v/release/thedjchi/Shizuku?style=for-the-badge&color=3060bf&labelColor=204080&label=Latest%20Version&logo=data:image/svg+xml;base64,PCFET0NUWVBFIHN2ZyBQVUJMSUMgIi0vL1czQy8vRFREIFNWRyAxLjEvL0VOIiAiaHR0cDovL3d3dy53My5vcmcvR3JhcGhpY3MvU1ZHLzEuMS9EVEQvc3ZnMTEuZHRkIj4KDTwhLS0gVXBsb2FkZWQgdG86IFNWRyBSZXBvLCB3d3cuc3ZncmVwby5jb20sIFRyYW5zZm9ybWVkIGJ5OiBTVkcgUmVwbyBNaXhlciBUb29scyAtLT4KPHN2ZyB3aWR0aD0iODAwcHgiIGhlaWdodD0iODAwcHgiIHZpZXdCb3g9IjAgMCA1MTIgNTEyIiB2ZXJzaW9uPSIxLjEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgeG1sbnM6eGxpbms9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkveGxpbmsiIGZpbGw9IiMwMDAwMDAiIGRhdGEtZGFya3JlYWRlci1pbmxpbmUtZmlsbD0iIiBzdHlsZT0iLS1kYXJrcmVhZGVyLWlubGluZS1maWxsOiB2YXIoLS1kYXJrcmVhZGVyLWJhY2tncm91bmQtMDAwMDAwLCAjMDAwMDAwKTsiPgoNPGcgaWQ9IlNWR1JlcG9fYmdDYXJyaWVyIiBzdHJva2Utd2lkdGg9IjAiLz4KDTxnIGlkPSJTVkdSZXBvX3RyYWNlckNhcnJpZXIiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCIvPgoNPGcgaWQ9IlNWR1JlcG9faWNvbkNhcnJpZXIiPiA8dGl0bGU+dmVyc2lvbi1oaXN0b3J5PC90aXRsZT4gPGcgaWQ9IlBhZ2UtMSIgc3Ryb2tlPSJub25lIiBzdHJva2Utd2lkdGg9IjEiIGZpbGw9Im5vbmUiIGZpbGwtcnVsZT0iZXZlbm9kZCIgZGF0YS1kYXJrcmVhZGVyLWlubGluZS1zdHJva2U9IiIgc3R5bGU9Ii0tZGFya3JlYWRlci1pbmxpbmUtc3Ryb2tlOiBub25lOyI+IDxnIGlkPSJ2ZXJzaW9uLWhpc3RvcnkiIHRyYW5zZm9ybT0idHJhbnNsYXRlKC0wLjAwMDAwMCwgMC4wMDAwMDApIiBmaWxsPSIjZmZmZmZmIiBkYXRhLWRhcmtyZWFkZXItaW5saW5lLWZpbGw9IiIgc3R5bGU9Ii0tZGFya3JlYWRlci1pbmxpbmUtZmlsbDogdmFyKC0tZGFya3JlYWRlci1iYWNrZ3JvdW5kLWZmZmZmZiwgIzE4MWExYik7Ij4gPGcgaWQ9Imljb24iIHRyYW5zZm9ybT0idHJhbnNsYXRlKDEyLjQ5Njc3NywgNjQuMDAwMDAwKSI+IDxwYXRoIGQ9Ik0yNjQuODM2NTU2LDEuNDIxMDg1NDdlLTE0IEMzNzAuODc1MjI4LDEuNDIxMDg1NDdlLTE0IDQ1Ni44MzY1NTYsODUuOTYxMzI4IDQ1Ni44MzY1NTYsMTkyIEM0NTYuODM2NTU2LDI5OC4wMzg2NzIgMzcwLjg3NTIyOCwzODQgMjY0LjgzNjU1NiwzODQgQzIxMi43NjI5ODQsMzg0IDE2NS41MzEyNzQsMzYzLjI2OTUyMyAxMzAuOTQ0MDIzLDMyOS42MTExNjUgTDE2MC42OTg1NSwyOTkuMDMxNTIgQzE4Ny41OTk2ODksMzI1LjIwOTg4MSAyMjQuMzM1MjA2LDM0MS4zMzMzMzMgMjY0LjgzNjU1NiwzNDEuMzMzMzMzIEMzNDcuMzExMDc5LDM0MS4zMzMzMzMgNDE0LjE2OTg4OSwyNzQuNDc0NTIzIDQxNC4xNjk4ODksMTkyIEM0MTQuMTY5ODg5LDEwOS41MjU0NzcgMzQ3LjMxMTA3OSw0Mi42NjY2NjY3IDI2NC44MzY1NTYsNDIuNjY2NjY2NyBDMTgyLjM2MjAzMyw0Mi42NjY2NjY3IDExNS41MDMyMjMsMTA5LjUyNTQ3NyAxMTUuNTAzMjIzLDE5MiBDMTE1LjUwMzIyMywxOTYuMDQyMjM2IDExNS42NjM4MjksMjAwLjA0Njk2MSAxMTUuOTc5MDA4LDIwNC4wMDgxNDEgTDE1OC4xNjk4ODksMTYxLjgzMDExMSBMMTg4LjMzOTc3OSwxOTIgTDk0LjE2OTg4OTMsMjg2LjE2OTg4OSBMMS43NzYzNTY4NGUtMTQsMTkyIEwzMC4xNjk4ODkzLDE2MS44MzAxMTEgTDczLjI2NDEwNTksMjA0LjkxNTcyMiBDNzIuOTgwNTc4NywyMDAuNjQ3MTMyIDcyLjgzNjU1NiwxOTYuMzQwMzg1IDcyLjgzNjU1NiwxOTIgQzcyLjgzNjU1Niw4NS45NjEzMjggMTU4Ljc5Nzg4NCwxLjQyMTA4NTQ3ZS0xNCAyNjQuODM2NTU2LDEuNDIxMDg1NDdlLTE0IFogTTIzMS44MjQwMjUsMTI3LjEyNjkxMyBMMjY1LjAzODcxNCwyMTUuNjgyMjEzIEwyOTguMjUyMDI1LDEyNy4xMjY5MTMgTDM0My44MjIwMjUsMTI3LjEyNjkxMyBMMjg3LjgyMjAyNSwyNzYuNDU5OTEzIEwyNDIuMjU0MDI1LDI3Ni40NTk5MTMgTDE4Ni4yNTUwMjUsMTI3LjEyNjkxMyBMMjMxLjgyNDAyNSwxMjcuMTI2OTEzIFoiIGlkPSJDb21iaW5lZC1TaGFwZSI+IDwvcGF0aD4gPC9nPiA8L2c+IDwvZz4gPC9nPgoNPC9zdmc+)](https://github.com/thedjchi/Shizuku/releases/latest)

[![Bug Reports](https://img.shields.io/github/issues-search/thedjchi/Shizuku?query=label%3Abug%20state%3Aopen&style=for-the-badge&color=bf3030&labelColor=802020&label=Bug%20Reports)](https://github.com/thedjchi/Shizuku/issues?q=is%3Aissue%20state%3Aopen%20label%3Abug)
[![Feature Requests](https://img.shields.io/github/issues-search/thedjchi/Shizuku?query=label%3Aenhancement%20state%3Aopen&style=for-the-badge&color=30a7bf&labelColor=207080&label=Feature%20Requests)](https://github.com/thedjchi/Shizuku/issues?q=is%3Aissue%20state%3Aopen%20label%3Aenhancement)

[![Translate on Crowdin](https://img.shields.io/badge/Translate%20on%20Crowdin-2e3340?style=for-the-badge&logo=crowdin&logoColor=ffffff)](https://crowdin.com/project/shizuku)
[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20A%20Coffee-bfb330?style=for-the-badge&logo=buymeacoffee&logoColor=ffffff)](https://www.buymeacoffee.com/thedjchi)

</div>

### Disclaimer

This is a **FORK** of Shizuku. If you are looking for the original version, please visit the [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku) repository.



### Added Features

This version of Shizuku includes some extra features over the original version, such as:
* **More robust "start on boot":** waits for a Wi-Fi connection before starting the Shizuku service
* **TCP mode:** (i.e., the `adb tcpip` command) once Shizuku successfully starts with Wi-Fi after a reboot, you can stop/restart Shizuku without a Wi-Fi connection!
* **Watchdog service:** automatically restarts Shizuku if it stops unexpectedly, and can alert you of crashes/potential fixes
* **Start/stop intents**: toggle Shizuku on-demand using automation apps (e.g., Tasker, MacroDroid, Automate)
* **Android/Google TV and VR headset support:** UI is now compatible with D-Pad remotes, all TVs are supported (including Android 14+ TVs that require pairing), and the multi-window pairing dialog is toggleable in settings for VR headsets
* **MediaTek support:** fixes a critical bug in the original v13.6.0 which prevented Shizuku from working on MediaTek devices
* And more!

## User guide & Download
Please read the [wiki](https://github.com/thedjchi/Shizuku/wiki) for setup and troubleshooting instructions from this fork.

- **User Guide**: <https://shizuku.rikka.app/>
- **Download latest**: <https://github.com/thedjchi/Shizuku/releases/latest>
- **Understanding how Shizuku work behind the scene** *(rundown/simplified version for new Shizuku users)*: [HOW_IT_WORKS.md](./HOW_IT_WORKS.md)

### Translations

Contribute translations through the [Crowdin project](https://crowdin.com/project/shizuku).

### Donations

This Shizuku fork and all of its features will always be free, and there will never be ads. If you've found any of the added features to be useful, consider [donating](https://www.buymeacoffee.com/thedjchi) to help me maintain the project!

## Background

When developing apps that requires root, the most common method is to run some commands in the su shell. For example, there is an app that uses the `pm enable/disable` command to enable/disable components.

This method has very big disadvantages:

1. **Extremely slow** (Multiple process creation)
2. Needs to process texts (**Super unreliable**)
3. The possibility is limited to available commands
4. Even if ADB has sufficient permissions, the app requires root privileges to run

Shizuku uses a completely different way. See detailed description below.

## How does Shizuku work?

First, we need to talk about how app use system APIs. For example, if the app wants to get installed apps, we all know we should use `PackageManager#getInstalledPackages()`. This is actually an interprocess communication (IPC) process of the app process and system server process, just the Android framework did the inner works for us.

Android uses `binder` to do this type of IPC. `Binder` allows the server-side to learn the uid and pid of the client-side, so that the system server can check if the app has the permission to do the operation.

Usually, if there is a "manager" (e.g., `PackageManager`) for apps to use, there should be a "service" (e.g., `PackageManagerService`) in the system server process. We can simply think if the app holds the `binder` of the "service", it can communicate with the "service". The app process will receive binders of system services on start.

Shizuku guides users to run a process, Shizuku server, with root or ADB first. When the app starts, the `binder` to Shizuku server will also be sent to the app.

The most important feature Shizuku provides is something like be a middle man to receive requests from the app, sent them to the system server, and send back the results. You can see the `transactRemote` method in `rikka.shizuku.server.ShizukuService` class, and `moe.shizuku.api.ShizukuBinderWrapper` class for the detail.

So, we reached our goal, to use system APIs with higher permission. And to the app, it is almost identical to the use of system APIs directly.

## Developer guide

### API & sample

https://github.com/RikkaApps/Shizuku-API

### Migrating from pre-v11

> Existing applications still works, of course.

https://github.com/RikkaApps/Shizuku-API#migration-guide-for-existing-applications-use-shizuku-pre-v11

### Attention

1. ADB permissions are limited

   ADB has limited permissions and different on various system versions. You can see permissions granted to ADB [here](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/packages/Shell/AndroidManifest.xml).

   Before calling the API, you can use `ShizukuService#getUid` to check if Shizuku is running user ADB, or use `ShizukuService#checkPermission` to check if the server has sufficient permissions.

2. Hidden API limitation from Android 9

   As of Android 9, the usage of the hidden APIs is limited for normal apps. Please use other methods (such as <https://github.com/LSPosed/AndroidHiddenApiBypass>).

3. Android 8.0 & ADB

   At present, the way Shizuku service gets the app process is to combine `IActivityManager#registerProcessObserver` and `IActivityManager#registerUidObserver` (26+) to ensure that the app process will be sent when the app starts. However, on API 26, ADB lacks permissions to use `registerUidObserver`, so if you need to use Shizuku in a process that might not be started by an Activity, it is recommended to trigger the send binder by starting a transparent activity.

4. Direct use of `transactRemote` requires attention

   * The API may be different under different Android versions, please be sure to check it carefully. Also, the `android.app.IActivityManager` has the aidl form in API 26 and later, and `android.app.IActivityManager$Stub` exists only on API 26.

   * `SystemServiceHelper.getTransactionCode` may not get the correct transaction code, such as `android.content.pm.IPackageManager$Stub.TRANSACTION_getInstalledPackages` does not exist on API 25 and there is `android.content.pm.IPackageManager$Stub.TRANSACTION_getInstalledPackages_47` (this situation has been dealt with, but it is not excluded that there may be other circumstances). This problem is not encountered with the `ShizukuBinderWrapper` method.

## Developing Shizuku itself

### Build

- Clone with `git clone --recurse-submodules`
- Run gradle task `:manager:assembleDebug` or `:manager:assembleRelease`

The `:manager:assembleDebug` task generates a debuggable server. You can attach a debugger to `shizuku_server` to debug the server. Be aware that, in Android Studio, "Run/Debug configurations" - "Always install with package manager" should be checked, so that the server will use the latest code.

## License

All code files in this project are licensed under Apache 2.0

Under Apache 2.0 section 6, specifically:

* You are **FORBIDDEN** to use `manager/src/main/res/mipmap*/ic_launcher*.png` image files, unless for displaying Shizuku itself.

* You are **FORBIDDEN** to use `Shizuku` as app name or use `moe.shizuku.privileged.api` as application id or declare `moe.shizuku.manager.permission.*` permission.
