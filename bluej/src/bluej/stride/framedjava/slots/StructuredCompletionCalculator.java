package bluej.stride.framedjava.slots;

import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.slots.SuggestionList;
import bluej.utility.javafx.FXPlatformConsumer;

/**
 * Created by neil on 25/05/2016.
 */
public interface StructuredCompletionCalculator
{
    public void withCalculatedSuggestionList(JavaFragment.PosInSourceDoc pos, ExpressionSlot<?> completing, CodeElement codeEl, SuggestionList.SuggestionListListener clickListener, String targetType, FXPlatformConsumer<SuggestionList> handler);
}
