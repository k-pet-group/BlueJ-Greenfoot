package bluej.pkgmgr;

import bluej.Config;
import bluej.utility.MultiLineLabel;
import bluej.utility.Utility;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 ** @version $Id: AboutBlue.java 138 1999-06-22 01:05:50Z mik $
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
  	MultiLineLabel text = new MultiLineLabel(JLabel.LEFT);
	text.setBackground(Color.white);
	    text.addText("BlueJ version " + version, true, false);
	    text.addText("\nThe BlueJ development environment was\n" +
			 "developed at the School of Computer Science\n" +
			 "and Software Engineering, Monash University.");
	    text.addText("\nThe BlueJ team:", false, true);
	    text.addText("      John Rosenberg\n" + 
			 "      Michael K\u00F6lling\n" +
			 "      Bruce Quig\n" +
			 "      Andrew Patterson");
	    text.addText("\nwith help from:", false, true);
	    text.addText("      Michael Cahill\n" +
			 "      Andrew Marks");
	    text.addText("\nFor more information contact Michael K\u00F6lling\n" +
			 "(mik@csse.monash.edu.au).");

  	aboutPanel.add(text, BorderLayout.CENTER);

	// Create Button Panel
	JPanel buttonPanel = new JPanel();
	buttonPanel.setBackground(Color.white);
	buttonPanel.setLayout(new FlowLayout());
	JButton ok = new JButton(Config.getString("okay"));
	buttonPanel.add(ok);
	aboutPanel.add(buttonPanel,BorderLayout.SOUTH);

	// insert logo
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

