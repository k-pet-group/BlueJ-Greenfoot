package greenfoot.event;

import rmiextension.wrappers.event.RCompileEvent;

/**
 * Listens for compile events in greenfoot.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: CompileListener.java 3124 2004-11-18 16:08:48Z polle $
 */
public interface CompileListener
{
    public void compileStarted(RCompileEvent event);

    public void compileError(RCompileEvent event);

    public void compileWarning(RCompileEvent event);

    public void compileSucceeded(RCompileEvent event);

    public void compileFailed(RCompileEvent event);
}