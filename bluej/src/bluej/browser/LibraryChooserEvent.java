package bluej.browser;

import java.awt.*;
import java.awt.event.*;

import java.util.*;

import java.io.*;

/**
 * The event which occurs when a node is chosen in the LibraryChooser.
 * 
 * @author Andrew Patterson
 * @version $Id: LibraryChooserEvent.java 277 1999-11-16 00:57:17Z ajp $
 */
public class LibraryChooserEvent extends EventObject {

    public final static int NODE_CLICKED = 1;
    public final static int NODE_POPUP = 2;         // not used yet

    protected LibraryChooserNode node;
    protected int id;
    
    public LibraryChooserEvent(Object source, int id, LibraryChooserNode node)
    {
        super(source);

        this.id = id;
        this.node = node;                
    }

    public int getID()
    {
        return id;        
    }

    public LibraryChooserNode getNode()
    {
        return node;        
    }
}
