package bluej.debugmgr.texteval;

import org.gjt.sp.jedit.syntax.*;

/**
 * Text Evaluator token marker.
 *
 * @author Michael Kolling
 * @version $Id: TextEvalTokenMarker.java 2630 2004-06-19 14:26:37Z polle $
 */
public class TextEvalTokenMarker extends CTokenMarker
{
    private static KeywordMap keywords;

    public TextEvalTokenMarker()
    {
        super(getKeywords());
    }

    public static KeywordMap getKeywords()
    {
        if(keywords == null) {
            keywords = new KeywordMap(false);
        }
        return keywords;
    }
}
