package org.bluej.extensions.submitter.properties;

import java.io.InputStream;
import java.io.InputStreamReader;
import javax.swing.tree.DefaultTreeModel;
import org.bluej.extensions.submitter.Stat;

/**
 * Parses a given input stream, adding the information to the given node.
 * 
 * @version $Id: ConfParser.java 1708 2003-03-19 09:39:47Z damiano $
 */
public class ConfParser
{
    private Tokenizer token;
    private DefaultTreeModel treeModel;
    private Stat stat;
    
    public ConfParser (Stat i_stat, DefaultTreeModel treeModel)
    {
        stat = i_stat;
        this.treeModel = treeModel;
    }

    /**
     * When you parse you give where you want to insert and where you get it from.
     */
    public void parse (TreeNode current, InputStream is) throws CompilationException
    {
        stat.aDbg.debug(Stat.SVC_PARSER,"Parser.parse: current="+current.toString());

        token = new Tokenizer (new InputStreamReader (is));
        
        if (parseStatements (current) != Tokenizer.END) {
            throw new CompilationException ("} without {",token);
        }
    }


    private Tokenizer.Type parseStatements (TreeNode current) throws CompilationException
    {
        Tokenizer.Type type;
        for (;;) {
            type = token.next();

            if (type == Tokenizer.END) break;

            if (type == Tokenizer.BLOCK_END) break;

            if (type == Tokenizer.BLOCK_START) {
                stat.aDbg.debug(Stat.SVC_PARSER,"parseStatements: new Node, title="+token.getTitle());
                TreeNode newNode = new TreeNode (stat, token.getTitle());
                if (!token.getTitle().startsWith ("#")) {
                    treeModel.insertNodeInto (newNode, current, 0);
                }
                type = parseStatements (newNode);
                if (type != Tokenizer.BLOCK_END) {
                    throw new CompilationException ("} expected", token);
                }
                continue;
            } 

            if (type == Tokenizer.EMPTY_SCHEME) {
                TreeNode newNode = new TreeNode (stat, token.getTitle());
                if (!token.getTitle().startsWith ("#")) {
                    treeModel.insertNodeInto (newNode, current, 0);
                }
                continue;
            } 

            if (type == Tokenizer.CONFIG) {
                try {
                    current.addConfig (token.getKey(), token.getValue());
                } catch (IllegalArgumentException ex) {
                    throw new CompilationException (ex.getMessage(), token);
                }
                continue;
            } 

            throw new CompilationException ("Syntax error", token);
        }
        return type;
    }
}
