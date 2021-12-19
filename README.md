# XFTP - A SFTP Plugin for IntelliJ Ultimate Edition IDE, like IDEA, WebStorm, PHPStorm and more.

### [GitHub](https://github.com/allape/Java-IDEAPlugin-XFTP) | [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/16590-xftp)

### Screen Shots
![ScreenShot1](examples/screenshot-1.png)

### Features
- ✔️ Editing a remote file just by double-click it, and auto upload on saving.
- ✔️ Drop on remote list to upload (also available for dragging from Finder and Explorer).
- ✔️️ Dragging from local file list to remote file list to transfer, and vice versa.
- ✔️️ Open new terminal session in current folder.
- ✔️️️ Memo for history locations.
- ✔️️️ Operations in context menu: rm, cp, mv, touch, mkdir.
- ✖️ Copy and Paste actions in both list...
- ✖️ Transferring history list with retry button.
- ✖️ Custom remote file VirtualFile.

## Known Issues
- Connection can NOT be cancelled in connecting progress.
  - Wait for 30s with timeout exception...
- Shift Option R is not working in macOS.
  - Manually to reload local list.
- Shift F6 conflicts with Refactor/Rename action.
  - <= v0.10.4: Remove XFTP rename action shortcuts in Preference / Keymap.
  - \> v0.10.4: Change shortcut to Shift F7
