package bluej.pkgmgr;

import bluej.*;
import bluej.Config;
import bluej.utility.MultiLineLabel;
import bluej.utility.DialogManager;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * The BlueJ about box.
 *
 * @author  Michael Kolling
 * @version $Id: AboutBlue.java 2276 2003-11-05 15:02:44Z mik $
 */
class AboutBlue extends JDialog
{
    public AboutBlue(JFrame parent, String version)
    {
        super(parent, "About BlueJ", true);

        // Create About box text
        JPanel aboutPanel = new JPanel();
        aboutPanel.setBorder(BlueJTheme.dialogBorder);
        aboutPanel.setLayout(new BorderLayout(2,2));
        aboutPanel.setBackground(Color.white);

        // Create Text Panel
        MultiLineLabel toptext = new MultiLineLabel(LEFT_ALIGNMENT);
        toptext.setBackground(Color.white);
        toptext.addText("BlueJ version " + version +
                     "  (Java version " + System.getProperty("java.version") +
                     ")", true, false);
        toptext.addText(" ");
        toptext.addText("Virtual machine: " +
                     System.getProperty("java.vm.name") + " " +
                     System.getProperty("java.vm.version") +
                     " (" + System.getProperty("java.vm.vendor") + ")");
        toptext.addText("Running on: " + System.getProperty("os.name") +
                     " " + System.getProperty("os.version") +
                     " (" + System.getProperty("os.arch") + ")");
        toptext.addText("Java Home: " + System.getProperty("java.home"));

        toptext.addText(" ");
        toptext.addText("Debug log file: " + Config.getUserConfigFile(Config.debugLogName));
        
        aboutPanel.add(toptext, BorderLayout.NORTH);

        // Create Text Panel
        MultiLineLabel text = new MultiLineLabel(LEFT_ALIGNMENT);
        text.setBackground(Color.white);
        text.addText(" ");
        text.addText("The BlueJ team at Deakin University", false, true);
        text.addText("      Andrew Patterson, Bruce Quig,\n" +
                     "      John Rosenberg\n");
        text.addText(" ");
        text.addText("The BlueJ team at the University", false, true);
        text.addText("of Southern Denmark", false, true);
        text.addText("      Kasper Fisker, Poul Henriksen,\n" +
                     "      Michael K\u00F6lling\n");
        text.addText(" ");
        text.addText("The BlueJ extension team at Kent University", false, true);
        text.addText("      Damiano Bolla, Ian Utting");

        aboutPanel.add(text, BorderLayout.CENTER);

        // footer text
        MultiLineLabel bottomtext = new MultiLineLabel(LEFT_ALIGNMENT);
        bottomtext.setBackground(Color.white);
        bottomtext.addText(" ");
        bottomtext.addText("More information is at www.bluej.org");

        aboutPanel.add(bottomtext, BorderLayout.SOUTH);

        // insert logo
        Icon icon = Config.getImageAsIcon("image.logo");
        JLabel logoLabel = new JLabel(icon);
        aboutPanel.add(logoLabel, BorderLayout.WEST);

        // Create Button Panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.white);
        buttonPanel.setLayout(new FlowLayout());
        JButton ok = BlueJTheme.getOkButton();
        buttonPanel.add(ok);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(aboutPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        // Close Action when OK is pressed
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
        	{
        	    setVisible(false);
        	    dispose();
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

        DialogManager.centreDialog(this);
        setResizable(false);
        pack();
    }
}

