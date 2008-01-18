package bluej.groupwork.ui;

import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.groupwork.TeamSettings;
import bluej.groupwork.TeamworkProvider;
import bluej.utility.DBox;
import bluej.utility.EscapeDialog;

/**
 * A dialog which displays an activity indicator while connection settings are
 * being verified
 * 
 * @author Davin McCall
 */
public class CheckConnectionDialog extends EscapeDialog
{
    private ActivityIndicator activityIndicator;
    private JLabel connLabel;
    private JButton closeButton;
    
    private TeamSettings settings;
    private TeamworkProvider provider;
    
    public CheckConnectionDialog(Dialog owner, TeamworkProvider provider,
            TeamSettings settings)
    {
        super(owner, true);
        setTitle(Config.getString("team.settings.checkConnection"));
        
        this.provider = provider;
        this.settings = settings;
        
        buildUI();
        setLocationRelativeTo(owner);
    }
    
    private void buildUI()
    {
        DBox contentPane = new DBox(DBox.Y_AXIS, 0, BlueJTheme.componentSpacingLarge, 0.0f);
        contentPane.setBorder(BlueJTheme.dialogBorder);
        setContentPane(contentPane);
        
        connLabel = new JLabel(Config.getString("team.checkconn.checking"));
        contentPane.add(connLabel);
        
        activityIndicator = new ActivityIndicator();
        activityIndicator.setRunning(true);
        contentPane.add(activityIndicator);
        
        closeButton = BlueJTheme.getCancelButton();
        closeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    setVisible(false);
                }
            });
        contentPane.add(closeButton);
        
        pack();
    }
    
    public void setVisible(boolean vis)
    {
        // Must start the thread before calling super.setVisible(), because
        // we are modal - super.setVisible() will block.
        if (vis) {
            new Thread() {
                public void run()
                {
                    final boolean res = validateConnection();
                    EventQueue.invokeLater(new Runnable() {
                        public void run()
                        {
                            if (res) {
                                connLabel.setText(Config.getString("team.checkconn.ok"));
                            }
                            else {
                                connLabel.setText(Config.getString("team.checkconn.bad"));
                            }
                            
                            activityIndicator.setRunning(false);
                            closeButton.setText(BlueJTheme.getCloseLabel());
                            pack();
                        }
                    });
                }
            }.start();
        }
        super.setVisible(vis);
    }
    
    private boolean validateConnection()
    {
        return provider.checkConnection(settings);
    }   
}
