package bluej.pkgmgr;

import bluej.Config;
import bluej.utility.MultiLineLabel;
import bluej.utility.Utility;
import bluej.utility.DialogManager;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * The BlueJ about dialog
 *
 * @author  Justin Tan
 * @version $Id: AboutBlue.java 549 2000-06-14 03:26:05Z mik $
 */
class AboutBlue extends JDialog
{
    public AboutBlue(JFrame thisFrame, String version)
    {
        super(thisFrame, "BlueJ", true);

        // Create About box text
        JPanel aboutPanel = new JPanel();
        aboutPanel.setBorder(Config.dialogBorder);
        aboutPanel.setLayout(new BorderLayout(2,2));
        aboutPanel.setBackground(Color.white);

        // Create Text Panel
        MultiLineLabel text = new MultiLineLabel(LEFT_ALIGNMENT);
        text.setBackground(Color.white);
        text.addText("BlueJ version " + version + 
                     "  (Java version " + System.getProperty("java.version") +
                     ")", true, false);
        text.addText(" ");
        text.addText("Virtual machine: " +
                     System.getProperty("java.vm.name") + " " +
                     System.getProperty("java.vm.version") +
                     " (" + System.getProperty("java.vm.vendor") + ")");
        text.addText("Running on: " + System.getProperty("os.name") +
                     " " + System.getProperty("os.version") +
                     " (" + System.getProperty("os.arch") + ")");
 
        text.addText(" ");
        text.addText("The BlueJ development environment was\n" +
            		 "developed at Monash University, Australia.");
        text.addText(" ");
        text.addText("The BlueJ team:", false, true);
        text.addText("      Michael K\u00F6lling\n" +
                     "      Markus Ostman\n" +
                     "      Andrew Patterson\n" +
                     "      Bruce Quig\n" +
                     "      John Rosenberg\n" +
                     "      Axel Schmolitzky");
        text.addText(" ");
        text.addText("For more information contact Michael K\u00F6lling\n" +
                     "(mik@monash.edu.au).");

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
        DialogManager.centreDialog(this);
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

