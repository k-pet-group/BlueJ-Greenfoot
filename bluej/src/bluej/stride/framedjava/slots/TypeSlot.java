package bluej.stride.framedjava.slots;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import bluej.Config;
import bluej.editor.stride.FrameCatalogue;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.ast.links.PossibleTypeLink;
import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.framedjava.frames.ReturnFrame;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.SuggestedFollowUpDisplay;
import bluej.stride.slots.TypeCompletionCalculator;
import bluej.utility.Utility;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;

/**
 * Created by neil on 22/05/2016.
 */
public class TypeSlot extends StructuredSlot<TypeSlotFragment, InfixType, TypeCompletionCalculator>
{
    private final InteractionManager editor;
    private boolean isReturnType = false;
    
    public static enum Role
    {
        /** Declaring arbitrary variable; can be any type */
        DECLARATION,
        /** Return type; like DECLARATION but could also be void */
        RETURN,
        /** Extends; must be a non-final class (not interface or primitive) */
        EXTENDS,
        /** Must be an interface */
        INTERFACE,
        /** Throws or Catch; must be a throwable */
        THROWS_CATCH;
    }
    
    public TypeSlot(InteractionManager editor, Frame parentFrame, CodeFrame<?> parentCodeFrame, FrameContentRow row, Role role, String stylePrefix)
    {
        super(editor, parentFrame, parentCodeFrame, row, stylePrefix, calculatorForRole(editor, role), hintsForRole(role));
        this.editor = editor;
        onTextPropertyChangeOld(this::adjustReturnFrames);
    }

    private static TypeCompletionCalculator calculatorForRole(InteractionManager editor, Role role)
    {
        switch (role)
        {
            case THROWS_CATCH:
                return new TypeCompletionCalculator(editor, Throwable.class);
            case INTERFACE:
                return new TypeCompletionCalculator(editor, InteractionManager.Kind.INTERFACE);
            case EXTENDS:
                return new TypeCompletionCalculator(editor, InteractionManager.Kind.CLASS_NON_FINAL);
        }
        return new TypeCompletionCalculator(editor);
    }

    private static List<FrameCatalogue.Hint> hintsForRole(Role role)
    {
        FrameCatalogue.Hint hintInt = new FrameCatalogue.Hint("int", "An integer (whole number)");
        FrameCatalogue.Hint hintDouble = new FrameCatalogue.Hint("double", "A number value");
        FrameCatalogue.Hint hintVoid = new FrameCatalogue.Hint("void", "No return");
        FrameCatalogue.Hint hintString = new FrameCatalogue.Hint("String", "Some text");
        FrameCatalogue.Hint hintActor = new FrameCatalogue.Hint("Actor", "A Greenfoot actor");
        FrameCatalogue.Hint hintList = new FrameCatalogue.Hint("List<String>", "A list of String");
        FrameCatalogue.Hint hintObj = Config.isGreenfoot() ? hintActor : hintList;
        FrameCatalogue.Hint hintIO = new FrameCatalogue.Hint("IOException", "An IO exception");
        switch (role)
        {
            case DECLARATION:
                return Arrays.asList(hintInt, hintDouble, hintString, hintObj);
            case RETURN:
                return Arrays.asList(hintInt, hintDouble, hintString, hintVoid);
            case EXTENDS:
                if (Config.isGreenfoot())
                    return Arrays.asList(hintActor);
                else
                    return Collections.emptyList();
            case INTERFACE:
                return Collections.emptyList();
            case THROWS_CATCH:
                return Arrays.asList(hintIO);
        }
        return Collections.emptyList();
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
    protected InfixType newInfix(InteractionManager editor, String stylePrefix)
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

    @Override
    public void saved()
    {
        
    }

    @Override
    public boolean canCollapse()
    {
        // Type slots can never be collapsed:
        return false;
    }

    @Override
    public List<PossibleTypeLink> findLinks()
    {
        //TODOTYPESLOT find links for sub-types of generics too, and for "Object" in Object[]
        return Collections.singletonList(new PossibleTypeLink(getText(), 0, getText().length(), this));
    }
}
