package bluej.groupwork.ui;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import bluej.BlueJTheme;
import bluej.Config;

/**
 * @author fisker
 *
  */
public class ProgressDialog extends JDialog {
	
	private JProgressBar progressbar;
	private static final String title = Config.getString("team.teamcontrols.pleasewait");
	
	public ProgressDialog(JFrame frame){
		super(frame, title);
		 JPanel mainPanel = new JPanel();
	        {
	        	mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
	        	mainPanel.setBorder(BlueJTheme.dialogBorder);
	        	mainPanel.setBorder(BorderFactory.createCompoundBorder(
	                    BorderFactory.createTitledBorder("CVS Operation"),
	                    BlueJTheme.generalBorder));
	        	mainPanel.setAlignmentX(LEFT_ALIGNMENT);
	        	
	        	//progressbar
	        	progressbar = new JProgressBar(0, 100);
	    		progressbar.setIndeterminate(true);
	        	mainPanel.add(progressbar);

	        }
		setLocation(300, 300);
		getContentPane().add(mainPanel);
		setModal(true);
        pack();
	}
	
	public void start(){
		setVisible(true);
	}
	
	public void stop(){
		setVisible(false);
	}
}
