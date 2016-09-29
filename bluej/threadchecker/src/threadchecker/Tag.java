package threadchecker;

public enum Tag
{
    // FXPlatform is the FX thread, Swing is the EDT
    // FX means either FX thread or a loader thread for FX
    // Unique means it always runs in a thread which will be different from all others
    //  (including, and especially, FX and Swing).
    // Things like Thread.run or SwingWorker.construct are tagged as Unique
    // Worker is similar to Unique, except that Unique tags can only call
    // methods on its own thread (one Unique thread can't cal another Unique thread)
    // but Worker threads can do cross-calling.
    // Any means that the method is safe to call from any thread (including FX, Swing, and others)
    FX, FXPlatform, Swing, Unique, Simulation, Worker, Any;

    /**
     * Checks if this tag on a method is allowed when overriding the given (potentially null) parent method tag.
     * 
     * The rule is pretty simple: if the parent tag is present, it must match this tag.
     * If the parent tag is empty, this tag must be Any (because the untagged parent can be called from any thread, so we can too)
     */
    public boolean canOverride(Tag parent)
    {
        if (parent == null)
            return this == Any;
        else if (parent == FXPlatform && this == FX)
            return true; // FX can override FXPlatform, but not vice versa
        else if (this == Any)
            return true; // Any can override a more-specific parent tag
        else
            return this == parent;
    }

    /**
     * Checks if code tagged with this tag can call a method tagged with the given (potentially null) tag.
     * 
     * The rule is simple: we return false if the destination tag is Unique; or is FX/Swing and does not match this tag.
     * In all other cases we return true.
     */
    public boolean canCall(Tag dest, boolean sameInstance)
    {
        if (dest == null || dest == Any) // Can call dest any from any source
            return true;
        else if (dest == Tag.Unique)
            return sameInstance && this == Tag.Unique; // Can't call a unique thread directly unless same instance)
        else if (dest == Tag.FX && this == Tag.FXPlatform)
            return true; // FXPlatform can call FX, but not vice versa
        else
            return this == dest;
        
    }
}