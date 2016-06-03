package bluej.stride.framedjava.convert;

import java.util.ArrayList;
import java.util.List;

import bluej.parser.lexer.LocatableToken;
import bluej.stride.framedjava.ast.ParamFragment;

/**
 * Created by neil on 03/06/2016.
 */
class MethodBuilder
{
    final String name;
    final List<Modifier> modifiers = new ArrayList<>();
    final List<ParamFragment> parameters = new ArrayList<>();
    final List<String> throwsTypes = new ArrayList<>();
    final String comment; // may be null
    String constructorCall; // may be null
    List<Expression> constructorArgs; // may be null
    final String type;
    boolean hasBody = false;

    MethodBuilder(String type, String name, List<Modifier> modifiers, String comment)
    {
        this.type = type;
        this.name = name;
        this.modifiers.addAll(modifiers);
        this.comment = comment;
    }
}
