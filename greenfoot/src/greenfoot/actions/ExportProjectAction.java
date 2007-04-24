package greenfoot.actions;

import greenfoot.core.GreenfootMain;
import greenfoot.gui.export.ExportDialog;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * Action to export a project to a standalone program.
 * 
 * @author Poul Henriksen, Michael Kolling
 * @version $Id: ExportProjectAction.java 4996 2007-04-24 08:45:41Z mik $
 */
public class ExportProjectAction extends AbstractAction 
{
    private static ExportProjectAction instance = new ExportProjectAction();
    private ExportDialog exportDialog;
    
    /**
     * Singleton factory method for action.
     */
    public static ExportProjectAction getInstance()
    {
        return instance;
    }

    private ExportProjectAction()
    {
        super("Export...");
        setEnabled(false);
    }

    public void actionPerformed(ActionEvent event)
    {
        if(exportDialog == null) {
            exportDialog = new ExportDialog(GreenfootMain.getInstance().getFrame());
        }
        exportDialog.display();
    }
}
