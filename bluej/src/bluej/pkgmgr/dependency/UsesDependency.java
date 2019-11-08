/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012,2015,2016,2017,2019  Michael Kolling and John Rosenberg
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.pkgmgr.dependency;

import bluej.pkgmgr.Package;
import bluej.pkgmgr.target.DependentTarget;
import bluej.utility.Utility;
import javafx.geometry.Point2D;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.Properties;

/**
 * A dependency between two targets in a package
 *
 * @author  Michael Kolling
 */
@OnThread(Tag.FXPlatform)
public class UsesDependency extends Dependency
{
    // All are rounded to the nearest integer + 0.5 boundary
    // to make the lines sharp;
    private double sourceX, sourceY, destX, destY;
    private boolean startTop, endLeft;

    @OnThread(Tag.Any)
    public UsesDependency(Package pkg, DependentTarget from, DependentTarget to)
    {
        super(pkg, from, to);
    }

    @OnThread(Tag.Any)
    public UsesDependency(Package pkg)
    {
        this(pkg, (DependentTarget)null, null);
    }

    @OnThread(Tag.Any)
    public UsesDependency(Package pkg, Properties props, String prefix) throws DependencyNotFoundException
    {
        super(pkg, props, prefix);
    }

    public void setSourceCoords(double src_x, double src_y, boolean start_top)
    {
        this.sourceX = Utility.roundHalf(src_x);
        this.sourceY = Utility.roundHalf(src_y);
        this.setStartTop(start_top);
    }

    public void setDestCoords(double dst_x, double dst_y, boolean end_left)
    {
        this.destX = Utility.roundHalf(dst_x);
        this.destY = Utility.roundHalf(dst_y);
        this.setEndLeft(end_left);
    }

    /**
     * Test whether (x,y) is in rectangle (x0,x1,y0,y1),
     */
    static final boolean inRect(double x, double y, double x0, double y0, double x1, double y1)
    {
        double xmin = Math.min(x0, x1);
        double xmax = Math.max(x0, x1);
        double ymin = Math.min(y0, y1);
        double ymax = Math.max(y0, y1);
        return (xmin <= x) && (ymin <= y) && (x < xmax) && (y < ymax);
    }

    public boolean contains(int x, int y)
    {
        double src_x = this.sourceX;
        double src_y = this.sourceY;
        double dst_x = this.destX;
        double dst_y = this.destY;

        // Check the first segment
        double corner_y = src_y + (isStartTop() ? -15 : 15);
        if(inRect(x, y, src_x - SELECT_DIST, corner_y, src_x + SELECT_DIST, src_y))
            return true;

        src_y = corner_y;

        // Check the last line segment
        double corner_x = dst_x + (isEndLeft() ? -15 : 15);
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

    
    /**
     * Compute line information (start point, end point, angle)
     * for the current state of this dependency.
     */
    public Line computeLine()
    {
        return new Line(new Point2D(sourceX, sourceY), new Point2D(destX, destY), 0.0);
    }
    
    @OnThread(Tag.FXPlatform)
    public void save(Properties props, String prefix)
    {
        super.save(props, prefix);

        // This may be overridden by decendents
        props.put(prefix + ".type", "UsesDependency");
    }

    public void remove()
    {
        pkg.removeArrow(this);
    }
    
    /**
     * @return Returns the sourceX.
     */
    public double getSourceX() {
        return sourceX;
    }
    /**
     * @return Returns the sourceY.
     */
    public double getSourceY() {
        return sourceY;
    }

    /**
     * @return Returns the destX.
     */
    public double getDestX() {
        return destX;
    }
    /**
     * @return Returns the destY.
     */
    public double getDestY() {
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

    @Override
    @OnThread(Tag.Any)
    public Type getType()
    {
        return Type.USES;
    }

    @Override
    public boolean isRemovable()
    {
        return false;
    }
}
