package bluej.pkgmgr;

import bluej.utility.Utility;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Properties;

/** 
 ** @version $Id: StdClassRole.java 140 1999-06-22 06:44:42Z mik $
 ** @author Bruce Quig
 **
 ** A role object which a class target uses to delegate behaviour to.
 ** StdClassRole is used to represent standard Java classes.
 **/
public class StdClassRole extends ClassRole 
{


    public void save(Properties props, int modifiers, String prefix)
    {
	super.save(props, modifiers, prefix);
	props.put(prefix + ".type", "ClassTarget");
    }

    /**
     * generates a source code skeleton for this class	
     *
     * @param template the name of the particular class template
     * @param pkg the package that the class target resides in
     * @param name the name of the class
     * @param sourceFile the name of the source file to be generated
     */	
    public void generateSkeleton(Package pkg, String name, String sourceFile, 
				 boolean isAbstract, boolean isInterface)
    {
	String template = isAbstract ? "template.abstract" :
	    isInterface ? "template.interface" :
	    "template.stdclass";

	generateSkeleton(template, pkg, name, sourceFile);
    }  


   /**
     * Generate a popup menu for this AppletClassRole.
     * @param cl the class object that is represented by this target
     * @param editorFrame the frame in which this targets package is displayed
     * @return the generated JPopupMenu
     */
    protected void createMenu(JPopupMenu menu, ClassTarget ct, int state) {
	// no implementation at present
    }


    // overloads method in Target super class
    public void draw(Graphics g, ClassTarget ct, int x, int y, int width, int height)
    {
	// no implementation as yet
    }

	
    // -- modified ActionListener interface --

    public void actionPerformed(ActionEvent e, ClassTarget ct) {
	// no implementation
    }
    
}
