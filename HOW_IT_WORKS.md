[Back to README](./README.md)
# How does Shizuku work behind the scenes?
*This parts focus on the ADB mode, rooted devices do not need to use ADB and are more straight forward, Shizuku directly open a shell.*

Shizuku is a "handler" that gives out access to ADB for external apps. ADB ([Android Debug Bridge](https://developer.android.com/tools/adb)) allows a developer to run power-user commands on the Android OS, usually used for debugging applications and Android itself. These commands are very similar to having a rooted phone, with some limitations we won't go into details here.

>[!IMPORTANT]
>If you want detailed instructions on setting up Shizuku, look at the [rikka website](https://shizuku.rikka.app/guide/setup/#user-manual) or at this [archive mirror](https://web.archive.org/web/20260120123643/https://shizuku.rikka.app/guide/setup/#user-manual) from 2026. **This markdown file will not explain how to install Shizuku with step-by-step explanations, we explain a simplified version of the "behind the scene"**.

## On the very first installation of Shizuku: SETUP
- For devices **before Android 11**, the first setup (and every future start after a phone reboot) will require a computer with a physical connection.
- **From Android 11 and onward**, you no longer need a computer and a physical connection, as they added **Wireless Debugging** (debugging over "WiFi/Local Networks").

**During the initial setup:**
1. The user must enable **USB Debugging** and **Wireless Debugging** manually to allow Shizuku to connect to the device using ADB.
2. Shizuku will then guide the user in the Wireless Debugging pairing process so it is allowed to connect. This is a one-time process. The pairing process (with code) should no longer be necessary afterward.
3. Once paired, Shizuku will grant itself the **WRITE_SECURE_SETTINGS** permission,  once it has successfully connected to ADB, it will use the **WRITE_SECURE_SETTINGS** permission to disable **Wireless Debugging**.

>[!NOTE]
>**Wireless Debugging** only needs to be enabled for initializing a ADB connection. Once connected, it is no longer needed, and **disabling it prevents other devices from trying to connect to your device**. Note that when any new device tries pairing for the first time, it requires a unique code shown on your device. Disabling **Wireless Debugging** is more of an additional safety precaution against CVE or brute-force attacks.

**During every subsequent use:**

1. Shizuku already has the **WRITE_SECURE_SETTINGS** permission.
2. It verifies that **USB Debugging** is enabled. If not, it enables it to keep its future ADB connection alive.
3. It enables **Wireless Debugging**, connects using ADB, and finally disables **Wireless Debugging** again.
4. NOT-YET-IMPLEMENTED https://github.com/thedjchi/Shizuku/issues/110: **Assuming you enabled the toggle to disable USB Debugging *when not needed***: Shizuku closes its ADB connection, and the **WRITE_SECURE_SETTINGS** permission will be used to disable **USB Debugging** until the next start.

>[!CAUTION]
>USB Debugging ***MUST*** stay enabled to keep any type of ***ongoing*** ADB connection alive, even **Wireless Debugging** ones. However, having **USB Debugging** enabled exposes your device to more security risks. Therefore, an option was added to disable **USB Debugging** as soon as it is no longer needed to improve the security and integrity of your device. Read below.

# How do Shizuku's features behave behind the scenes?

## Boot on [startup](https://github.com/thedjchi/Shizuku/blob/master/manager/src/main/java/moe/shizuku/manager/receiver/BootCompleteReceiver.kt)
1. If the Watchdog monitoring feature is enabled, start the monitoring.
2. Create a notification to keep running in the background.
3. If TCP Mode is disabled, wait until it detect the device is connected to a UNMETERED Wifi connection. This is required to connect using **Wireless Debugging**.
4. Start Shizuku --> [Click here to see steps](#on-the-very-first-installation-of-shizuku-setup).

## [Watchdog](https://github.com/thedjchi/Shizuku/blob/master/manager/src/main/java/moe/shizuku/manager/service/WatchdogService.kt)
TODO: Not sure how to explains this well in a simplified manner what the general intent of the feature is : crash detection, auto restart, help notification, etc.

## TCP Mode
TODO: Not sure how to explains this well in a simplified manner, I did not use `adb tcpip` much. I think there was also some security consideration with encryption when this get enabled? Maybe im getting confused with another closed issue can't find it anymore.

## [Start](https://github.com/thedjchi/Shizuku/blob/master/manager/src/main/java/moe/shizuku/manager/receiver/ManualStartReceiver.kt)/[Stop](https://github.com/thedjchi/Shizuku/blob/master/manager/src/main/java/moe/shizuku/manager/receiver/ManualStopReceiver.kt) Intents
Toggle Shizuku on-demand using automation apps (by firing intents events).
Upon receiving a event, the application will trigger the start/stop actions.

| **Action**                                | **Package**                      | **Class**                                      | **Target**          |
|-------------------------------------------|----------------------------------|------------------------------------------------|--------------------|
| `moe.shizuku.privileged.api.START`        | `moe.shizuku.privileged.api`     | `moe.shizuku.manager.receiver.ManualStartReceiver` | `Broadcast Receiver` |
| `moe.shizuku.privileged.api.STOP`         | `moe.shizuku.privileged.api`     | `moe.shizuku.manager.receiver.ManualStopReceiver`  | `Broadcast Receiver` |

**Security Consideration**: WIP:https://github.com/thedjchi/Shizuku/issues/111

## Biometrics
WIP:https://github.com/thedjchi/Shizuku/issues/112

