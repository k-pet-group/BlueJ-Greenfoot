package bluej.groupwork.actions;

import java.awt.Dialog;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import bluej.groupwork.cvsnb.ProtocolMapper;
import bluej.groupwork.ui.CheckConnectionDialog;
import bluej.groupwork.ui.TeamSettingsPanel;

/**
 * Test the username, password, host, etc. settings to make sure they are valid
 * 
 * @author fisker
 */
public class ValidateCvsConnectionAction extends AbstractAction
{
    private TeamSettingsPanel teamSettingsPanel;
    private Dialog owner;
    
    public ValidateCvsConnectionAction(String name, TeamSettingsPanel teamSettingsPanel,
            Dialog owner)
    {
        super(name);
        this.teamSettingsPanel = teamSettingsPanel;
        this.owner = owner;
    }
    
    /**
     * Extra information from teamSettingsPanel and build a cvsroot string
     * @return
     */
    private String makeCvsRoot()
    {
        String protocol = ProtocolMapper.getProtocol(teamSettingsPanel.getProtocol());
		String user = teamSettingsPanel.getUser();
		String password = teamSettingsPanel.getPassword();
		String server = teamSettingsPanel.getServer();
		String repositoryPrefix = teamSettingsPanel.getPrefix();
		String groupname = teamSettingsPanel.getGroup();
		String cvsRoot = ":" + protocol+":"+user+":"+password+"@"+server+":"+repositoryPrefix+groupname;
		return cvsRoot; 
    }
    
    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e)
    {
        String cvsRoot = makeCvsRoot();
        new CheckConnectionDialog(owner, cvsRoot).setVisible(true);
    }
}
