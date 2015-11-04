package bluej.stride.framedjava.ast;

import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.framedjava.errors.ErrorShower;
import bluej.stride.generic.Frame;
import bluej.stride.slots.EditableSlot;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Created by neil on 26/02/15.
 */
public abstract class ChoiceSlotFragment extends SlotFragment
{
    private final Frame frame;

    protected ChoiceSlotFragment(Frame f)
    {
        this.frame = f;
    }

    @Override
    public void addError(CodeError codeError)
    {
        frame.addError(codeError);
    }

    @Override
    public ErrorRelation checkCompileError(int startLine, int startColumn, int endLine, int endColumn)
    {
        if (frame == null)
            return ErrorRelation.CANNOT_SHOW;
        else
            return super.checkCompileError(startLine, startColumn, endLine, endColumn);
    }

    @Override
    @OnThread(Tag.FX)
    protected JavaFragment getCompileErrorRedirect()
    {
        EditableSlot slot = frame.getErrorShowRedirect();
        if (slot != null)
            return slot.getSlotElement();
        else
            return this;
    }

    @Override
    @OnThread(Tag.FX)
    public ErrorShower getErrorShower()
    {
        EditableSlot slot = frame.getErrorShowRedirect();
        if (slot != null)
            return slot;
        else
            return frame;
    }
}
