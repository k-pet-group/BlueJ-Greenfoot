package bluej.debugger;

import java.awt.*;
import java.awt.event.*;

import java.util.*;

/**
 * The listener for ObjectBench events.
 *
 * @author  Andrew Patterson
 * @version $Id: ObjectBenchListener.java 505 2000-05-24 05:44:24Z ajp $
 */
public interface ObjectBenchListener extends EventListener
{
    void objectEvent(ObjectBenchEvent e);
}
