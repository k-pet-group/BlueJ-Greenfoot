package bluej.stride.framedjava.slots;

import java.util.Arrays;
import java.util.List;

import bluej.editor.stride.FrameCatalogue;
import bluej.stride.framedjava.ast.CallExpressionSlotFragment;
import bluej.stride.framedjava.ast.FilledExpressionSlotFragment;
import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.InteractionManager;

/**
 * Created by neil on 04/12/2015.
 */
public class CallExpressionSlot extends ExpressionSlot<CallExpressionSlotFragment>
{
    public static final List<FrameCatalogue.Hint> CALL_HINTS = Arrays.asList(
        new FrameCatalogue.Hint("move(3)", "Move forward 3 pixels"),
        new FrameCatalogue.Hint("turn(5)", "Turn right 5 degrees"),
        new FrameCatalogue.Hint("removeTouching(Crab.class)", "Remove touching Crab actors")
    );

    public CallExpressionSlot(InteractionManager editor, Frame parentFrame, CodeFrame<?> parentCodeFrame, FrameContentRow row, String stylePrefix, List<FrameCatalogue.Hint> hints)
    {
        super(editor, parentFrame, parentCodeFrame, row, stylePrefix, hints);
    }

    @Override
    protected CallExpressionSlotFragment makeSlotFragment(String content, String javaCode)
    {
        return new CallExpressionSlotFragment(content, javaCode, this);
    }
}
