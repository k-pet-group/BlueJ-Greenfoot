package bluej.stride.framedjava.errors;

import java.util.Collections;
import java.util.List;

import bluej.stride.framedjava.ast.StringSlotFragment;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.stride.framedjava.ast.JavaFragment;
import bluej.utility.javafx.FXRunnable;

public class UnneededSemiColonError extends SyntaxCodeError
{
    private final FXRunnable fix;

    @OnThread(Tag.Any)
    public UnneededSemiColonError(StringSlotFragment slot, FXRunnable fix)
    {
        super(slot, "Unnecessary semi-colon at end of slot");
        this.fix = fix;
    }

    @Override
    public List<FixSuggestion> getFixSuggestions()
    {
        return Collections.singletonList(new FixSuggestion() {
            
            @Override
            public String getDescription()
            {
                return "Remove semi-colon";
            }
            
            @Override
            public void execute()
            {
                fix.run();
            }
        });
    }
}
