package bluej.debugmgr.objectbench;

import java.util.*;

/**
 * The listener for ObjectBench events.
 *
 * @author  Andrew Patterson
 * @version $Id: ObjectBenchListener.java 2032 2003-06-12 05:04:28Z ajp $
 */
public interface ObjectBenchListener extends EventListener
{
    void objectEvent(ObjectBenchEvent e);
}
