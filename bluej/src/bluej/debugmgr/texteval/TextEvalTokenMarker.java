package bluej.debugmgr.texteval;

import org.syntax.jedit.KeywordMap;
import org.syntax.jedit.tokenmarker.CTokenMarker;

/**
 * Text Evaluator token marker.
 *
 * @author Michael Kolling
 * @version $Id: TextEvalTokenMarker.java 3070 2004-11-08 04:14:32Z bquig $
 */
public class TextEvalTokenMarker extends CTokenMarker
{
    private static KeywordMap keywords;

    public TextEvalTokenMarker()
    {
        super(false, getKeywords());
    }

    public static KeywordMap getKeywords()
    {
        if(keywords == null) {
            keywords = new KeywordMap(false);
        }
        return keywords;
    }
}
