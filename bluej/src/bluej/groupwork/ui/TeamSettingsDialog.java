package bluej.groupwork.ui;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.groupwork.TeamSettingsController;
import bluej.groupwork.cvsnb.ProtocolMapper;
import bluej.utility.EscapeDialog;

/**
 * A dialog for teamwork settings.
 *
 * @author fisker
 * @author bquig
 * @version $Id: TeamSettingsDialog.java 4926 2007-04-13 02:28:18Z davmac $
 */
public class TeamSettingsDialog extends EscapeDialog
{
    private String title = Config.getString(
            "team.settings.title");
    public static final int OK = 0;
    public static final int CANCEL = 1;
    private TeamSettingsController teamSettingsController;
    private TeamSettingsPanel teamSettingsPanel;
    private int event;
    
    private JButton okButton;

    /**
     * Create a team settings dialog with a reference to the team settings
     * controller that it manipulates.
     */
    public TeamSettingsDialog(TeamSettingsController controller)
    {
        teamSettingsController = controller;
        event = CANCEL;
        if(teamSettingsController.hasProject())
            title += " - " + teamSettingsController.getProject().getProjectName();
        setTitle(title);
       
        setModal(true);

        JPanel mainPanel = new JPanel();
        mainPanel.setBorder(BlueJTheme.dialogBorder);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        JPanel buttonPanel = makeButtonPanel();
        teamSettingsPanel = new TeamSettingsPanel(teamSettingsController, this);
        setFocusTraversalPolicy(teamSettingsPanel.getTraversalPolicy(
                getFocusTraversalPolicy()));
        mainPanel.add(teamSettingsPanel);
        mainPanel.add(buttonPanel);
        setContentPane(mainPanel);
        pack();
    }

    /**
     * Set up the panel containing the ok and cancel buttons, with associated
     * actions.
     */
    private JPanel makeButtonPanel()
    {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        {
            buttonPanel.setAlignmentX(LEFT_ALIGNMENT);

            okButton = BlueJTheme.getOkButton();
            okButton.addActionListener(new ActionListener() {
                    /**
                     * Write the data from the teamSettingsPanel to the project's team.defs file
                     * If checkbox in teamSettingsPanel is checked, the data is also stored in
                     * bluej.properties
                     */
                    public void actionPerformed(ActionEvent e)
                    {
                        String userKey = "bluej.teamsettings.user";
                        String userValue = teamSettingsPanel.getUser();
                        teamSettingsController.setPropString(userKey, userValue);

                        // passwords are handled differently for security reasons,
                        // we don't at present store them on disk
                        String passValue = teamSettingsPanel.getPassword();
                        teamSettingsController.setPasswordString(passValue);

                        String serverKey = "bluej.teamsettings.cvs.server";
                        String serverValue = teamSettingsPanel.getServer();
                        teamSettingsController.setPropString(serverKey,
                            serverValue);

                        String prefixKey = "bluej.teamsettings.cvs.repositoryPrefix";
                        String prefixValue = teamSettingsPanel.getPrefix();
                        teamSettingsController.setPropString(prefixKey,
                            prefixValue);

                        String groupKey = "bluej.teamsettings.groupname";
                        String groupValue = teamSettingsPanel.getGroup();
                        teamSettingsController.setPropString(groupKey,
                            groupValue);

                        String protocolKey = "bluej.teamsettings.cvs.protocol";
                        String protocolValue = ProtocolMapper.getProtocol(teamSettingsPanel.getProtocol());
                                                
                        teamSettingsController.setPropString(protocolKey,
                            protocolValue);

                        String useAsDefaultKey = "bluej.teamsettings.useAsDefault";
                        Config.putPropString(useAsDefaultKey,
                            Boolean.toString(
                                teamSettingsPanel.getUseAsDefault()));

                        if (teamSettingsPanel.getUseAsDefault()) {
                            Config.putPropString(userKey, userValue);
                            Config.putPropString(serverKey, serverValue);
                            Config.putPropString(prefixKey, prefixValue);
                            Config.putPropString(groupKey, groupValue);
                            Config.putPropString(protocolKey, protocolValue);
                        }

                        if ((teamSettingsController != null) &&
                                teamSettingsController.hasProject()) {
                            teamSettingsController.writeToProject();
                        }

                        //if (getProject() != null && getProject().getTeamControlsFrame() != null){
                        //    getProject().getTeamControlsFrame().configureHelp();
                        //}
                        event = OK;
                        setVisible(false);
                    }
                });

            getRootPane().setDefaultButton(okButton);

            JButton cancelButton = BlueJTheme.getCancelButton();
            cancelButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        event = CANCEL;
                        setVisible(false);
                    }
                });

            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);
        }

        return buttonPanel;
    }

    /**
     * Disable the fields used to specify the repository:
     * group, prefix, server and protocol
     */
    public void disableRepositorySettings()
    {
        teamSettingsPanel.disableRepositorySettings();
    }

    /**
     * Display the dialog and wait for a response. Returns the user
     * response as OK or CANCEL.
     */
    public int doTeamSettings()
    {
        setVisible(true);

        return event;
    }
    
    /**
     * Enabled or disable to "Ok" button of the dialog.
     */
    public void setOkButtonEnabled(boolean enabled)
    {
        okButton.setEnabled(enabled);
    }
}
