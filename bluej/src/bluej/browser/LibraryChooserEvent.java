package bluej.browser;

import java.awt.*;
import java.awt.event.*;

import java.util.*;

import java.io.*;

/**
 * The event which occurs when a node is chosen in the LibraryChooser.
 * 
 * @author Andrew Patterson
 * @version $Id: LibraryChooserEvent.java 282 1999-11-18 10:36:00Z ajp $
 */
public class LibraryChooserEvent extends EventObject {

    public final static int FINISHED_LOADING = 1;   // a hack event to indicate that
                                                    // the library chooser has finished
                                                    // loading. 'node' should be null.
    public final static int NODE_CLICKED = 2;
    public final static int NODE_POPUP = 3;         // not used yet

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
