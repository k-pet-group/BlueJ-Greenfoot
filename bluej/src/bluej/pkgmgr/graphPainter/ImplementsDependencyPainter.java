/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010  Michael Kolling and John Rosenberg 
 
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
package bluej.pkgmgr.graphPainter;

import java.awt.*;
import java.awt.Graphics2D;

import bluej.pkgmgr.dependency.Dependency;
import bluej.pkgmgr.dependency.ImplementsDependency;

/**
 * Paintes ImplementsDependencies
 * 
 * @author fisker
 * @author Michael Kolling
 */
public class ImplementsDependencyPainter
    implements DependencyPainter
{
    protected static final float strokeWidthDefault = 1.0f;
    protected static final float strokeWidthSelected = 2.0f;

    static final Color normalColour = Color.BLACK;
    static final int ARROW_SIZE = 18; // pixels
    static final double ARROW_ANGLE = Math.PI / 6; // radians
    //static final int SELECT_DIST = 4;
    private static final float dash1[] = {5.0f, 2.0f};
    private static final BasicStroke dashedUnselected = new BasicStroke(strokeWidthDefault, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER, 10.0f, dash1, 0.0f);
    private static final BasicStroke dashedSelected = new BasicStroke(strokeWidthSelected, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER, 10.0f, dash1, 0.0f);
    private static final BasicStroke normalSelected = new BasicStroke(strokeWidthSelected);
    private static final BasicStroke normalUnselected = new BasicStroke(strokeWidthDefault);

    public ImplementsDependencyPainter()
    {
    }

    public void paint(Graphics2D g, Dependency dependency, boolean hasFocus)
    {
        if (!(dependency instanceof ImplementsDependency)) {
            throw new IllegalArgumentException();
        }
        Stroke oldStroke = g.getStroke();

        ImplementsDependency d = (ImplementsDependency) dependency;
        Stroke dashedStroke, normalStroke;
        boolean isSelected = d.isSelected() && hasFocus;
        if (isSelected) {
            dashedStroke = dashedSelected;
            normalStroke = normalSelected;
        }
        else {
            dashedStroke = dashedUnselected;
            normalStroke = normalUnselected;
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(normalColour);

        // Start from the centre of the src class
        Point pFrom = new Point(d.getFrom().getX() + d.getFrom().getWidth() / 2, d.getFrom().getY()
                + d.getFrom().getHeight() / 2);
        Point pTo = new Point(d.getTo().getX() + d.getTo().getWidth() / 2, d.getTo().getY()
                + d.getTo().getHeight() / 2);

        // Get the angle of the line from src to dst.
        double angle = Math.atan2(-(pFrom.getY() - pTo.getY()), pFrom.getX() - pTo.getX());

        // Get the dest point
        pFrom = d.getFrom().getAttachment(angle + Math.PI);
        pTo = d.getTo().getAttachment(angle);

        Point pArrow = new Point(pTo.x + (int) ((ARROW_SIZE - 2) * Math.cos(angle)), pTo.y
                - (int) ((ARROW_SIZE - 2) * Math.sin(angle)));

        // draw the arrow head
        int[] xPoints = {pTo.x, pTo.x + (int) ((ARROW_SIZE) * Math.cos(angle + ARROW_ANGLE)),
                pTo.x + (int) (ARROW_SIZE * Math.cos(angle - ARROW_ANGLE))};
        int[] yPoints = {pTo.y, pTo.y - (int) ((ARROW_SIZE) * Math.sin(angle + ARROW_ANGLE)),
                pTo.y - (int) (ARROW_SIZE * Math.sin(angle - ARROW_ANGLE))};

        g.setStroke(dashedStroke);
        g.drawLine(pFrom.x, pFrom.y, pArrow.x, pArrow.y);

        g.setStroke(normalStroke);
        g.drawPolygon(xPoints, yPoints, 3);

        g.setStroke(oldStroke);
    }

    /*
     * (non-Javadoc)
     * 
     * @see bluej.pkgmgr.graphPainter.DependencyPainter#getPopupMenuPosition(bluej.pkgmgr.dependency.Dependency)
     */
    public Point getPopupMenuPosition(Dependency dependency)
    {
        if (!(dependency instanceof ImplementsDependency)) {
            throw new IllegalArgumentException("Not a ExtendsDependency");
        }
        Point pFrom = new Point(dependency.getFrom().getX() + dependency.getFrom().getWidth() / 2, dependency.getFrom()
                .getY()
                + dependency.getFrom().getHeight() / 2);
        Point pTo = new Point(dependency.getTo().getX() + dependency.getTo().getWidth() / 2, dependency.getTo().getY()
                + dependency.getTo().getHeight() / 2);
        // Get the angle of the line from src to dst.
        double angle = Math.atan2(-(pFrom.y - pTo.y), pFrom.x - pTo.x);
        pTo = dependency.getTo().getAttachment(angle);

        //      draw the arrow head
        int[] xPoints = {pTo.x, pTo.x + (int) ((ARROW_SIZE) * Math.cos(angle + ARROW_ANGLE)),
                pTo.x + (int) (ARROW_SIZE * Math.cos(angle - ARROW_ANGLE))};
        int[] yPoints = {pTo.y, pTo.y - (int) ((ARROW_SIZE) * Math.sin(angle + ARROW_ANGLE)),
                pTo.y - (int) (ARROW_SIZE * Math.sin(angle - ARROW_ANGLE))};

        return new Point((xPoints[0] + xPoints[1] + xPoints[2]) / 3, (yPoints[0] + yPoints[1] + yPoints[2]) / 3);
    }
}