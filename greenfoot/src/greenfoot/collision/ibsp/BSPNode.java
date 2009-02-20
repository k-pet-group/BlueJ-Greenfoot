/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kšlling 
 
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
package greenfoot.collision.ibsp;

import greenfoot.Actor;

import java.util.*;

/**
 * A node in a BSP tree.
 * 
 * @author Davin McCall
 */
public final class BSPNode
{
    // private List<ActorNode> actorNodes;
    private Map<Actor, ActorNode> actors;
    
    private BSPNode parent;
    private Rect area;
    private int splitAxis;  // which axis is split
    private int splitPos;  // where it is split (absolute)
    private BSPNode left;
    private BSPNode right;
    
    private boolean areaRipple; // area has been set, need to ripple
        // down to children at some stage
    
    public BSPNode(Rect area, int splitAxis, int splitPos)
    {
        this.area = area;
        this.splitAxis = splitAxis;
        this.splitPos = splitPos;
        
        // actorNodes = new LinkedList<ActorNode>();
        actors = new HashMap<Actor, ActorNode>();
    }
    
    public void setChild(int side, BSPNode child)
    {
//        int oldDepth = getDepth();
        if (side == IBSPColChecker.PARENT_LEFT) {
            left = child;
            if (child != null) {
                child.parent = this;
                child.setArea(getLeftArea());
//                leftDepth = child.getDepth();
            }
            else {
//                leftDepth = 0;
            }
        }
        else {
            right = child;
            if (child != null) {
                child.parent = this;
                child.setArea(getRightArea());
//                rightDepth = child.getDepth();
            }
            else {
//                rightDepth = 0;
            }
        }
//        int depth = getDepth();
//        if (depth != oldDepth) {
//            if (parent != null) {
//                parent.updateDepth();
//            }
//        }
    }
    
//    private void updateDepth()
//    {
//        int oldDepth = getDepth();
//        leftDepth = left != null ? left.getDepth() : 0;
//        rightDepth = right != null ? right.getDepth() : 0;
//        int depth = getDepth();
//        if (depth != oldDepth) {
//            if (parent != null) {
//                parent.updateDepth();
//            }
//        }
//    }
    
//    public int getDepth()
//    {
//        return Math.max(leftDepth, rightDepth) + 1;
//    }
    
//    public int getLeftDepth()
//    {
//        return leftDepth;
//    }
    
//    public boolean needsRebalance()
//    {
//        return Math.abs(leftDepth - rightDepth) > IBSPColChecker.REBALANCE_THRESHOLD;
//    }
    
//    public int getRightDepth()
//    {
//        return rightDepth;
//    }
    
    public void setArea(Rect area)
    {
//        if (this.area != null) {
//            if (! area.contains(this.area)) {
//                System.out.println("Error, old area = " + this.area + ", new area = " + area);
//                throw new Error();
//            }
//        }
        this.area = area;
        areaRipple = true;
//        if (splitAxis == IBSPColChecker.Y_AXIS) {
//            if (splitPos < area.getY() || splitPos > area.getTop()) {
//                throw new Error();
//            }
//        }
//        else {
//            if (splitPos < area.getX() || splitPos > area.getRight()) {
//                throw new Error();
//            }
//        }
    }
    
    public void setSplitAxis(int axis)
    {
        if (axis != splitAxis) {
            splitAxis = axis;
            areaRipple = true;
        }
    }
    
    public void setSplitPos(int pos)
    {
        if (pos != splitPos) {
            splitPos = pos;
            areaRipple = true;
        }
    }

    public int getSplitAxis()
    {
        return splitAxis;
    }
    
    public int getSplitPos()
    {
        return splitPos;
    }

    public Rect getLeftArea()
    {
        if (splitAxis == IBSPColChecker.X_AXIS) {
            return new Rect(area.getX(), area.getY(), splitPos - area.getX(), area.getHeight());
        }
        else {
            return new Rect(area.getX(), area.getY(), area.getWidth(), splitPos - area.getY());
        }
    }
    
    public Rect getRightArea()
    {
        if (splitAxis == IBSPColChecker.X_AXIS) {
            return new Rect(splitPos, area.getY(), area.getRight() - splitPos, area.getHeight());
        }
        else {
            return new Rect(area.getX(), splitPos, area.getWidth(), area.getTop() - splitPos);
        }
    }
    
    public Rect getArea()
    {
        return area;
    }
    
    private void resizeChildren()
    {
        if (left != null) {
            left.setArea(getLeftArea());
        }
        if (right != null) {
            right.setArea(getRightArea());
        }
    }
    
    public BSPNode getLeft()
    {
        if (areaRipple) {
            resizeChildren();
            areaRipple = false;
        }
        return left;
    }
    
    public BSPNode getRight()
    {
        if (areaRipple) {
            resizeChildren();
            areaRipple = false;
        }
        return right;
    }
    
    public BSPNode getParent()
    {
        return parent;
    }
    
    public void setParent(BSPNode parent)
    {
        this.parent = parent;
    }
    
    public int getChildSide(BSPNode child)
    {
        if (left == child) {
            return IBSPColChecker.PARENT_LEFT;
        }
        else {
            return IBSPColChecker.PARENT_RIGHT;
        }
    }
    
    public String toString()
    {
        return "bsp" + hashCode();
    }
    
    public void addActor(Actor actor)
    {
        actors.put(actor, new ActorNode(actor, this));
    }
    
    public boolean containsActor(Actor actor)
    {
        // return actors.containsKey(actor);
        ActorNode anode = actors.get(actor);
        if (anode != null) {
            anode.mark();
            return true;
        }
        return false;
    }
    
    public void actorRemoved(Actor actor)
    {
        actors.remove(actor);
    }
    
//    public void removeActor(Actor actor)
//    {
//        ActorNode anode = actors.remove(actor);
//        if (anode != null) {
//            anode.removed();
//        }
//    }
    
    public int numberActors()
    {
        return actors.size();
    }
    
    public boolean isEmpty()
    {
        return actors.isEmpty();
    }
    
    public Iterator<Map.Entry<Actor, ActorNode>> getEntriesIterator()
    {
        return actors.entrySet().iterator();
    }
    
    public Iterator<Actor> getActorsIterator()
    {
        return actors.keySet().iterator();
    }
    
    public List<Actor> getActorsList()
    {
        return new ArrayList<Actor>(actors.keySet());
    }
    
    /**
     * Remove all actors from this node.
     */
//    public void clear()
//    {
//        Iterator<Map.Entry<Actor,ActorNode>> i = actors.entrySet().iterator();
//
//        while (i.hasNext()) {
//            i.next().getValue().removed();
//            i.remove();
//        }
//    }
}
