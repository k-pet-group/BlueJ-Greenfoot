package bluej.groupwork.ui;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.groupwork.Repository;
import bluej.groupwork.BasicServerResponse;
import bluej.groupwork.UpdateListener;
import bluej.groupwork.UpdateResult;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;

/**
 * The group work related controls are presented in the TeamControls frame
 * @author fisker
 *
 */
public class TeamControls extends JFrame {
	
	private JButton commitButton;
	private JButton updateButton;
	private JCheckBox includeGraphLayoutCheckBox;
	//private PkgMgrFrame pmf;
	private Project project;
	private static final String title = Config.getString("team.teamcontrols.title");
	private static final Image iconImage =
        Config.getImageAsIcon("image.icon").getImage();
	private ProgressDialog progress;
	
	public TeamControls(Project project){
		super(title);
		this.project = project;
		makeWindow();
	}
	
	private void makeWindow()
	{
		setIconImage(iconImage);

		// Close Action when close button is pressed
		addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent E)
            {
                setVisible(false);
            }
        });
		// Save position when window is moved
        addComponentListener(new ComponentAdapter() {
                public void componentMoved(ComponentEvent event)
                {
                    Config.putLocation("bluej.teamcontrols", getLocation());
                }
            });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        
        setLocation(Config.getLocation("bluej.teamcontrols"));

        JPanel mainPanel = new JPanel();
        {
        	mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        	mainPanel.setBorder(BlueJTheme.dialogBorder);
        	mainPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder("Code Synchronization"),
                    BlueJTheme.generalBorder));
        	mainPanel.setAlignmentX(LEFT_ALIGNMENT);
        	
        	// Commit button
        	commitButton = new JButton("Commit to Repository");
        	commitButton.addActionListener(new ActionListener() {
        		public void actionPerformed(ActionEvent evt) { 
        			doCommit();
        		}		
        	});
        
        	// Update button
        	updateButton = new JButton("Update from Repository");
        	updateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) { 
				doUpdate(); 
				}		
        	});
        	
        	// IncludeGraphLayoutCheckbox
        	includeGraphLayoutCheckBox = new JCheckBox("Include Graphlayout");
        	
        	//allow the Add and Delete buttons to be resized to equal width
    		commitButton.setMaximumSize(new Dimension(Integer.MAX_VALUE,
    						commitButton.getPreferredSize().height));
    		updateButton.setMaximumSize(new Dimension(Integer.MAX_VALUE,
    						updateButton.getPreferredSize().height));
        	mainPanel.add(commitButton);
        	mainPanel.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
        	mainPanel.add(updateButton);
        	mainPanel.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
        	mainPanel.add(includeGraphLayoutCheckBox);
        }
        progress = new ProgressDialog(this); 
        getContentPane().add(mainPanel);
        pack();
	}
	 
	/**
     * Show or hide the ExecControl window.
     */
    public void showHide(boolean show)
    {
        setVisible(show);
    }
	
	private void doUpdate() {
		Thread thread = new Thread(){
			public void run(){
				UpdateListener updateListener = null;
				try {
					updateListener = project.getRepository().updateAll(includeGraphLayoutCheckBox.isSelected());
				} catch (CommandAbortedException e) {
					e.printStackTrace();
				} catch (CommandException e) {
					e.printStackTrace();
				} catch (AuthenticationException e) {
					e.printStackTrace();
				}
				project.reloadFilesInEditors();
				progress.stop();
				handleConflicts(updateListener);
			}
			
			public void handleConflicts(UpdateListener updateListener){
				StringBuffer conflicts = new StringBuffer();
				if (updateListener.getConflicts().size() > 0){
					conflicts.append("The following classes had conflicts:\n");
					for (Iterator i = updateListener.getConflicts().iterator(); i.hasNext();) {
						UpdateResult updateResult = (UpdateResult) i.next();
						conflicts.append(updateResult.getFilename() + "\n");					
					}
					JOptionPane.showMessageDialog(TeamControls.this,conflicts);
				}
			}
		};
		thread.start();
		progress.start(); 
	}
	
	
	private void doCommit() {
		Thread thread = new Thread(){
			public void run(){
				BasicServerResponse basicServerResponse = null;
				try {
					basicServerResponse = project.getRepository().commitAll(includeGraphLayoutCheckBox.isSelected());
				} catch (CommandAbortedException e) {
					e.printStackTrace();
				} catch (CommandException e) {
					e.printStackTrace();
				} catch (AuthenticationException e) {
					e.printStackTrace();
				}
				progress.stop();
				if (basicServerResponse.isError()){
					JOptionPane.showMessageDialog(TeamControls.this, basicServerResponse.getMessage());
				}
			}
		};
		thread.start();
		progress.start();
	    //project.getRepository().shareProject(project);
	}
}
