package bluej.editor.moe;

import java.awt.Color;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.JTextComponent;

import bluej.Config;


public class MoeHighlighter extends DefaultHighlighter {

    protected HighlightPainter selectPainter ;
    protected HighlightPainter highlightPainter;
    
    
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
        selectPainter=new MoeHighlighterPainter(Config.getSelectionColour()); 
        highlightPainter=new MoeHighlighterPainter(Config.getHighlightColour());
        install(comp);
    }
    
    // Convenience method to add a highlight with
    // the default painter.
    public Object addHighlight(int p0, int p1) throws BadLocationException {
      return super.addHighlight(p0, p1, highlightPainter);
    }
}
