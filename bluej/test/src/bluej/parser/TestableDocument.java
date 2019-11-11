package bluej.parser;

import bluej.editor.flow.HoleDocument;
import bluej.editor.flow.JavaSyntaxView;
import bluej.editor.flow.ScopeColors;
import bluej.parser.entity.EntityResolver;
import bluej.parser.nodes.ReparseableDocument;
import javafx.beans.property.ReadOnlyBooleanWrapper;

public class TestableDocument extends JavaSyntaxView implements ReparseableDocument
{
    boolean parsingSuspended = false;
    
    public TestableDocument(EntityResolver entityResolver)
    {
        super(new HoleDocument(), null, ScopeColors.dummy(), entityResolver, new ReadOnlyBooleanWrapper(true));
    }
    
    public TestableDocument()
    {
        this(null);
    }

    public void insertString(int pos, String content)
    {
        document.replaceText(pos, pos, content);
    }
    
    public void remove(int start, int length)
    {
        document.replaceText(start, start + length, "");
    }

    @Override
    public void flushReparseQueue()
    {
        if (!parsingSuspended)
            super.flushReparseQueue();
    }
}
