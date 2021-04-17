package net.allape.model;

import javax.swing.*;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;

public abstract class FileTransferHandler<E> extends TransferHandler {

    @Override
    protected Transferable createTransferable(JComponent c) {
        return new FileTransferable<E>(new ArrayList<>(0), null);
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY;
    }

}
