# NexShell

[![Build NexShell](https://github.com/hastagaming/nexshell/actions/workflows/build.yml/badge.svg)](https://github.com/hastagaming/nexshell/actions/workflows/build.yml)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[![](https://jitpack.io/v/termux/termux-app.svg)](https://jitpack.io/#termux/termux-app)

**One Android App. Multiple Linux Systems.**

NexShell is an Android terminal application that runs multiple fully
isolated Linux workspaces on a single device. Each workspace — Ubuntu,
Debian, Alpine, or a custom rootfs — owns its own filesystem, `$HOME`,
environment variables, package manager, shell, processes, sessions, and
services. Workspaces are not tabs of the same terminal; they are
independent systems that happen to share one Android app shell.

Built and developed entirely on-device in [Termux](https://termux.dev)
on a Realme Note 50 — no desktop toolchain involved.

## Status

NexShell is implemented in phases. Each phase below produces real,
compilable code — no mocks, no stubs, no placeholder UI.

| Phase | Scope | Status |
|---|---|---|
| 1 | Workspace Manager, Workspace model/storage, `.properties` config system, Workspace Switcher UI | Done |
| 2 | Native PTY (JNI), ANSI/VT100 Terminal Engine, Multiple Sessions, Split Terminal | Done |
| 3 | RootFS Manager (install/import/export/remove), real isolation via `proot` | Done |
| 4 | Snapshot, Clone, Workspace Profiles, Service Manager, Auto Start | Done |
| 5 | `nexshell-setup`, Yazi file manager integration, Font system, Theme system | Done |
| 6 | Foreground Service, dynamic notification, Exit action, Acquire/Release Wakelock | Done |

## Architecture

- **UI**: Kotlin + Jetpack Compose + Material 3
- **PTY**: native C++ via JNI (`posix_openpt` / `fork` / `execve`), not a
  text-only imitation terminal
- **Workspace isolation**: real per-workspace directory trees under
  `/data/data/com.nexshell/<workspace-id>/files/{home,usr,etc,var}`
- **RootFS isolation**: [proot](https://github.com/proot-me/proot) built
  from its official source and executed as a real chroot-style process
  boundary per workspace — the same underlying approach used by Termux
- **File manager**: [Yazi](https://github.com/sxyazi/yazi), installed
  from its official prebuilt release binaries and run inside the active
  workspace's shell session
- **Foreground Service**: real Android `Service` tracking live sessions
  and background service processes, with a dynamic notification and a
  genuine `PowerManager` partial wakelock

## Workspace layout

Each workspace is a sibling directory, fully separate from every other
workspace:

```Code
/data/data/com.nexshell/
├── ubuntu/
│   ├── files/{home,usr,etc,var}/
│   ├── ubuntu.properties
│   └── snapshots/
├── debian/
│   ├── files/{home,usr,etc,var}/
│   ├── debian.properties
│   └── snapshots/
└── alpine/
├── files/{home,usr,etc,var}/
├── alpine.properties
└── snapshots/
```

No workspace shares a rootfs, `$HOME`, session, or process with another.

## Building

### Requirements

- android 8+
- android 15

## Known limitations

- The ANSI/VT100 parser in TerminalEngine covers cursor movement,
erase, and SGR color (16/256) — enough for standard shell usage, but
full-screen TUI programs (vim, htop) will need alternate-screen
buffer support, which is not yet implemented.

- Nerd Font .ttf assets and official rootfs SHA-256 checksums are not
bundled in source (binary/release-specific data) and must be added
manually — see app/src/main/assets/fonts/.

- Building proot from source requires network access at first CMake
configure; fully offline builds need the source vendored locally.

## License

MIT License — see [LICENSE](LICENSE) for full text.