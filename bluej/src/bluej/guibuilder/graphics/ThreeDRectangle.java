package javablue.gui;

import java.awt.*;

/**
 * A DrawnRectangle which draws in 3D.<p>
 *
 * Drawn raised by default, drawing style used by paint() is 
 * controlled by raise() and inset().  Note that raise() and 
 * inset() do not result in anything being painted, but only set
 * the state for the next call to paint().  To set the state and
 * paint in one operation, use paintRaised() and paintInset().
 * <p>
 *
 * The current state of the rectangle may be obtained by 
 * calling isRaised().<p>
 *
 * @version 1.0, Apr 1 1996
 * @author  David Geary
 * @see     DrawnRectangle
 * @see     EtchedRectangle
 * @see     javablue.gui.test.DrawnRectangleTest
 */
public class ThreeDRectangle extends DrawnRectangle {
    protected static BorderStyle 
                      _defaultState = BorderStyle.RAISED;

    private BorderStyle state;

    public ThreeDRectangle(Component drawInto) {
        this(drawInto, _defaultState, 
             _defaultThickness, 0, 0, 0, 0);
    }
    public ThreeDRectangle(Component drawInto, int thickness) {
        this(drawInto, _defaultState, thickness, 0, 0, 0, 0);
    }
    public ThreeDRectangle(Component drawInto, 
                           int x, int y, int w, int h) {
        this(drawInto, 
             _defaultState, _defaultThickness, x, y, w, h);
    }
    public ThreeDRectangle(Component drawInto, int thickness,
                                               int x, int y, 
                                               int w, int h) {
        this(drawInto, _defaultState, thickness, x, y, w, h);
    }
    public ThreeDRectangle(Component drawInto, 
                           BorderStyle state, 
                           int thickness, int x, int y, 
                           int w, int h) {
        super(drawInto, thickness, x, y, w, h);
        this.state = state;
    }
    public void paint() {
        if(state == BorderStyle.RAISED) paintRaised();
        else                                  paintInset ();
    }
    public void raise() { state = BorderStyle.RAISED; }
    public void inset() { state = BorderStyle.INSET;  }

    public boolean isRaised() { 
        return state == BorderStyle.RAISED; 
    }
    public String paramString() {
        return super.paramString() + "," + state;
    }
    public void paintRaised() {
        Graphics g = drawInto.getGraphics();

        if(g != null) {
            raise               ();
            drawTopLeftLines    (g, brighter());
            drawBottomRightLines(g, getLineColor());
			g.dispose();
        }
    }    
    public void paintInset() {
        Graphics g = drawInto.getGraphics();

        if(g != null) {
            inset               ();
            drawTopLeftLines    (g, getLineColor());
            drawBottomRightLines(g, brighter());
			g.dispose();
        }
    }
    private void drawTopLeftLines(Graphics g, Color color) {
        int thick = getThickness();
        g.setColor(color);

        for(int i=0; i < thick; ++i) {
            g.drawLine(x+i, y+i,   x + width-(i+1), y+i);
            g.drawLine(x+i, y+i+1, x+i, y + height-(i+1));  
        }
    }
    private void drawBottomRightLines(Graphics g, Color color) {
        int thick = getThickness();
        g.setColor(color);

        for(int i=1; i <= thick; ++i) {
            g.drawLine(x+i-1, y + height-i, 
                       x + width-i, y + height-i);
            g.drawLine(x + width-i, y+i-1,        
                       x + width-i, y + height-i);
        }
    }
}
