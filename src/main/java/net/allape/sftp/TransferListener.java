package net.allape.sftp;

import net.schmizz.sshj.common.LoggerFactory;
import net.schmizz.sshj.xfer.LoggingTransferListener;

public class TransferListener extends LoggingTransferListener {

    public TransferListener(LoggerFactory loggerFactory) {
        super(loggerFactory);
    }

}
