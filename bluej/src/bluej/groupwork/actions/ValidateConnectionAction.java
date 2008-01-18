package bluej.groupwork.actions;

import java.awt.Dialog;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import bluej.groupwork.TeamSettings;
import bluej.groupwork.TeamworkProvider;
import bluej.groupwork.ui.CheckConnectionDialog;
import bluej.groupwork.ui.TeamSettingsPanel;

/**
 * Test the username, password, host, etc. settings to make sure they are valid
 * 
 * @author fisker
 */
public class ValidateConnectionAction extends AbstractAction
{
    private TeamSettingsPanel teamSettingsPanel;
    private Dialog owner;
    
    public ValidateConnectionAction(String name, TeamSettingsPanel teamSettingsPanel,
            Dialog owner)
    {
        super(name);
        this.teamSettingsPanel = teamSettingsPanel;
        this.owner = owner;
    }
    
    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e)
    {
        TeamworkProvider provider = teamSettingsPanel.getSelectedProvider();
        TeamSettings settings = teamSettingsPanel.getSettings();
        
        new CheckConnectionDialog(owner, provider, settings).setVisible(true);
    }
}
