package bluej.pkgmgr;

import bluej.utility.Debug;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.TransferHandler;

/**
 *
 * @author mik
 */
public class FileTransferHandler extends TransferHandler {
    private DataFlavor fileFlavour;
    private PkgMgrFrame pmf;
    
    public FileTransferHandler(PkgMgrFrame pmf)
    {
        fileFlavour = DataFlavor.javaFileListFlavor;
        this.pmf = pmf;
    }
    
    public boolean importData(JComponent c, Transferable t) {
        try {

            if (!canImport(c, t.getTransferDataFlavors())) {
                return false;
            }

            List<File> files = (List<File>) t.getTransferData(fileFlavour);
      //      pmf.importFromFile(files);
        } catch (UnsupportedFlavorException ex) {
            Debug.reportError("Cannot handle D&D transfer");
        } catch (IOException ex) {
            Debug.reportError("I/O exception during D7D import attempt");
        }
        return true;
    }
    
    public boolean canImport(JComponent c, DataFlavor[] flavours) {
        return hasFileFlavor(flavours);
    }

    private boolean hasFileFlavor(DataFlavor[] flavours) {
        for (int i = 0; i < flavours.length; i++) {
            if (fileFlavour.equals(flavours[i])) {
                return true;
            }
        }
        return false;
    }

}
