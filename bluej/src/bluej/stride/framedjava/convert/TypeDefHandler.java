package bluej.stride.framedjava.convert;

import java.util.List;

import bluej.parser.lexer.LocatableToken;
import bluej.stride.framedjava.elements.CodeElement;

/**
 * Created by neil on 03/06/2016.
 */
interface TypeDefHandler
{
    public void typeDefBegun(LocatableToken start);

    public void typeDefEnd(LocatableToken end);

    public void gotName(String name);

    public void startedClass(List<LocatableToken> modifiers, String doc);

    public void startedInterface(List<LocatableToken> modifiers, String doc);

    void gotContent(List<CodeElement> content);

    void typeDefExtends(String type);

    void typeDefImplements(String type);
}
