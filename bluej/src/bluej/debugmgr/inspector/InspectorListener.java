package bluej.debugmgr.inspector;

import java.util.*;

/**
 *  The listener for InspectorEvent events.
 *
 *@author     Duane Buck
 *@created    December 26, 2000
 *@version    $Id: InspectorListener.java 2032 2003-06-12 05:04:28Z ajp $
 */
public interface InspectorListener extends EventListener
{

    void inspectEvent(InspectorEvent e);

    void getEvent(InspectorEvent e);

}
