package bluej.stride.framedjava.errors;

import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.ast.StringSlotFragment;
import bluej.stride.slots.EditableSlot;
import javafx.application.Platform;

/**
 * A class for errors which are directly targeted at a given slot,
 * like our own unknown-variable errors or extra semi-colon error
 * (as distinct from JavaCompileError, which is targeted at Java code, which maps to a slot)
 */
public abstract class DirectSlotError extends CodeError
{
    public DirectSlotError(SlotFragment code)
    {
        super(code);
    }
}
