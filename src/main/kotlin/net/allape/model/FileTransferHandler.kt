package net.allape.model

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import javax.swing.JComponent
import javax.swing.TransferHandler

class FileTransferable<E>(
    private val files: List<E>,
    private val dataFlavor: DataFlavor? = null,
) : Transferable {

    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return if (dataFlavor == null) emptyArray() else arrayOf(dataFlavor)
    }

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
        return flavor.equals(dataFlavor)
    }

    @Throws(UnsupportedFlavorException::class)
    override fun getTransferData(flavor: DataFlavor?): Any {
        if (!isDataFlavorSupported(flavor!!)) {
            throw UnsupportedFlavorException(flavor)
        }
        return files
    }

}

abstract class FileTransferHandler<E> : TransferHandler() {
    override fun createTransferable(c: JComponent?): Transferable? {
        return FileTransferable(ArrayList<E>(0), null)
    }

    override fun getSourceActions(c: JComponent?): Int {
        return COPY
    }
}