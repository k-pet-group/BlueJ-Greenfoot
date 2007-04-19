/*
 * ExportAppPane.java
 *
 * Created on April 19, 2007, 6:15 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.

 * @author Michael Kolling
 * @version $Id: ExportAppPane.java 4978 2007-04-19 18:56:45Z mik $
 */

package greenfoot.gui.export;

import java.io.File;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JTextField;

public class ExportAppPane extends ExportPane
{
    private JFileChooser fileChooser;
    private JTextField targetDirField;
    
    /** Creates a new instance of ExportAppPane */
    public ExportAppPane(List<String> worlds, File defaultExportDir) 
    {
        super(worlds);
    }
    
}
