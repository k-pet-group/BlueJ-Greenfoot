package bluej.browser;

import java.awt.*;
import java.awt.event.*;

import java.util.*;

/**
 * The listener for LibraryChooser events.
 * 
 * @author Andrew Patterson
 * @version $Id: LibraryChooserListener.java 279 1999-11-16 07:06:32Z ajp $
 */
public interface LibraryChooserListener extends EventListener {

    void nodeEvent(LibraryChooserEvent e);

}
