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

## Known Issues
- Connection can NOT be cancelled in connecting progress.
  - Wait for 30s with timeout exception...x
- `Shift F6` conflicts with Refactor/Rename action.
  - <= v0.10.4: Remove XFTP rename action shortcuts in `Preferences / Keymap`.
  - \> v0.10.4: Change shortcut to `Shift F7`.
  - \> v0.10.5: This shortcut has been removed, but this action can be assigned in `Preferences > Keymap`.
- Open local path selector popup while remote popup is shown, local path selector popup will disappear in a short amount of time.
  - Because the remote list will be reloaded after the remote path selector popup became hidden, no solution for this for now...
- `this.project.messageBus.connect(this).subscribe(VirtualFileManager.VFS_CHANGES, ...)` can NOT be disposed successfully.
  - This is a bug, been working on this...
- Action buttons will be disturbed if there are multiple XFTP windows in ToolWindow.
  - This is a bug too, been working on this...
