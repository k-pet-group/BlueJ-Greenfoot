/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2013,2016  Michael Kolling and John Rosenberg 
 
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
package bluej.graph;

import bluej.pkgmgr.Package;
import bluej.pkgmgr.target.Target;
import javafx.scene.input.KeyCode;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A strategy to move graph selections with keyboard input.
 * 
 * @author fisker
 */
@OnThread(Tag.FXPlatform)
public class TraverseStrategyImpl
    implements TraverseStrategy
{

    private double calcDistance(Target vertex1, Target vertex2)
    {
        if (vertex1 == null || vertex2 == null) {
            return Double.POSITIVE_INFINITY;
        }
        int x1 = vertex1.getX() + vertex1.getWidth() / 2;
        int y1 = vertex1.getY() + vertex1.getHeight() / 2;
        int x2 = vertex2.getX() + vertex2.getWidth() / 2;
        int y2 = vertex2.getY() + vertex2.getHeight() / 2;
        double d = Math.sqrt(Math.pow((x2 - x1), 2) + Math.pow((y2 - y1), 2));
        return d;
    }

    /**
     * Given a currently selected vertex and a key press, decide which vertex 
     * should be selected next.
     * 
     * @param graph  The graph we're looking at.
     * @param currentVertex  The currently selected vertex.
     * @param key  The key that was pressed.
     * @return     A vertex that should be selected now.
     */
    public Target findNextVertex(Package graph, Target currentVertex, KeyCode key)
    {
        int currentVertexCenterX = currentVertex.getX() + currentVertex.getWidth() / 2;
        int currentVertexCenterY = currentVertex.getY() + currentVertex.getHeight() / 2;
        int x;
        int y;
        double closest = Double.POSITIVE_INFINITY;
        double currentDistance;
        Target closestVertex = null;
        boolean left, right, up, down, notSelf, inRightRegion;
        for (Target v : graph.getVertices()) {
            x = v.getX() + v.getWidth() / 2 - currentVertexCenterX;
            y = v.getY() + v.getHeight() / 2 - currentVertexCenterY;
            left = key == KeyCode.LEFT && y >= x && y <= -x;
            right = key == KeyCode.RIGHT && y <= x && y >= -x;
            up = key == KeyCode.UP && y <= x && y <= -x;
            down = key == KeyCode.DOWN && y >= x && y >= -x;
            notSelf = currentVertex != v;
            inRightRegion = (left || right || up || down) && notSelf;

            if (inRightRegion) {
                if (closestVertex == null) {
                    closestVertex = v;
                    closest = calcDistance(v, currentVertex);
                }
                if (closest > (currentDistance = calcDistance(v, currentVertex))) {
                    closest = currentDistance;
                    closestVertex = v;
                }

            }

        }
        if (closestVertex == null) {
            closestVertex = currentVertex;
        }
        return closestVertex;
    }
}