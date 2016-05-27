package bluej.stride.framedjava.slots;

import java.util.List;

import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.slots.SuggestionList;
import bluej.utility.javafx.FXPlatformConsumer;

/**
 * Created by neil on 25/05/2016.
 */
public interface StructuredCompletionCalculator
{
    public void withCalculatedSuggestionList(JavaFragment.PosInSourceDoc pos, ExpressionSlot<?> completing, CodeElement codeEl, SuggestionList.SuggestionListListener clickListener, String targetType, boolean completingStartOfSlot, FXPlatformConsumer<SuggestionList> handler);

    public String getName(int selected);
    public List<String> getParams(int selected);
    public char getOpening(int selected);
}
