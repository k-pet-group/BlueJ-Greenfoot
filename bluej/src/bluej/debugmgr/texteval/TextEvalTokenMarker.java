package bluej.debugmgr.texteval;

import org.gjt.sp.jedit.syntax.*;

/**
 * Text Evaluator token marker.
 *
 * @author Michael Kšlling
 * @version $Id: TextEvalTokenMarker.java 2614 2004-06-15 15:37:24Z mik $
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
            keywords.add("get", (byte)0);
            keywords.add("inspect", (byte)0);
        }
        return keywords;
    }
}
