package greenfoot.actions;

import bluej.Config;
import greenfoot.gui.GreenfootFrame;
import greenfoot.gui.export.ExportDialog;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * Action to export a project to a standalone program.
 * 
 * @author Poul Henriksen, Michael Kolling
 * @version $Id: ExportProjectAction.java 5284 2007-10-04 04:09:40Z bquig $
 */
public class ExportProjectAction extends AbstractAction 
{
    private ExportDialog exportDialog;
    private GreenfootFrame gfFrame;
    
    public ExportProjectAction(GreenfootFrame gfFrame)
    {
        super(Config.getString("export.project"));
        this.gfFrame = gfFrame;
        setEnabled(false);
    }

    public void actionPerformed(ActionEvent event)
    {
        if(exportDialog == null) {
            exportDialog = new ExportDialog(gfFrame);
        }
        exportDialog.display();
    }
}
