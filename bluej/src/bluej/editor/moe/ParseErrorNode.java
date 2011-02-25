/*
 This file is part of the BlueJ program. 
 Copyright (C) 2011  Michael Kolling and John Rosenberg 

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
package bluej.editor.moe;

import bluej.parser.nodes.RBTreeNode;

/**
 * A node in a tree representing a parse error highlight and its position.
 * 
 * @author Davin McCall
 */
public class ParseErrorNode extends RBTreeNode<ParseErrorNode>
{
    private Object highlightTag;
    private String errCode;
    
    /**
     * Construct a ParseErrorNode for the given highlight tag and the given error code / message.
     */
    public ParseErrorNode(Object highlightTag, String errCode)
    {
        this.highlightTag = highlightTag;
        this.errCode = errCode;
    }
    
    /**
     * Get the highlight tag for this parse error node.
     */
    public Object getHighlightTag()
    {
        return highlightTag;
    }
    
    /**
     * Get the error code for this parse error node.
     */
    public String getErrCode()
    {
        return errCode;
    }
}
