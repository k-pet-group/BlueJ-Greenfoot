package bluej.groupwork;

import bluej.Config;
import bluej.utility.Debug;

import java.awt.Container;
import java.awt.Cursor;
import java.util.*;
import java.io.*;


import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.ice.jcvsii.*;


public class GroupWorkPanel extends JPanel
{
    protected MainGrpPanel parent;
    protected Properties props;

    public GroupWorkPanel( MainGrpPanel parent )
    {
	super();
	this.parent = parent;
	this.setBorder( new EmptyBorder( 4, 4, 4, 4 ) );
	this.loadPrefs();
    }

    public MainGrpPanel getMainGrpPanel()
    {
	return this.parent;
    }

    public GroupWorkDialog getGroupWorkDialog()
    {
	return this.parent.getGroupWorkDialog();
    }

    public void savePreferences()
    {
    }
    
    public void loadPrefs()
    {
	String propsFile = Config.sys_confdir+File.separatorChar+"group.defs";
	Debug.message("GrpWrkPnl,45 "+propsFile);

        if (props == null) {
            // try to load the Properties for the group stuff
            try {
                FileInputStream input = new FileInputStream(propsFile);
		
                this.props = new Properties();
                this.props.load(input);
            } catch(IOException e) {
                Debug.reportError("Error loading group properties file" + 
                                  propsFile + ": " + e);
            }
	}
    }
}













