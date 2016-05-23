package bluej.stride.framedjava.slots;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javafx.application.Platform;

import bluej.editor.stride.FrameCatalogue;
import bluej.stride.framedjava.ast.ExpressionSlotFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.framedjava.frames.ReturnFrame;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.SuggestedFollowUpDisplay;
import bluej.stride.slots.CompletionCalculator;
import bluej.utility.Utility;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;

/**
 * Created by neil on 22/05/2016.
 */
public class TypeSlot extends StructuredSlot<TypeSlotFragment>
{
    private final InteractionManager editor;
    private boolean isReturnType = false;
    
    public TypeSlot(InteractionManager editor, Frame parentFrame, CodeFrame<?> parentCodeFrame, FrameContentRow row, CompletionCalculator completionCalculator, String stylePrefix)
    {
        super(editor, parentFrame, parentCodeFrame, row, stylePrefix, Collections.emptyList() /*TODOTYPESLOT*/);
        this.editor = editor;
        onTextPropertyChangeOld(this::adjustReturnFrames);
    }

    @Override
    protected TypeSlotFragment makeSlotFragment(String content, String javaCode)
    {
        return new TypeSlotFragment(content, javaCode, this);
    }

    @Override
    public ExpressionSlot asExpressionSlot() { return null; }

    public void setText(TypeSlotFragment rhs)
    {
        rhs.registerSlot(this);
        setText(rhs.getContent());        
    }

    @Override
    protected InfixStructured newInfix(InteractionManager editor, String stylePrefix)
    {
        return new InfixType(editor, this, stylePrefix);
    }

    public void markReturnType()
    {
        isReturnType = true;
    }
    
    private void adjustReturnFrames(String oldValue, String newValue)
    {
        if ((oldValue.equals("void") || oldValue.equals("")) && !(newValue.equals("void") || newValue.equals("")))
        {
            // Added a return type; need to go through and add empty slots for all returns that don't have them:
            for (Frame f : Utility.iterableStream(getParentFrame().getAllFrames()))
            {
                if (f instanceof ReturnFrame)
                {
                    ReturnFrame rf = (ReturnFrame) f;
                    rf.showValue();
                }
            }
        }
        else if (!oldValue.equals("void") && newValue.equals("void"))
        {
            // Removed a return type; prompt about removing return values from all returns
            List<FXRunnable> removeActions = getParentFrame().getAllFrames()
                .filter(f -> f instanceof ReturnFrame)
                .map(f -> (ReturnFrame)f)
                .map(rf -> rf.getRemoveFilledValueAction())
                .filter(a -> a != null)
                .collect(Collectors.toList());

            if (!removeActions.isEmpty())
            {
                JavaFXUtil.runNowOrLater(() -> {
                    SuggestedFollowUpDisplay disp = new SuggestedFollowUpDisplay(editor, "Return type changed to void.  Would you like to remove return values from all return frames in this method?", () -> removeActions.forEach(FXRunnable::run));
                    disp.showBefore(getComponents().get(0));
                });
            }
        }
    }
}
