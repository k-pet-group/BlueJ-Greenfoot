package bluej.browser;

import java.awt.*;
import java.awt.event.*;

import java.util.*;

import java.io.*;

/**
 * The listener for LibraryChooser events.
 * 
 * @author Andrew Patterson
 * @version $Id: LibraryChooserListener.java 277 1999-11-16 00:57:17Z ajp $
 */
public abstract interface LibraryChooserListener extends EventListener {

    void nodeEvent(LibraryChooserEvent e);

}
