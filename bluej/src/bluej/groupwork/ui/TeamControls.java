package bluej.groupwork.ui;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import bluej.BlueJTheme;
import bluej.Config;
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
	//private PkgMgrFrame pmf;
	private Project project;
	private static final String title = ""; // Config.
	private static final Image iconImage =
        Config.getImageAsIcon("image.icon").getImage();
	
	
	public TeamControls(Project project){
		//super(pmf, title);
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
        	commitButton = new JButton("Commit to Repository");
        	commitButton.addActionListener(new ActionListener() {
        		public void actionPerformed(ActionEvent evt) { 
        			doCommit();
        		}		
        	});
        
        	updateButton = new JButton("Update from Repository");
        	updateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) { 
				doUpdate(); 
				}		
        	});
        	mainPanel.add(commitButton);
        	mainPanel.add(updateButton);
        }
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
		System.out.println("TeamDialog: Update");
		project.getRepository().updateAll(project); 
	}
	
	
	private void doCommit() {
		System.out.println("TeamDialog: Commit");
		project.getRepository().commitAll(project);
	    //project.getRepository().shareProject(project); 
	}
}
