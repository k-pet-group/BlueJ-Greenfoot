package bluej.stride.framedjava.slots;

import java.util.List;
import java.util.Optional;

import bluej.editor.stride.FrameCatalogue;
import bluej.stride.framedjava.ast.ExpressionSlotFragment;
import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.framedjava.ast.SuperThis;
import bluej.stride.framedjava.ast.links.PossibleLink;
import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.InteractionManager;
import bluej.stride.slots.ChoiceSlot;
import bluej.utility.javafx.FXConsumer;

/**
 * Created by neil on 22/05/2016.
 */
public abstract class ExpressionSlot<SLOT_FRAGMENT extends ExpressionSlotFragment> extends StructuredSlot<SLOT_FRAGMENT, InfixExpression, ExpressionCompletionCalculator>
{    
    // If we are an expression slot for the parameters to the super/this call in a constructor,
    // this points to the choice slot preceding us, which we use to decide hints in our slot.
    // If we are not parameters to super/this, paramsToConstructor will be null.
    private ChoiceSlot<SuperThis> paramsToConstructor;
    
    public ExpressionSlot(InteractionManager editor, Frame parentFrame, CodeFrame<?> parentCodeFrame, FrameContentRow row, String stylePrefix, List<FrameCatalogue.Hint> hints)
    {
        super(editor, parentFrame, parentCodeFrame, row, stylePrefix, new ExpressionCompletionCalculator(editor), hints);
    }

    @Override
    public ExpressionSlot asExpressionSlot() { return this; }

    public void setText(ExpressionSlotFragment rhs)
    {
        rhs.registerSlot(this);
        setText(rhs.getContent());        
    }

    @Override
    protected InfixExpression newInfix(InteractionManager editor, String stylePrefix)
    {
        return new InfixExpression(editor, this, stylePrefix);
    }
    
    @Override
    public void saved()
    {
        if (getParentFrame().isFrameEnabled())
        {
            if (paramsToConstructor != null)
            {
                topLevel.treatAsConstructorParams_updatePrompts();
            }
        }
    }


    public void setParamsToConstructor(ChoiceSlot<SuperThis> paramsToConstructor)
    {
        this.paramsToConstructor = paramsToConstructor;
    }

    // package-visible
    boolean isConstructorParams()
    {
        return paramsToConstructor != null;
    }

    // package-visible
    void withParamNamesForConstructor(FXConsumer<List<List<String>>> handler)
    {
        editor.afterRegenerateAndReparse(() -> {
            completionCalculator.withConstructorParamNames(paramsToConstructor.getValue(SuperThis.EMPTY), handler);
        });
    }

    // package-visible
    void withParamNamesForPos(CaretPos pos, String methodName, FXConsumer<List<List<String>>> handler)
    {
        editor.afterRegenerateAndReparse(() -> {
            JavaFragment.PosInSourceDoc posJava = getSlotElement().getPosInSourceDoc(topLevel.caretPosToStringPos(pos, true));
            completionCalculator.withParamNames(posJava, this.asExpressionSlot(), methodName, parentCodeFrame.getCode(), handler);
        });
    }

    // package-visible
    void withParamHintsForPos(CaretPos pos, String methodName, FXConsumer<List<List<String>>> handler)
    {
        editor.afterRegenerateAndReparse(() -> {
            JavaFragment.PosInSourceDoc posJava = getSlotElement().getPosInSourceDoc(topLevel.caretPosToStringPos(pos, true));
            completionCalculator.withParamHints(posJava, this.asExpressionSlot(), methodName, parentCodeFrame.getCode(), handler);
        });
    }

    // package-visible
    void withParamHintsForConstructor(int totalParams, FXConsumer<List<List<String>>> handler)
    {
        editor.afterRegenerateAndReparse(() -> {
            completionCalculator.withConstructorParamHints(paramsToConstructor.getValue(SuperThis.EMPTY), totalParams, handler);
        });
    }

    // package-visible
    void withMethodHint(CaretPos pos, String methodName, FXConsumer<List<String>> handler)
    {
        editor.afterRegenerateAndReparse(() -> {
            JavaFragment.PosInSourceDoc posJava = getSlotElement().getPosInSourceDoc(topLevel.caretPosToStringPos(pos, true));
            completionCalculator.withMethodHints(posJava, this.asExpressionSlot(), methodName, parentCodeFrame.getCode(), handler);
        });
    }

    @Override
    public boolean canCollapse()
    {
        return isConstructorParams();
    }

    public List<? extends PossibleLink> findLinks()
    {
        return topLevel.findLinks(Optional.empty(), getSlotElement().getVars(), offset -> getSlotElement().getPosInSourceDoc(offset), 0);
    }
}
