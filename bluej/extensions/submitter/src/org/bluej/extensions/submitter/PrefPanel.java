package org.bluej.extensions.submitter;

import bluej.extensions.*;

import javax.swing.*;
import java.awt.*;

public class PrefPanel implements PreferenceGenerator
    {
    private Stat stat;
    private JPanel myPanel;
    private JTextField smtphost, useraddr, username;


    public PrefPanel(Stat i_stat)
        {
        stat = i_stat;
        
        // This is a complex layout manager, if you want it :-)
        myPanel = new JPanel(new GridBagLayout());
    
        GridBagConstraints tag = new GridBagConstraints();
        tag.anchor = GridBagConstraints.NORTHEAST;
        tag.gridx = 0;

        GridBagConstraints value = new GridBagConstraints();
        value.anchor = GridBagConstraints.NORTHWEST;
        value.gridx  = 1;
        value.gridwidth = 3;

        myPanel.add (new JLabel (stat.bluej.getLabel ("preferences.label."+GlobalProp.SMTPHOST_VAR)),tag);
        myPanel.add (smtphost = new JTextField(30), value);

        myPanel.add (new JLabel (stat.bluej.getLabel ("preferences.label."+GlobalProp.USERADDR_VAR)),tag);
        myPanel.add (useraddr = new JTextField(30), value);

        myPanel.add (new JLabel (stat.bluej.getLabel ("preferences.label."+GlobalProp.USERNAME_VAR)),tag);
        myPanel.add (username = new JTextField(30), value);
        
        loadValues(); // So the user sees the previous values
        }

    /**
     * Called by core BlueJ to get the panel to display.
     */
    public JPanel getPanel()
        {
        return myPanel;
        }


    /**
     * Tiny Utility to make code simpler
     */
    private void saveOneValue ( String propKey, String propVal )
        {
        stat.bluej.setExtensionPropertyString(propKey,propVal);
        stat.globalProp.setProperty(propKey,propVal);
        }

    /**
     * Called by the core BlueJ when someone wants to save the values...
     */    
    public void saveValues()
        {
        saveOneValue (GlobalProp.SMTPHOST_VAR,smtphost.getText());
        saveOneValue (GlobalProp.USERADDR_VAR,useraddr.getText());
        saveOneValue (GlobalProp.USERNAME_VAR,username.getText());
        }

    /**
     * This is called when we want to load the values into the panel.
     * It may happen at the beginning or anytime, really.
     */
    public void loadValues()
        {

        String propVal = stat.bluej.getExtensionPropertyString(GlobalProp.SMTPHOST_VAR,"");
        smtphost.setText(propVal);
        stat.globalProp.setProperty(GlobalProp.SMTPHOST_VAR,propVal);
        
        propVal = stat.bluej.getExtensionPropertyString(GlobalProp.USERADDR_VAR,System.getProperty("user.name","")+"@");
        useraddr.setText(propVal);
        stat.globalProp.setProperty(GlobalProp.USERADDR_VAR,propVal);

        propVal = stat.bluej.getExtensionPropertyString(GlobalProp.USERNAME_VAR,System.getProperty("user.name",""));
        username.setText(propVal);
        stat.globalProp.setProperty(GlobalProp.USERNAME_VAR,propVal);
        }
    }
