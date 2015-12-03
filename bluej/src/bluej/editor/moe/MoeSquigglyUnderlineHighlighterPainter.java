package bluej.editor.moe;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.text.View;

import bluej.Config;

public class MoeSquigglyUnderlineHighlighterPainter implements AdvancedHighlightPainter
{
    private final Color stroke;
    private final Function<Integer, Integer> offsetToLineNumber;

    public MoeSquigglyUnderlineHighlighterPainter(Color stroke, Function<Integer, Integer> offsetToLineNumber)
    {
        this.stroke = stroke;
        this.offsetToLineNumber = offsetToLineNumber;
    }

    public void paint(Graphics g, int offs0, int offs1, Shape bounds,
            JTextComponent c, View view)
    {        
        // Should only render part of View.
        try {
            // --- determine locations ---
            offs0 = Math.max(offs0, view.getStartOffset());
            offs1 = Math.min(offs1, view.getEndOffset());
            
            List<Rectangle> rs = new ArrayList<>();
            int lineStart = offs0;
            // Find all the lines involved:
            for (int i = offs0; i < offs1; i++)
            {
                if ((int)offsetToLineNumber.apply(i) != (int)offsetToLineNumber.apply(i+1))
                {
                    Shape s = view.modelToView(lineStart, Position.Bias.Forward, i, Position.Bias.Backward, bounds);
                    rs.add(s.getBounds());
                    lineStart = i + 1;
                }
            }
            Shape s = view.modelToView(lineStart, Position.Bias.Forward, offs1, Position.Bias.Backward, bounds);
            rs.add(s.getBounds());
            
            for (Rectangle r : rs)
            {
                g.setColor(stroke);

                int width = r.width + 2;
                int startX = r.x - 1;
                int startY = r.y + r.height - 3;

                int n = 0;
                //if too small, add width to overshoot on the left and right
                //also add more 2 squiggly points.
                if (width < 15)
                {
                    startX -= (int) ((15 - width) / 2) + 1;
                    width = 15;
                }

                n += Math.round((double)width / 2.0) + 1;
                int[] xPoints = new int[n + 1];
                int[] yPoints = new int[n + 1];

                for (int j = 0; j <= n; j++)
                {
                    xPoints[j] = startX + j * 2;
                    yPoints[j] = startY + 3 * (j % 2);
                }
                g.drawPolyline(xPoints, yPoints, n + 1);
            }
            
        } catch (BadLocationException e) {
            // throw new RuntimeException(e);
            return;
        }
    }

    // Copied from MoeBorderHighlighterPainter:
    @Override
    public void issueRepaint(int p0, int p1, Shape viewBounds,
            JTextComponent editor, View rootView)
    {
        try {
            Shape s = rootView.modelToView(p0, Position.Bias.Forward, p1, Position.Bias.Backward, viewBounds);
            Rectangle r = s.getBounds();
            r.x -= 2;
            r.width += 4;
            editor.repaint(r);
        }
        catch (BadLocationException ble) {
            throw new RuntimeException(ble);
        }
    }
}
