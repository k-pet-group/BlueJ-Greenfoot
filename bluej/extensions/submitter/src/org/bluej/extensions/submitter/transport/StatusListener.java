package org.bluej.extensions.submitter.transport;

/**
 * A channel for passing status updates
 * 
 * @author Clive Miller
 * @version $Id: StatusListener.java 1463 2002-10-23 12:40:32Z jckm $
 */
public interface StatusListener
{
    public void statusChanged (String status);
}
