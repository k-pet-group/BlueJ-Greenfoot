/*
Copyright (C) 2001  ThoughtWorks, Inc

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
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package bluej.parser.ast;

import java.io.File;
import java.util.*;

import antlr.collections.*;
import antlr.*;

/**
 * an extension of <code>antlr.CommonAST</code> that includes
 * extra information about the AST's location.  This information
 * is the file and line number where the AST was created.
 *
 * To use this AST node in your tree structure, assuming your
 * antlr.TreeParser is called parser, use
 *
 * parser.setASTNOdeCLass(SymTabAST.class.getName());
 *
 * make sure you also call setTokenObjectClass for the lexer as well
 *
 * @see SymTabToken
 */
public class LocatableAST extends antlr.CommonASTWithHiddenTokens
{
  private int line;
  private int column;
  protected ArrayList importantTokens;
  
  /**
   * sets the line where this node reside
   * @return <code>void</code>
   */
    public void setLine(int line)
    {
        this.line = line;
    }

  /**
   * gets the line where this node reside
   * @return <code>int</code>
   */
    public int getLine()
    {
        return line;
    }

  /**
   * sets the column where this node reside
   * @param column
   */
    public void setColumn(int column)
    {
        this.column = column;
    }

  /**
   * gets the column where this node reside
   * @return <code>int</code>
   */
    public int getColumn() {
        return column;
    }

    public LocatableAST()
    {
        super();
    }

    public LocatableAST(Token tok)
    {
        super(tok);
    }

  /**
   * initialized this node with input node
   * @param t
   * @return <code>void</code>
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
   * initializes the node with input <code>Token</code>
   * @param t
   * @return <code>void</code>
   */
    public void initialize(Token t)
    {
        super.initialize(t);

        setLine(t.getLine());
        setColumn(t.getColumn());

/*    if ( (getColumn() != 0) && (getText() != null) ) {
      setSpan( new Span( getLine(), getColumn(), getLine(),
                         getColumn() + getText().length() - 1 ) );
    } */
    
    }

    public void addImportantToken(Token tok)
    {
        if (importantTokens == null)
            importantTokens = new ArrayList(5);

        importantTokens.add(tok);           
    }

    public Token getImportantToken(int id)
    {
        return (Token) importantTokens.get(id);    
    }

    public int getImportantTokenCount()
    {
        if (importantTokens == null)
            return 0;
        else
            return importantTokens.size();
    }
    
}
