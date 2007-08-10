package greenfoot.actions;

import greenfoot.gui.GreenfootFrame;
import greenfoot.gui.export.ExportDialog;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * Action to export a project to a standalone program.
 * 
 * @author Poul Henriksen, Michael Kolling
 * @version $Id: ExportProjectAction.java 5154 2007-08-10 07:02:51Z davmac $
 */
public class ExportProjectAction extends AbstractAction 
{
    private ExportDialog exportDialog;
    private GreenfootFrame gfFrame;
    
    public ExportProjectAction(GreenfootFrame gfFrame)
    {
        super("Export...");
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
