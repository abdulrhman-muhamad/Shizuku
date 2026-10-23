# WhtsAgent Helper

WhtsAgent Helper is the privileged companion app for WhtsAgent products. It runs a long-lived
server process with ADB shell (or root) identity and exposes it over Binder IPC, so the
WhtsAgent dialer and CRM apps can use system APIs that normal apps cannot.

This app is not intended for general use. Support is provided only in the context of
WhtsAgent products.

## Building the App

- Clone with `git clone --recurse-submodules` (`api/` is a git submodule)
- Run the Gradle task `:manager:assembleDebug` or `:manager:assembleRelease`

The `:manager:assembleDebug` task generates a debuggable server. You can attach a debugger to
the `shizuku_server` process to debug it. In Android Studio, ensure
`Run/Debug configurations > Always install with package manager` is checked, so that the
server uses the latest code.

## Credits

WhtsAgent Helper is based on [Shizuku](https://github.com/RikkaApps/Shizuku) by RikkaApps, by
way of the [thedjchi](https://github.com/thedjchi/Shizuku) and
[NLL Apps](https://github.com/NLLAPPS/Shizuku) forks. It stays compatible with the standard
Shizuku client API.

## 📃 License

All code files in this project are licensed under [Apache 2.0](LICENSE).
