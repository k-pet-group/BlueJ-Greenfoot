package bluej.stride.framedjava.convert;

import java.util.ArrayList;
import java.util.List;

import bluej.parser.lexer.LocatableToken;

/**
 * Created by neil on 03/06/2016.
 */
class FieldOrVarBuilder
{
    final String type;
    final List<LocatableToken> modifiers = new ArrayList<>();

    FieldOrVarBuilder(String type, List<LocatableToken> modifiers)
    {
        this.type = type;
        this.modifiers.addAll(modifiers);
    }
}
