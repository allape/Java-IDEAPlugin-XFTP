package net.allape.models;

import javax.swing.*;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;

public abstract class FileTransferHandler<E> extends TransferHandler {

    @Override
    protected Transferable createTransferable(JComponent c) {
        return new FileTransferable(new ArrayList<E>(0), null);
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY;
    }

}
