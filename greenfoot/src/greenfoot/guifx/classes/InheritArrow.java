package greenfoot.guifx.classes;

import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

import java.util.List;

/**
 * I tried many different variants before using this design:
 *  - If you have each L piece in the diagram as separate items, it is difficult
 *    to get the join to line up
 *  - The same applies if you have the triangle as a separate piece
 *  - If you use Polyline, it's awkward to draw multiple dangling "side-arms"
 *    as you have to redraw parts of the line.
 *  Hence the finished solution: A Path that does all the inheritance arrows for
 *  a particular class (down to its subclasses) as a single item, using a mixture
 *  of LineTo and MoveTo.
 */
public class InheritArrow extends Path
{
    private final double ARROWHEAD_WIDTH = 12;
    private final double ARROWHEAD_HEIGHT = 8;
    
    public InheritArrow()
    {
                
    }

    /**
     * Set the locations of the subclass arms (the pieces going from the main vertical
     * line across to the right to touch the classes).
     * @param width The width of each subclass arm from the vertical line
     * @param yPositions The Y positions (relative to the top of this inherit arrow) of
     *                   the arms to draw.
     */
    public void setArmLocations(double width, List<Double> yPositions)
    {
        // If no arms, then no subclasses: clear all elements to effectively hide this arrow:
        if (yPositions.isEmpty())
        {
            getElements().clear();
            return;
        }
        
        // Draw arrow head and end at the bottom middle:
        getElements().setAll(
                new MoveTo(ARROWHEAD_WIDTH / 2.0, ARROWHEAD_HEIGHT), // Mid bottom of arrow
                new LineTo(0.0, ARROWHEAD_HEIGHT), // bottom left of arrow
                new LineTo(ARROWHEAD_WIDTH / 2.0, 0.0), // Top of arrow
                new LineTo(ARROWHEAD_WIDTH, ARROWHEAD_HEIGHT), // bottom right of arrow
                new LineTo(ARROWHEAD_WIDTH / 2.0, ARROWHEAD_HEIGHT) // Mid bottom of arrow
        );

        double indent = ARROWHEAD_WIDTH / 2.0;
        for (Double yPosition : yPositions)
        {
            // Draw down, then draw right, then move back to the bottom left of the current line:
            getElements().addAll(
                new LineTo(indent, yPosition),
                new LineTo(indent + width, yPosition),
                new MoveTo(indent, yPosition)
            );
        }
    }
}
