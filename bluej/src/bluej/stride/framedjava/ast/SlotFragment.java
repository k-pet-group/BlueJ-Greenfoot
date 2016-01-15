package bluej.stride.framedjava.ast;

import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.framedjava.errors.DirectSlotError;
import bluej.stride.generic.InteractionManager;

import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * Created by neil on 20/02/2015.
 */
public abstract class SlotFragment extends JavaFragment
{
    /*
    public final void showError(CodeError codeError)
    {
        getSlot().addError(codeError);
    }
    */

    /**
     * Finds errors that do not prevent compilation.  Often these errors
     * overlap javac errors, but add more information or suggested fixes
     * @param editor
     * @return Null if no future, otherwise a future to complete for errors
     */
    public Future<List<DirectSlotError>> findLateErrors(InteractionManager editor, CodeElement parent, Function<JavaFragment, String> rootPathMap)
    {
        return null;
    }
}
