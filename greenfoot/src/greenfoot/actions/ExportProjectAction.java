package greenfoot.actions;

import bluej.Config;
import greenfoot.core.WorldHandler;
import greenfoot.gui.GreenfootFrame;
import greenfoot.gui.MessageDialog;
import greenfoot.gui.export.ExportDialog;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;

/**
 * Action to export a project to a standalone program.
 * 
 * @author Poul Henriksen, Michael Kolling
 * @version $Id: ExportProjectAction.java 5865 2008-09-10 15:29:05Z polle $
 */
public class ExportProjectAction extends AbstractAction 
{
    private static final String dialogTitle = Config.getString("export.noworld.dialog.title");
    private static final String dialogMsg = Config.getString("export.noworld.dialog.msg");
    
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
        if(WorldHandler.getInstance().getLastWorldClass() == null) {
            JButton[] buttons = new JButton[]{new JButton(Config.getString("greenfoot.continue"))};
            MessageDialog errorDialog = new MessageDialog(gfFrame, dialogMsg, dialogTitle, 50 , buttons);
            errorDialog.display();
            return;
        }
       
        if(exportDialog == null) {
            exportDialog = new ExportDialog(gfFrame);
        }
        exportDialog.display();
    }
}
