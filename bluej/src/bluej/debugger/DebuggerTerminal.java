package bluej.debugger;

import java.io.*;

/**
 *
 * @author  Andrew Patterson
 * @version $Id: DebuggerTerminal.java 2328 2003-11-13 04:08:23Z ajp $
 */
public interface DebuggerTerminal
{
    Writer getErrorWriter();

    Writer getWriter();

    Reader getReader();
}
