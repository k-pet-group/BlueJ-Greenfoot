package bluej.debugmgr.texteval;

import org.gjt.sp.jedit.syntax.*;

/**
 * Text Evaluator token marker.
 *
 * @author Michael Kšlling
 * @version $Id: TextEvalTokenMarker.java 2618 2004-06-17 14:03:32Z mik $
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
