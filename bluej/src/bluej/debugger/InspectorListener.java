package bluej.debugger;

import java.util.*;

/**
 *  The listener for InspectorEvent events.
 *
 *@author     Duane Buck
 *@created    December 26, 2000
 *@version    $Id: InspectorListener.java 1818 2003-04-10 13:31:55Z fisker $
 */
public interface InspectorListener extends EventListener
{

    void inspectEvent(InspectorEvent e);

    void getEvent(InspectorEvent e);

}
