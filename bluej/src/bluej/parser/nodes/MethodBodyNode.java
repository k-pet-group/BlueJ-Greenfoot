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

import java.io.Reader;
import java.util.Stack;

import javax.swing.text.Document;

import bluej.parser.DocumentReader;
import bluej.parser.EditorParser;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.nodes.NodeTree.NodeAndPosition;

public class MethodBodyNode extends ParentParsedNode
{
    public MethodBodyNode(ParsedNode parent)
    {
        super(parent);
    }
    
    @Override
    protected void reparseNode(Document document, int nodePos, int offset, NodeStructureListener listener)
    {
        // Remove any child nodes straddling the offset
        NodeAndPosition nap = getNodeTree().findNodeAtOrBefore(offset);
        if (nap != null) {
            if (nap.getEnd() > offset) {
                nap.getNode().remove();
                listener.nodeRemoved(nap);
                offset = nap.getPosition(); // re-parse from where the removed child was
            }
            else {
                offset = nap.getEnd();
            }
        }
        else {
            offset = 0; // reparse from this node's beginning
        }
        
        int pline = document.getDefaultRootElement().getElementIndex(offset) + 1;
        int pcol = offset - document.getDefaultRootElement().getElement(pline - 1).getStartOffset() + 1;
        Reader r = new DocumentReader(document, nodePos + offset, nodePos + getSize());
        
        EditorParser parser = new EditorParser(r, pline, pcol, buildScopeStack());
        
        int ttype = parser.getTokenStream().LA(1).getType();
        while (ttype != JavaTokenTypes.RCURLY && ttype != JavaTokenTypes.EOF) {
            parser.parseStatement();
            
            // PROBLEM: the above may create a new node which overlaps subsequent child nodes.
            // Maybe insert a false duplicate of us in the scopestack, rather than insert ourselves
            // directly? Then we need to pull children from the duplicate afterwards.
            // But this breaks getOffsetFromParent() so we might have to duplicate the whole stack of
            // nodes... ugly.
            
            ttype = parser.getTokenStream().LA(1).getType();
        }
        
        // TODO: lots. If we have '}', check it matches the expected end of this node (otherwise, we
        // have been cut short). If we have EOF, we have been extended...
    }
    
    protected Stack<ParsedNode> buildScopeStack()
    {
        Stack<ParsedNode> r = new Stack<ParsedNode>();
        ParsedNode pn = this;
        do {
            r.add(0, pn);
            pn = pn.getParentNode();
        } while (pn != null);
        
        return r;
    }
}
