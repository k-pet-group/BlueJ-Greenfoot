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
 * @version $Id: UsesDependency.java 2488 2004-04-06 09:42:07Z fisker $
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
                                                              
                                                              
    private int sourceX, sourceY, destX, destY;
    private boolean startTop, endLeft;
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
        this.sourceX = src_x;
        this.sourceY = src_y;
        this.setStartTop(start_top);
    }

    public void setDestCoords(int dst_x, int dst_y, boolean end_left)
    {
        this.destX = dst_x;
        this.destY = dst_y;
        this.setEndLeft(end_left);
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
        int src_x = this.sourceX;
        int src_y = this.sourceY;
        int dst_x = this.destX;
        int dst_y = this.destY;

        // Check the first segment
        int corner_y = src_y + (isStartTop() ? -15 : 15);
        if(inRect(x, y, src_x - SELECT_DIST, corner_y, src_x + SELECT_DIST, src_y))
            return true;

        src_y = corner_y;

        // Check the last line segment
        int corner_x = dst_x + (isEndLeft() ? -15 : 15);
        if(inRect(x, y, corner_x, dst_y - SELECT_DIST, dst_x, dst_y + SELECT_DIST))
            return true;

        dst_x = corner_x;

        // if arrow vertical corner, check first segment up to corner
        if((src_y != dst_y) && (isStartTop() == (src_y < dst_y))) {
            corner_x = ((src_x + dst_x) / 2) + (isEndLeft() ? 15 : -15);
            corner_x = (isEndLeft() ? Math.min(dst_x, corner_x) :
                        Math.max(dst_x, corner_x));
            if(inRect(x, y, src_x, src_y - SELECT_DIST, corner_x, src_y + SELECT_DIST))
                return true;
            src_x = corner_x;
        }

        // if arrow horiz. corner, check first segment up to corner
        if((src_x != dst_x) && (isEndLeft() == (src_x > dst_x))) {
            corner_y = ((src_y + dst_y) / 2) + (isStartTop() ? 15 : -15);
            corner_y = (isStartTop() ? Math.min(src_y, corner_y) :
                        Math.max(src_y, corner_y));
            if(inRect(x, y, dst_x - SELECT_DIST, corner_y, dst_x + SELECT_DIST, dst_y))
                return true;
            dst_y = corner_y;
        }

        // Check the middle bit
        return inRect(x, y, src_x - SELECT_DIST, src_y, src_x + SELECT_DIST, dst_y)
            || inRect(x, y, src_x, dst_y - SELECT_DIST, dst_x, dst_y + SELECT_DIST);
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
        pkg.removeArrow(this);
    }
    /**
     * @return Returns the sourceX.
     */
    public int getSourceX() {
        return sourceX;
    }
    /**
     * @return Returns the sourceY.
     */
    public int getSourceY() {
        return sourceY;
    }
    /**
     * @param sourceY The sourceY to set.
     */
    public void setSourceY(int sourceY) {
        this.sourceY = sourceY;
    }
    /**
     * @return Returns the destX.
     */
    public int getDestX() {
        return destX;
    }
    /**
     * @return Returns the destY.
     */
    public int getDestY() {
        return destY;
    }

    public void setStartTop(boolean startTop) {
        this.startTop = startTop;
    }

    public boolean isStartTop() {
        return startTop;
    }

    public void setEndLeft(boolean endLeft) {
        this.endLeft = endLeft;
    }

    public boolean isEndLeft() {
        return endLeft;
    }
    
    public boolean isResizable()
    {
        return false;
    }
}
