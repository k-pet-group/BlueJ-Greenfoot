package bluej.stride.framedjava.slots;

import java.util.List;

import bluej.editor.stride.FrameCatalogue;
import bluej.stride.framedjava.ast.ExpressionSlotFragment;
import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.InteractionManager;

/**
 * Created by neil on 22/05/2016.
 */
public abstract class ExpressionSlot<SLOT_FRAGMENT extends ExpressionSlotFragment> extends StructuredSlot<SLOT_FRAGMENT>
{
    public ExpressionSlot(InteractionManager editor, Frame parentFrame, CodeFrame<?> parentCodeFrame, FrameContentRow row, String stylePrefix, List<FrameCatalogue.Hint> hints)
    {
        super(editor, parentFrame, parentCodeFrame, row, stylePrefix, hints);
    }

    @Override
    public ExpressionSlot asExpressionSlot() { return this; }

    public void setText(ExpressionSlotFragment rhs)
    {
        rhs.registerSlot(this);
        setText(rhs.getContent());        
    }

    @Override
    protected InfixStructured newInfix(InteractionManager editor, String stylePrefix)
    {
        return new InfixExpression(editor, this, stylePrefix);
    }
}
