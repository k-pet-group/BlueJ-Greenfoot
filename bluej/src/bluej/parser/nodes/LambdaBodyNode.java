/*
 This file is part of the BlueJ program. 
 Copyright (C) 2022  Michael Kolling and John Rosenberg 
 
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

import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeSolid;
import bluej.debugger.gentype.Reflective;
import bluej.parser.CompletionParser;
import bluej.parser.ExpressionTypeInfo;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.parser.nodes.ReparseableDocument.Element;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.Reader;

/**
 * A node representing the body of a lambda in an expression.
 * 
 * Note that the body itself may be an expression (e.g. x -> foo())
 * or a block (e.g. x -> {foo();}) but they will both have a LambdaBodyNode
 * which then has a child that is either an expression node or
 * a block node.
 */
public class LambdaBodyNode extends ExpressionNode
{
    public LambdaBodyNode(JavaParentNode parent)
    {
        super(parent);
    }

    @Override
    protected ExpressionTypeInfo getExpressionType(int pos, int nodePos, JavaEntity defaultType, ReparseableDocument document, ExpressionNode largestPlainExpressionNode)
    {
        // Crucial difference here compared to our parent class ExpressionNode is that
        // we pass null as the largestPlainExpressionNode, because if we need to do code completion
        // we want to constrain it to just this block, because the available names are different
        // in the lambda body (because they include the lambda parameters) to the larger expression
        // that contains the lambda.
        return super.getExpressionType(pos, nodePos, defaultType, document, null);
    }
}
