package org.bluej.extensions.submitter.properties;

/**
 * Catches Compilation errors.
 * 
 * @author Clive Miller 
 * @version $Id: CompilationException.java 1463 2002-10-23 12:40:32Z jckm $
 */
public class CompilationException extends Exception
{
    private Tokenizer token;
    private String filename;
    
    public CompilationException (String errorMessage, Tokenizer token)
    {
        super (errorMessage);
        this.token = token;
        filename = null;
    }
    
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        String line = token.getCurrentLine();
        if (filename != null) buf.append ("In file "+filename+" ");
        if (line == null) {
            buf.append ("Error at end of file: "+getMessage()+"\n");
        } else {
            buf.append ("Error in line "+token.getLineNumber()+": "
                                        +getMessage()+"\n"
                                        +line+"\n");
            for (int i=0,n=token.getLinePosition(); i<n; i++) {
                buf.append (' ');
            }
            buf.append ("^\n");
        }
        return buf.toString();
    }
    
    public String getSimpleString()
    {
        StringBuffer buf = new StringBuffer();
        String line = token.getCurrentLine();
        if (filename != null) buf.append ("In "+filename+" ");
        if (line == null) {
            buf.append ("Error at end of file: "+getMessage()+"\n");
        } else {
            buf.append ("Error in line "+token.getLineNumber()+": "
                                        +getMessage());
        }
        return buf.toString();
    }
    
    public void addFilename (String filename)
    {
        this.filename = filename;
    }
}
