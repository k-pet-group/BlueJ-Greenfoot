package bluej.debugger;

import java.awt.*;
import java.awt.event.*;

import java.util.*;

/**
 * The listener for InspectorEvent events.
 * 
 * @author Duane Buck
 * @version $Id: InspectorListener.java 710 2000-11-22 06:33:42Z dbuck $
 */
public interface InspectorListener extends EventListener {

    void inspectEvent(InspectorEvent e);

    void getEvent(InspectorEvent e);

}
