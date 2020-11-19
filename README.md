# XFTP - A SFTP tools for IntelliJ IDEA Ultimate

## Features
- ✔️ Editing remote files just by double-click it, and saving (Ctrl s or Cmd s by default) for uploading.
- ✔️ Drop on remote view port to upload (just available for dragging from Finder or Explorer).
- ✖️ Dragging from local file view port to remote file view port, and vice versa.
- ✖️ Transferring history list with re-try button.
- ✖️ More operations in file view port: delete, copy, paste, etc...

## Known Issues
- Invoking methods of RemoteFileObject will block UI thread while uploading or downloading. 
- Make sure the double-clicked remote file is not in the downloading queue.
- Folder uploading progress(Task.Backgroundable) calculation error.
- Folder uploading missing dragging/selecting folder(Only uploaded children files and folders).
- Download/upload queue is not suitable for multi-XFTPWindow

## Nuts-pain
```java
ConnectionBuilder connectionBuilder = RemoteCredentialsUtil.connectionBuilder(data, this.project);
SftpChannel sftpChannel = connectionBuilder.openSftpChannel();
sftpChannel.uploadFileOrDir(local, remote, ""); // -> throws some exceptions because the remote variable has been appended a "/" at its tail
```

## Enhancements
- UI needs obey [IntelliJ Platform UI Guidelines](https://jetbrains.github.io/ui/)
- Uploading and downloading performance test.
- English JavaDoc and comments maybe?
- More comfortable usage of SSHChannel?
