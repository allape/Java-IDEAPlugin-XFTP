package net.allape.sftp;

import net.schmizz.sshj.common.LoggerFactory;
import net.schmizz.sshj.xfer.LoggingTransferListener;

public class XFTPTransferListener extends LoggingTransferListener {

    public XFTPTransferListener(LoggerFactory loggerFactory) {
        super(loggerFactory);
    }

}
