package bluej.groupwork.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import bluej.groupwork.cvsnb.CvsRepository;
import bluej.groupwork.cvsnb.ProtocolMapper;
import bluej.groupwork.ui.TeamSettingsPanel;
import bluej.utility.Debug;

/**
 * @author fisker
 *

 */
public class ValidateCvsConnectionAction extends AbstractAction
{

    private TeamSettingsPanel teamSettingsPanel;
    /**
     * 
     */
    public ValidateCvsConnectionAction(String name, TeamSettingsPanel teamSettingsPanel)
    {
        super(name);
        this.teamSettingsPanel = teamSettingsPanel;
        
    }
    
    /**
     * Extra information from teamSettingsPanel and build a cvsroot string
     * @return
     */
    private String makeCvsRoot(){
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
        try {
            String result = (CvsRepository.validateConnection(cvsRoot) ? "Connection is ok" : "Could not connect to server");
            JOptionPane.showMessageDialog(teamSettingsPanel,result);
        } catch (Exception ee) {
            // experimental BQ
            Debug.reportError("SSH exception");
            
        }
        

    }

}
