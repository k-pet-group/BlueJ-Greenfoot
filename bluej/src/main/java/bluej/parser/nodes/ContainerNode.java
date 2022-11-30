/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.parser.nodes;

/**
 * A basic container node implementation. A container node contains some sort of inner
 * scope.
 * 
 * @author Davin McCall
 */
public class ContainerNode extends JavaParentNode
{
    private int nodeType;
    
    public ContainerNode(JavaParentNode parent, int nodeType)
    {
        super(parent);
        this.nodeType = nodeType;
    }
    
    @Override
    public int getNodeType()
    {
        return nodeType;
    }
    
    @Override
    public boolean isContainer()
    {
        return true;
    }
    
    @Override
    protected boolean marksOwnEnd()
    {
        return true;
    }
}
