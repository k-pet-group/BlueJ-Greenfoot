package org.bluej.extensions.submitter.properties;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import javax.swing.tree.DefaultTreeModel;
import org.bluej.extensions.submitter.Stat;
/**
 * Parses a given input stream, adding the information to the given node.
 * 
 * @version $Id: Parser.java 1612 2003-01-27 09:51:28Z damiano $
 */
public class Parser
{
    private Tokenizer token;
    private final boolean project;
    private final DefaultTreeModel treeModel;
    private Stat stat;
    
    public Parser (Stat i_stat, boolean isProject, DefaultTreeModel treeModel)
    {
        stat = i_stat;
        this.project = isProject;
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
        stat.aDbg.debug(Stat.SVC_PARSER,"Parser.parse: current="+current.toString());
        
        if (parseStatements (current) != Tokenizer.END) {
            throw new CompilationException ("} without {",token);
        }
    }
    
    private Tokenizer.Type parseStatements (Node current) throws CompilationException
    {
        Tokenizer.Type type;
        for (;;) {
            type = token.next();

            if (type == Tokenizer.END) break;

            if (type == Tokenizer.BLOCK_END) break;

            if (type == Tokenizer.BLOCK_START) {
                stat.aDbg.debug(Stat.SVC_PARSER,"parseStatements: new Node, title="+token.getTitle());
                Node newNode = new Node (stat, token.getTitle(), project);
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
                Node newNode = new Node (stat, token.getTitle(), project);
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
