package bluej.pkgmgr.dependency;

import bluej.Config;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.target.*;

import java.util.Properties;
import java.awt.*;

/**
 * A dependency between two targets in a package
 *
 * @author  Michael Cahill
 * @version $Id: UsesDependency.java 2198 2003-10-02 04:13:48Z ajp $
 */
public class UsesDependency extends Dependency
{
    private static final Color normalColour = Config.getItemColour("colour.arrow.uses");

    private static final int SELECT_DIST = 4;

    private static final float  dash1[] = {5.0f,2.0f};
    private static final BasicStroke dashedUnselected = new BasicStroke(strokeWithDefault,
                                                              BasicStroke.CAP_BUTT,
                                                              BasicStroke.JOIN_MITER,
                                                              10.0f, dash1, 0.0f);
    private static final BasicStroke dashedSelected = new BasicStroke(strokeWithSelected,
                                                                  BasicStroke.CAP_BUTT,
                                                                  BasicStroke.JOIN_MITER,
                                                                  10.0f, dash1, 0.0f);
    private static final BasicStroke normalSelected = new BasicStroke(strokeWithSelected);
    private static final BasicStroke normalUnselected = new BasicStroke(strokeWithDefault);
                                                              
                                                              
    private int src_x, src_y, dst_x, dst_y;
    private boolean start_top, end_left;
    private boolean flag;	// flag to mark some dependencies

    public UsesDependency(Package pkg, DependentTarget from, DependentTarget to)
    {
        super(pkg, from, to);
        flag = false;
    }

    public UsesDependency(Package pkg)
    {
        this(pkg, null, null);
    }

    public void setSourceCoords(int src_x, int src_y, boolean start_top)
    {
        this.src_x = src_x;
        this.src_y = src_y;
        this.start_top = start_top;
    }

    public void setDestCoords(int dst_x, int dst_y, boolean end_left)
    {
        this.dst_x = dst_x;
        this.dst_y = dst_y;
        this.end_left = end_left;
    }

    void draw(Color colour, Graphics2D g)
    {
        Stroke dashedStroke, normalStroke;
        if (isSelected()) 
        {
            dashedStroke = dashedSelected;
            normalStroke = normalSelected;         
        } 
        else
        {
            dashedStroke = dashedUnselected;
            normalStroke = normalUnselected;   
        }
        g.setStroke(normalStroke);
        int src_x = this.src_x;
        int src_y = this.src_y;
        int dst_x = this.dst_x;
        int dst_y = this.dst_y;

        g.setColor(colour);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Draw the end arrow
        int delta_x = end_left ? -10 : 10;
        int[] xPoints = { dst_x, dst_x + delta_x, dst_x + delta_x };
        int[] yPoints = { dst_y, dst_y - 3, dst_y + 3 };

        g.drawLine(dst_x, dst_y, dst_x + delta_x, dst_y + 4);
        g.drawLine(dst_x, dst_y, dst_x + delta_x, dst_y - 4);
        g.setStroke(dashedStroke);

       // Draw the start
        int corner_y = src_y + (start_top ? -15 : 15);
        g.drawLine(src_x, corner_y, src_x, src_y);
        src_y = corner_y;

        // Draw the last line segment
        int corner_x = dst_x + (end_left ? -15 : 15);
        g.drawLine(corner_x, dst_y, dst_x, dst_y);
        dst_x = corner_x;

        // if arrow vertical corner, draw first segment up to corner
        if((src_y != dst_y) && (start_top == (src_y < dst_y))) {
            corner_x = ((src_x + dst_x) / 2) + (end_left ? 15 : -15);
            corner_x = (end_left ? Math.min(dst_x, corner_x) :
                        Math.max(dst_x, corner_x));
            g.drawLine(src_x, src_y, corner_x, src_y);
            src_x = corner_x;
        }

        // if arrow horiz. corner, draw first segment up to corner
        if((src_x != dst_x) && (end_left == (src_x > dst_x))) {
            corner_y = ((src_y + dst_y) / 2) + (start_top ? 15 : -15);
            corner_y = (start_top ? Math.min(src_y, corner_y) :
                        Math.max(src_y, corner_y));
            g.drawLine(dst_x, corner_y, dst_x, dst_y);
            dst_y = corner_y;
        }

        // draw the middle bit
        g.drawLine(src_x, src_y, src_x, dst_y);
        g.drawLine(src_x, dst_y, dst_x, dst_y);
    }

    public void draw(Graphics2D g)
    {
        draw(normalColour, g);
    }

    /**
     * Test whether (x,y) is in rectangle (x0,x1,y0,y1),
     */
    static final boolean inRect(int x, int y, int x0, int y0, int x1, int y1)
    {
        int xmin = Math.min(x0, x1);
        int xmax = Math.max(x0, x1);
        int ymin = Math.min(y0, y1);
        int ymax = Math.max(y0, y1);
        return (xmin <= x) && (ymin <= y) && (x < xmax) && (y < ymax);
    }

    public boolean contains(int x, int y)
    {
        int src_x = this.src_x;
        int src_y = this.src_y;
        int dst_x = this.dst_x;
        int dst_y = this.dst_y;

        // Check the first segment
        int corner_y = src_y + (start_top ? -15 : 15);
        if(inRect(x, y, src_x - SELECT_DIST, corner_y, src_x + SELECT_DIST, src_y))
            return true;

        src_y = corner_y;

        // Check the last line segment
        int corner_x = dst_x + (end_left ? -15 : 15);
        if(inRect(x, y, corner_x, dst_y - SELECT_DIST, dst_x, dst_y + SELECT_DIST))
            return true;

        dst_x = corner_x;

        // if arrow vertical corner, check first segment up to corner
        if((src_y != dst_y) && (start_top == (src_y < dst_y))) {
            corner_x = ((src_x + dst_x) / 2) + (end_left ? 15 : -15);
            corner_x = (end_left ? Math.min(dst_x, corner_x) :
                        Math.max(dst_x, corner_x));
            if(inRect(x, y, src_x, src_y - SELECT_DIST, corner_x, src_y + SELECT_DIST))
                return true;
            src_x = corner_x;
        }

        // if arrow horiz. corner, check first segment up to corner
        if((src_x != dst_x) && (end_left == (src_x > dst_x))) {
            corner_y = ((src_y + dst_y) / 2) + (start_top ? 15 : -15);
            corner_y = (start_top ? Math.min(src_y, corner_y) :
                        Math.max(src_y, corner_y));
            if(inRect(x, y, dst_x - SELECT_DIST, corner_y, dst_x + SELECT_DIST, dst_y))
                return true;
            dst_y = corner_y;
        }

        // Check the middle bit
        return inRect(x, y, src_x - SELECT_DIST, src_y, src_x + SELECT_DIST, dst_y)
            || inRect(x, y, src_x, dst_y - SELECT_DIST, dst_x, dst_y + SELECT_DIST);
    }

    public void highlight(Graphics2D g)
    {
        g.setXORMode(Color.red);
        draw(g);
        //        draw(normalColour, g);
        g.setPaintMode();
    }

    public void load(Properties props, String prefix)
    {
        super.load(props, prefix);

        // Nothing extra to do.
    }

    public void save(Properties props, String prefix)
    {
        super.save(props, prefix);

        // This may be overridden by decendents
        props.put(prefix + ".type", "UsesDependency");
    }

    public void setFlag(boolean value)
    {
        flag = value;
    }

    public boolean isFlagged()
    {
        return flag;
    }
    
    public void remove(){
        //TODO implement remove
        pkg.removeArrow(this);
    }
}
