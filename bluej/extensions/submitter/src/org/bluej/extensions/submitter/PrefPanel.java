package org.bluej.extensions.submitter;

import bluej.extensions.*;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class PrefPanel implements PrefGen 
{
    private JPanel myPanel;
    private JTextField profileCmd;
    private BlueJ bluej;
    public  TreeMap globalSettings;  // is a name, JText

    private static final String[] globalProps = {"smtphost", "useraddr", "username"};
    private static final String[] globalDefaults = {"", System.getProperty("user.name","")+"@", System.getProperty("user.name","")};

    public PrefPanel(BlueJ bluej)
    {
        this.bluej = bluej;

        // This is a complex layout manager, if you want it :-)
        myPanel = new JPanel(new GridBagLayout());
    
        GridBagConstraints tag = new GridBagConstraints();
        tag.anchor = GridBagConstraints.NORTHEAST;
        tag.gridx = 0;

        GridBagConstraints value = new GridBagConstraints();
        value.anchor = GridBagConstraints.NORTHWEST;
        value.gridx  = 1;
        value.gridwidth = 3;

        globalSettings = new TreeMap();
        for (int i=0,n=globalProps.length; i<n; i++) {
            String prop = globalProps[i];
            // NOTE: the way I get the values is DIFFERENT than the stored label !
            myPanel.add (new JLabel (bluej.getLabel ("preferences.label."+prop)),tag);
            JTextField aTextField = new JTextField(30);
            myPanel.add (aTextField,value);

            // Store this in the properies map
            globalSettings.put(prop,aTextField);
            }

        // So the user sees the previos values
        loadValues();
        }

    public JPanel getPanel()
    {
        return myPanel;
    }
    
    public void saveValues()
    {
        for (int i=0,n=globalProps.length; i<n; i++) {
            String prop = globalProps[i];
            JTextField aText = (JTextField)globalSettings.get(prop);
            bluej.setExtPropString("preferences.label."+prop,aText.getText());
            }
    }

    public void loadValues()
    {
        for (int i=0,n=globalProps.length; i<n; i++) {
            String prop = globalProps[i];
            JTextField aText = (JTextField)globalSettings.get(prop);
            aText.setText(bluej.getExtPropString("preferences.label."+prop,globalDefaults[i]));
            }
    }

    public Properties getGlobalProps()
    {
        Properties props = new Properties();

        for (int i=0,n=globalProps.length; i<n; i++) {
            String prop = globalProps[i];      
            props.setProperty (prop, bluej.getExtPropString("preferences.label."+prop,globalDefaults[i]));
            }

        return props;
    } 
  }