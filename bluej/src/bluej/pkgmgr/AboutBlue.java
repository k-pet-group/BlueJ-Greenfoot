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
 * @version $Id: AboutBlue.java 1923 2003-04-30 06:11:12Z ajp $
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

        aboutPanel.add(toptext, BorderLayout.NORTH);

        // Create Text Panel
        MultiLineLabel text = new MultiLineLabel(LEFT_ALIGNMENT);
        text.setBackground(Color.white);
        text.addText(" ");
        text.addText("The BlueJ development environment was\n" +
            		 "developed at Monash University, Australia,\n" +
                     "and the University of Southern Denmark.");
        text.addText(" ");
        text.addText("The BlueJ team:", false, true);
        text.addText("      Michael K\u00F6lling\n" +
                     "      Andrew Patterson\n" +
                     "      Bruce Quig\n" +
                     "      John Rosenberg");
        text.addText(" ");
        text.addText("The extension mechanism was written by\n" +
                     "Ian Utting, Clive Miller and Damiano Bolla\n" +
                     "at the University of Kent at Canterbury.");

        aboutPanel.add(text, BorderLayout.CENTER);

        // footer text
        MultiLineLabel bottomtext = new MultiLineLabel(LEFT_ALIGNMENT);
        bottomtext.setBackground(Color.white);
        bottomtext.addText(" ");
        bottomtext.addText("For more information contact Michael " +
                           "K\u00F6lling (mik@mip.sdu.dk).");

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

