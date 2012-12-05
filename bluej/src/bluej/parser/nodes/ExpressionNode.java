/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2012  Michael Kolling and John Rosenberg 
 
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

import javax.swing.text.Document;
import javax.swing.text.Element;

import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeSolid;
import bluej.parser.CodeSuggestions;
import bluej.parser.CompletionParser;
import bluej.parser.DocumentReader;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.nodes.NodeTree.NodeAndPosition;

/**
 * A node representing a parsed expression.
 * 
 * @author Davin McCall
 */
public class ExpressionNode extends JavaParentNode
{
    public ExpressionNode(JavaParentNode parent)
    {
        super(parent);
    }
    
    @Override
    public int getNodeType()
    {
        return NODETYPE_EXPRESSION;
    }
    
    @Override
    protected boolean marksOwnEnd()
    {
        return false;
    }
    
    @Override
    protected CodeSuggestions getExpressionType(int pos, int nodePos, JavaEntity defaultType, Document document)
    {
        valueEntityCache.clear();
        pocEntityCache.clear();
        
        NodeAndPosition<ParsedNode> nap = findNodeAt(pos, nodePos);
        if (nap != null && nap.getNode().getNodeType() == ParsedNode.NODETYPE_TYPEDEF) {
            return nap.getNode().getExpressionType(pos, nap.getPosition(), defaultType, document);
        }
        return suggestAsExpression(pos, nodePos, this, defaultType, document);
    }
    
    public static CodeSuggestions suggestAsExpression(int pos, int nodePos, EntityResolver resolver,
            JavaEntity defaultType, Document document)
    {
        Reader r = new DocumentReader(document, nodePos, pos);
        Element map = document.getDefaultRootElement();
        int line = map.getElementIndex(nodePos) + 1;
        int col = nodePos - map.getElement(line - 1).getStartOffset() + 1;
        
        CompletionParser parser = new CompletionParser(resolver, r, defaultType, line, col, nodePos);
        parser.parseExpression();
        
        GenTypeSolid stype = parser.getSuggestionType();
        GenTypeClass atype = (defaultType != null) ? defaultType.getType().asClass() : null;
        if (stype != null) {
            return new CodeSuggestions(stype, atype, parser.getSuggestionToken(), parser.isSuggestionStatic());
        }
        else {
            return null;
        }
    }
    
}
