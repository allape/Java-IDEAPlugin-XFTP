package net.allape.model;

import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.List;

public class FileTransferable<E> implements Transferable {

    private final List<E> files;

    private final DataFlavor dataFlavor;

    public FileTransferable(List<E> files, DataFlavor dataFlavor) {
        this.files = files;
        this.dataFlavor = dataFlavor;
    }

    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{this.dataFlavor};
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(this.dataFlavor);
    }

    @NotNull
    @Override
    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException {
        if (!isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        return files;
    }
}
