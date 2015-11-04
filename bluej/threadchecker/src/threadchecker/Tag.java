package threadchecker;

public enum Tag
{
    // FX is the FX thread, Swing is the EDT
    // Unique means it always runs in a thread which will be different from all others
    //  (including, and especially, FX and Swing).
    // Things like Thread.run or SwingWorker.construct are tagged as Unique
    // Any means that the method is safe to call from any thread (including FX, Swing, and others)
    // FX means the FX thread, but calls to SwingUtilities.invokeLater
    // are banned.  This is used when we know the Swing thread is blocked.
    // Whereas FX_UsesSwing can make invokeLater calls.
    FX, FX_WaitsForSwing, Swing, Swing_WaitsForFX, Unique, Simulation, Any;

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
        else if (parent == FX_WaitsForSwing && this == FX)
            return true; // FX can override FX_UsesSwing, but not vice versa
        else if (parent == Swing_WaitsForFX && this == Swing)
            return true; // Swing can override Swing_WaitsForFX, but not vice versa
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
            return sameInstance && this == Tag.Unique; // Can't call a unique thread directly unless same instance
        else if (dest == Tag.FX && this == Tag.FX_WaitsForSwing)
            return true; // FX_UsesSwing can call FX, but not vice versa
        else if (dest == Tag.Swing && this == Tag.Swing_WaitsForFX)
            return true; // Swing_WaitsForFX can call Swing, but not vice versa
        else
            return this == dest;
        
    }
}