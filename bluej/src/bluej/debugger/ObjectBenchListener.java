package bluej.debugger;

import java.util.*;

/**
 * The listener for ObjectBench events.
 *
 * @author  Andrew Patterson
 * @version $Id: ObjectBenchListener.java 1818 2003-04-10 13:31:55Z fisker $
 */
public interface ObjectBenchListener extends EventListener
{
    void objectEvent(ObjectBenchEvent e);
}
