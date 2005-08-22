package bluej.parser.ast;

import java.util.*;

import antlr.collections.*;
import antlr.*;

/**
 * An AST type that keeps track of line and columns, as well
 * as maintaining a list of "important" tokens.
 */
public class LocatableAST extends antlr.CommonASTWithHiddenTokens
{
    private int line;
    private int column;
    protected ArrayList importantTokens;
    
    private int endLine;
    private int endColumn;
    
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
