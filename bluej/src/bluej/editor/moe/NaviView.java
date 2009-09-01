package bluej.editor.moe;

import java.awt.Color;
import java.awt.Event;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Segment;

public class NaviView extends JComponent
{
    Document document;
    
    public NaviView(Document document, JScrollBar scrollBar)
    {
        this.document = document;
        setOpaque(true);
        setFocusable(true);
        setEnabled(true);
        enableEvents(Event.MOUSE_MOVE  // Needed for tooltips apparently?
                | Event.MOUSE_DOWN
                | Event.MOUSE_DRAG);
        setToolTipText(""); // Otherwise tooltips don't work at all?
    }
    
    public void setDocument(Document document)
    {
        this.document = document;
        repaint();
    }
        
    @Override
    public String getToolTipText(MouseEvent event)
    {
        return "Pos: " + event.getY();
    }
    
    @Override
    protected void paintComponent(Graphics g)
    {
        Rectangle clipBounds = new Rectangle(new Point(0,0), getSize());
        g.getClipBounds(clipBounds);
        g.setColor(Color.WHITE);
        g.fillRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);
        
        if (document == null) {
            // Should not happen
            return;
        }
        
        Element map = document.getDefaultRootElement();
        Segment lineSeg = new Segment();
        int lines = map.getElementCount();
        
        try {
            g.setColor(Color.BLACK);
            for (int i = 0; i < lines; i++) {
                Element lineEl = map.getElement(i);
                int start = lineEl.getStartOffset();
                int end = lineEl.getEndOffset();
                document.getText(start, end - start, lineSeg);
                
                int pos = lineSeg.offset;
                int endPos = pos + lineSeg.count;
                int xpos = 0;
                for (int j = pos; j < endPos; j++) {
                    if (! Character.isWhitespace(lineSeg.array[j])) {
                        g.drawLine(xpos, i, xpos, i);
                        xpos++;
                    }
                    else if (lineSeg.array[j] == '\t') {
                        xpos += 4; // TODO use real tab size
                    }
                    else {
                        xpos++;
                    }
                }
            }
        }
        catch (BadLocationException ble) {
            throw new RuntimeException(ble);
        }
    }
}
