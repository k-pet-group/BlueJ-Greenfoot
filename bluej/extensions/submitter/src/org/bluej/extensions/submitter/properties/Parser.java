package org.bluej.extensions.submitter.properties;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import javax.swing.tree.DefaultTreeModel;

/**
 * Parses a given input stream, adding the information to the given node.
 * 
 * @author Clive Miller
 * @version $Id: Parser.java 1463 2002-10-23 12:40:32Z jckm $
 */
public class Parser
{
    private Tokenizer token;
    private final boolean project;
    private final DefaultTreeModel treeModel;
    
    public Parser (boolean project, DefaultTreeModel treeModel)
    {
        this.project = project;
        this.treeModel = treeModel;
    }
    
    public void parse (Node current, InputStream is) throws CompilationException
    {
        token = new Tokenizer (new InputStreamReader (is));
        parse (current);
    }
        
    public void parse (Node current, Reader reader) throws CompilationException
    {
        token = new Tokenizer (reader);
        parse (current);
    }
        
    public void parse (Node current) throws CompilationException
    {
        if (parseStatements (current) != Tokenizer.END) {
            throw new CompilationException ("} without {",token);
        }
    }
    
    private Tokenizer.Type parseStatements (Node current) throws CompilationException
    {
        Tokenizer.Type type;
        do {
            type = token.next();
            if (type == Tokenizer.END) break;
            if (type == Tokenizer.BLOCK_END) break;
            if (type == Tokenizer.BLOCK_START) {
                Node newNode = new Node (token.getTitle(), project);
                if (!token.getTitle().startsWith ("#")) {
                    treeModel.insertNodeInto (newNode, current, 0);
                }
                type = parseStatements (newNode);
                if (type != Tokenizer.BLOCK_END) {
                    throw new CompilationException ("} expected", token);
                }
            } else if (type == Tokenizer.EMPTY_SCHEME) {
                Node newNode = new Node (token.getTitle(), project);
                if (!token.getTitle().startsWith ("#")) {
                    treeModel.insertNodeInto (newNode, current, 0);
                }
            } else if (type == Tokenizer.CONFIG) {
                try {
                    current.addConfig (token.getKey(), token.getValue());
                } catch (IllegalArgumentException ex) {
                    throw new CompilationException (ex.getMessage(), token);
                }
            } else {
                throw new CompilationException ("Syntax error", token);
            }
        } while (true);
        return type;
    }
}
