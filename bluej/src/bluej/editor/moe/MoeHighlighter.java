package bluej.editor.moe;

import java.awt.Color;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.JTextComponent;


public class MoeHighlighter extends DefaultHighlighter {

    protected HighlightPainter selectPainter;
    protected HighlightPainter highlightPainter;
    protected HighlightPainter testingPainter;
    
    public MoeHighlighter(Color arg0, JTextComponent comp) 
    {
        super();
        selectPainter=new MoeHighlighterPainter(Color.YELLOW);
        highlightPainter=new MoeHighlighterPainter(arg0);
        testingPainter=new MoeHighlighterPainter(Color.GREEN);
        install(comp);
    }
    
    public MoeHighlighter(Color selectColor, Color highlightColor, JTextComponent comp) 
    {
        super();
        highlightPainter=new MoeHighlighterPainter(highlightColor);
        selectPainter=new MoeHighlighterPainter(selectColor);
        install(comp);
    }
    
    public MoeHighlighter(JTextComponent comp) 
    {
        super();
        //highlightPainter=new MoeHighlighterPainter(new Color(255, 255, 90));
        //selectPainter=new MoeHighlighterPainter(new Color(255, 255, 0)); 
        selectPainter=new MoeHighlighterPainter(new Color(249, 225, 87)); 
        highlightPainter=new MoeHighlighterPainter(new Color(255, 255, 0));
        //selectPainter=new MoeHighlighterPainter(comp.getSelectionColor());

        install(comp);
    }
    
    // Convenience method to add a highlight with
    // the default painter.
    public Object addHighlight(int p0, int p1) throws BadLocationException {
      return super.addHighlight(p0, p1, highlightPainter);
    }
}
