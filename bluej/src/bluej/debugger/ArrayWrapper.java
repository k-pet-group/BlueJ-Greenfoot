package bluej.debugger;

import bluej.Config;
import bluej.BlueJEvent;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.utility.Utility;
import bluej.utility.JavaNames;
import bluej.views.MethodView;
import bluej.views.View;
import bluej.views.ViewFilter;
import bluej.tester.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.lang.reflect.*;
import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A wrapper around a Java object that handles calling methods, inspecting, etc.
 *
 * The wrapper is represented by the red oval that is visible on the
 * object bench.
 *
 * @author  Michael Kolling
 * @version $Id: ArrayWrapper.java 1535 2002-11-29 13:37:46Z ajp $
 */
public class ArrayWrapper extends ObjectWrapper
{
    public static int WORD_GAP = 10;
    public static int SHADOW_SIZE = 3;

    ArrayWrapper(PkgMgrFrame pmf, ObjectBench ob, DebuggerObject obj, String instanceName)
    {
        super(pmf, ob, obj, instanceName);
    }

    /**
     * Creates the popup menu structure by parsing the object's
     * class inheritance hierarchy.
     *
     * @param className   class name of the object for which the menu is to be built
     */
    protected void createMenu(String className)
    {
        menu = new JPopupMenu(instanceName);

        JMenuItem item;
        menu.add(item = new JMenuItem("array len = "));
//        item.addActionListener(this);
        item.setFont(PrefMgr.getStandoutMenuFont());
        item.setForeground(envOpColour);

        add(menu);
    }

    /**
     * draw a UML style object (array) instance
     */
    protected void drawUMLStyle(Graphics2D g)
    {
        g.setFont(PrefMgr.getStandardFont());
        FontMetrics fm = g.getFontMetrics();

        drawUMLObjectShape(g,10,10,WIDTH-10,HEIGHT-10,3,5);
        drawUMLObjectShape(g,5,5,WIDTH-10,HEIGHT-10,3,5);
        drawUMLObjectShape(g,0,0,WIDTH-10,HEIGHT-10,3,5);

        drawUMLObjectText(g,0,0,WIDTH-10,HEIGHT-10,3,
                            instanceName + ":", displayClassName);
        
    }
}
