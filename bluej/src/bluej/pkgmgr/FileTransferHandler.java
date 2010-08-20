/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.pkgmgr;

import bluej.utility.Debug;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.TransferHandler;

/**
 * The TransferHandler handles drop events (the tail end of Drag-and-Drop).
 * This specific TransferHandler receives file drops (drops of files).
 * 
 * It is used for the package editor (the main diagram) so that is can receive
 * Java source files via drag-and-drop.
 * 
 * @author mik
 * @version 1.0
 */
public class FileTransferHandler extends TransferHandler 
{
    private DataFlavor fileFlavour;
    private PkgMgrFrame pmf;
    
    /**
     * Create a new FileTransferHandler for a specific PackageMgrFrame.
     */
    public FileTransferHandler(PkgMgrFrame pmf)
    {
        fileFlavour = DataFlavor.javaFileListFlavor;
        this.pmf = pmf;
    }
    
    /**
     * importData - called when a drop event is received. See whether we
     * can import the dropped item, and if so, handle it.
     * 
     * @param c The component that drop occured on.
     * @param t The item being dropped.
     * @return true iff we can import the item
     */
    @SuppressWarnings("unchecked")
    public boolean importData(JComponent c, Transferable t) 
    {
        try {
            if (!canImport(c, t.getTransferDataFlavors())) {
                return false;
            }

            List<File> files = (List<File>) t.getTransferData(fileFlavour);
            pmf.addFiles(files);
        } catch (UnsupportedFlavorException ex) {
            Debug.reportError("Cannot handle D&D transfer");
        } catch (IOException ex) {
            Debug.reportError("I/O exception during D&D import attempt");
        }
        return true;
    }
    
    /**
     * Check whether we can import the given data flavours into this component.
     * This will be true if the data items are files.
     */
    public boolean canImport(JComponent c, DataFlavor[] flavours)
    {
        return hasFileFlavor(flavours);
    }

    /**
     * Check whether the data can be received as a file.
     */
    private boolean hasFileFlavor(DataFlavor[] flavours)
    {
        for (int i = 0; i < flavours.length; i++) {
            if (fileFlavour.equals(flavours[i])) {
                return true;
            }
        }
        return false;
    }

}
