package bluej.debugger;

import java.awt.*;
import java.awt.event.*;

import java.util.*;

/**
 *  The listener for InspectorEvent events.
 *
 *@author     Duane Buck
 *@created    December 26, 2000
 *@version    $Id: InspectorListener.java 739 2000-12-27 08:11:41Z dbuck $
 */
public interface InspectorListener extends EventListener
{

    void inspectEvent(InspectorEvent e);

    void getEvent(InspectorEvent e);

}
