package bluej.editor.moe.autocomplete.parser;
import java.util.ArrayList;

/**
 * This is the inheritance between the MoeAutocompleteManager
 * and the Parser.  The MoeAutocompleteManager should
 * implement this interface and register itself as
 * a ParserListener with the Parser.  It will then
 * be informed about the established type if it
 * can be determined.
 *
 * THIS INTERFACE IS NOT SPEED JAVA CODE
 *
 * @version 1.2 (10/03/2003)
 *
 * @author Chris Daly (<A href="mailto:cdd1@ukc.ac.uk">cdd1@ukc.ac.uk</A>),
 *         Darren Link (<A href="mailto:drl1@ukc.ac.uk">drl1@ukc.ac.uk</A>),
 *         Mike Stewart (<A href="mailto:mjs7@ukc.ac.uk">mjs7@ukc.ac.uk</A>)
 */
public interface ParserListener {

    /**
     * Used to inform the implementing class about the established type
     * and the MoeDropDownItems that are associated with it.
     *
     * @param type the type established by the parser.
     * @param items an ArrayList containing the applicable MoeDropDownItems
     *              for the established type.
     */
    void typeEstablished(Class type, ArrayList members);

    /**
     * Used to inform the implementing class that the type
     * could not be established.
     */
    void typeNotEstablished();

}

