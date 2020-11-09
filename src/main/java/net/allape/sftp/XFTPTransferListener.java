package net.allape.sftp;

import net.schmizz.sshj.common.StreamCopier;
import net.schmizz.sshj.xfer.TransferListener;

public class XFTPTransferListener implements TransferListener {

    @Override
    public TransferListener directory(String name) {
        return this;
    }

    @Override
    public StreamCopier.Listener file(String name, long size) {
        return null;
    }

}
