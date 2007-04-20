/*
 * CommandExecutor.java
 *
 * Created on March 22, 2006, 12:36 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package greenfoot.export.gameserver;

/**
 * an Unspeakably Trivial Command
 */
public interface UTCmd {
    public void execute(Object target, UTCL ctx);
}
