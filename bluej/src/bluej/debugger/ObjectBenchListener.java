package bluej.debugger;

import java.awt.*;
import java.awt.event.*;

import java.util.*;

/**
 * The listener for ObjectBench events.
 *
 * @author  Andrew Patterson
 * @version $Id: ObjectBenchListener.java 323 2000-01-02 13:08:19Z ajp $
 */
public interface ObjectBenchListener extends EventListener {

    void objectEvent(ObjectBenchEvent e);

}
