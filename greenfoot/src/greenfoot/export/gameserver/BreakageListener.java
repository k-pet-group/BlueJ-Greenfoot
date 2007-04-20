/*
 * BreakageListener.java
 *
 * Created on April 26, 2006, 8:44 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package greenfoot.export.gameserver;

/**
 *
 * @author James Gosling
 */
public interface BreakageListener {
    public void connectionBroken(Throwable t, Endpoint e);
}
