package bluej.pkgmgr;

import bluej.Config;
import bluej.utility.MultiLineLabel;
import bluej.utility.Utility;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 ** @version $Id: AboutBlue.java 86 1999-05-18 02:49:53Z mik $
 ** @author Justin Tan
 **
 ** General graph edge
 **/
public class AboutBlue extends JDialog
{
    public AboutBlue(JFrame thisFrame, String version)
    {
	super(thisFrame, "BlueJ", true);

	// Create About box text
	JPanel aboutPanel = new JPanel();
	aboutPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
	aboutPanel.setLayout(new BorderLayout(2,2));
	aboutPanel.setBackground(Color.white);

	// Create Text Panel
	String text = "BlueJ version " + version;
	JPanel textPanel = new MultiLineLabel(text + "\n" + Config.getString("main.about"), JLabel.LEFT);
	textPanel.setBackground(Color.white);
	aboutPanel.add(textPanel, BorderLayout.CENTER);

	// Create Button Panel
	JPanel buttonPanel = new JPanel();
	buttonPanel.setBackground(Color.white);
	buttonPanel.setLayout(new FlowLayout());
	JButton ok = new JButton(Config.getString("okay"));
	buttonPanel.add(ok);
	aboutPanel.add(buttonPanel,BorderLayout.SOUTH);

	ImageIcon icon = new ImageIcon(Config.getImageFilename("image.logo"));
	JLabel logoLabel = new JLabel(icon);
	aboutPanel.add(logoLabel, BorderLayout.WEST);
	getContentPane().add(aboutPanel);
		
	// Set some attributes for this DialogBox
	Utility.centreDialog(this);
	setResizable(false);
		
	// Close Action when OK is pressed
	ok.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent event)
		{
		    AboutBlue.this.setVisible(false);
		    AboutBlue.this.dispose();
		}
	});

	// Close Action when close button is pressed
	addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent event)
		{
		    Window win = (Window)event.getSource();
		    win.setVisible(false);
		    win.dispose();
		}
	});
    }
}

