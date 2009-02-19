/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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
package bluej.parser.ast;

import java.util.ArrayList;

import antlr.Token;
import antlr.collections.AST;

/**
 * An AST type that keeps track of line and columns, as well
 * as maintaining a list of "important" tokens, and a "hidden"
 * token. The hidden token is used to store the previous javadoc
 * comment, if any.
 */
public class LocatableAST extends antlr.CommonAST
{
    private int line;
    private int column;
    protected ArrayList importantTokens;
    
    protected LocatableToken hiddenBefore;
    
    private int endLine;
    private int endColumn;
    
    public LocatableAST()
    {
        super();
    }

    public LocatableAST(Token tok)
    {
        super(tok);
    }
    
    /**
     * Sets the line number of this node
     */
    public void setLine(int line)
    {
        this.line = line;
    }

    /**
     * Gets the line number of this node
     */
    public int getLine()
    {
        return line;
    }

  /**
   * sets the column where this node resides
   */
    public void setColumn(int column)
    {
        this.column = column;
    }
        
    /**
     * gets the column where this node resides
     */
    public int getColumn() {
        return column;
    }

    /**
     * Set the location where this AST ends. This is used to mark the end of a statement,
     * for example, so that a statement sequence string can be broken up into individual
     * statements.
     * 
     * @param endline  The line the AST ends
     * @param endcol   The column the AST ends
     */
    public void setEndPos(int endline, int endcol)
    {
        endLine = endline;
        endColumn = endcol;
    }
    
    /**
     * Get the line where this AST ends.
     * @return  the end line
     */
    public int getEndLine()
    {
        return endLine;
    }
    
    /**
     * Get the column where this AST ends.
     * @return  the end column
     */
    public int getEndColumn()
    {
        return endColumn;
    }
    
    /**
     * Get the length of this AST node selection. Only works if the node is on one line.
     * This is true for most nodes derived from a token.
     */
    public int getLength()
    {
        return endColumn - column;
    }
    
  /**
   * initialized this node with input node.
   */
    public void initialize(AST t)
    {
        super.initialize(t);
        
        LocatableAST tree = (LocatableAST)t;
        setLine(tree.getLine());
        setColumn(tree.getColumn());

        if (tree.importantTokens != null) {
            this.importantTokens = new ArrayList(tree.importantTokens);
        }
    }

  /**
   * initializes the node with input <code>Token</code>.
   */
    public void initialize(Token t)
    {
        LocatableToken lt = (LocatableToken) t;
        
        super.initialize(lt);

        setLine(t.getLine());
        setColumn(t.getColumn());
        hiddenBefore = (LocatableToken) lt.getHiddenBefore();
        
        setEndPos(t.getLine(), lt.getEndColumn());
    }

    public void addImportantToken(Token tok)
    {
        if (importantTokens == null)
            importantTokens = new ArrayList(5);

        importantTokens.add(tok);           
    }

    public LocatableToken getImportantToken(int id)
    {
        return (LocatableToken) importantTokens.get(id);    
    }

    public int getImportantTokenCount()
    {
        if (importantTokens == null)
            return 0;
        else
            return importantTokens.size();
    }
    
    public LocatableToken getHiddenBefore()
    {
        return hiddenBefore;
    }
    
}
