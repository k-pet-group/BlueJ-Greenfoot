package bluej.compiler;

import java.io.PrintStream;

import bluej.utility.DialogManager;

/**
 * An OutputStream that parses javac output.
 *
 * @author  Michael Cahill
 */
class JavacErrorStream extends PrintStream
{
    private boolean haserror = false, hasfollowup = false, hasWarnings = false;
    private int ignoreCount = 0;    // when > 0, indicates number of lines to ignore

    private String filename, message;
    private String warning = "";
    private int lineno;
    
    private boolean internal;

    public JavacErrorStream(boolean internal)
    {
        // we do not actually intend to use an actual OutputStream from
        // within this class yet our superclass requires us to pass a
        // non-null OutputStream
        // we pass it the system error stream
        super(System.err);
        this.internal = internal;
    }

    public void reset()
    {
        haserror = false;
        hasfollowup = false;
        hasWarnings = false;
        ignoreCount = 0;
        warning = "";
    }

    public boolean hasError()
    {
        return haserror;
    }
    
    public boolean hasWarnings()
    {
        return hasWarnings;
    }

    public String getFilename()
    {
        return filename;
    }

    public int getLineNo()
    {
        return lineno;
    }

    public String getMessage()
    {
        return message;
    }
    
    public String getWarning()
    {
        return warning;
    }
    

    /**
     * Note: this class "cheats" by assuming that all output will be written by
     * a call to print or println. It happens that this is true for the
     * current version of javac but this could change in the future.
     *
     * We assume a certain error message format here:
     *   filename:line-number:message
     *
     * Some examples

o:\bj122\examples\appletdemo\Uncompile.java:19: cannot resolve symbol
symbol  : variable xxx
location: class Uncompile
                xxx = 0;
                ^
o:\bj122\examples\appletdemo\Uncompile.java:31: warning: getenv(java.lang.String) in java.lang.System has been deprecated
                System.getenv("aa");
                      ^
     *                      
     * We find the components by searching for the colons. Careful: MS Windows
     * systems might have a colon in the file name (if it is an absolute path
     * with a drive name included). In that case we have to ignore the first
     * colon.
     */
    public void print(String msg)
    {
        if (haserror)
            return;
            
        if (ignoreCount > 0) {
            ignoreCount--;
            return;
        }

        // there are some error messages that give important information in the
        // following lines. Try to munge it into a better message by utilising the
        // second/third line of the error
        if (hasfollowup) {
            int colonPoint = 9;
            String label = msg.substring(0, colonPoint);
            String info = msg.substring(colonPoint).trim();
            
            if(label.equals("found   :")) {             // incompatible types
                message += " - found " + info;
            } else if (label.equals("required:")) {
                message += " but expected " + info;
                haserror = true;  
            } else if (label.equals("symbol  :")) {     // unresolved symbol
                message += " - " + info;                             
                haserror = true;  
            }
            else                        // if not what we were expecting, bail out
                haserror = true;  
            
            return;          
        }

        //Debug.message("Compiler message: " + msg);

        int first_colon = msg.indexOf(':', 0);
        if(first_colon == -1) {
            // no colon may mean we are processing the end of compile msgs
            // of the form
            // x warning(s)

            if (msg.trim().endsWith("warnings") || msg.trim().endsWith("warning")) {
                warning += msg;
                hasWarnings = true;
                return;
            }
            
            // otherwise, cannot read format of error message
            DialogManager.showErrorWithText(null, "compiler-error", msg);
            return;
        }

        filename = msg.substring(0, first_colon);

        // "unchecked" warnings for generics begin with "Note:"
        if(filename.equals("Note")) {
            if(internal)
                return;
            warning += msg;
            hasWarnings = true;
            return;
        }
        
        // Windows might have a colon after drive name. If so, ignore it
        if(! filename.endsWith(".java")) {
            first_colon = msg.indexOf(':', first_colon + 1);
            if(first_colon == -1) {
                // cannot read format of error message
                DialogManager.showErrorWithText(null, "compiler-error", msg);
                return;
            }
            filename = msg.substring(0, first_colon);
        }
        int second_colon = msg.indexOf(':', first_colon + 1);
        if(second_colon == -1) {
            // cannot read format of error message
            DialogManager.showErrorWithText(null, "compiler-error", msg);
            return;
        }

        lineno = 0;
        try {
            lineno = Integer.parseInt(msg.substring(first_colon + 1, second_colon));
        } catch(NumberFormatException e) {
            // ignore it
        }

        message = msg.substring(second_colon + 1).trim();

        if (message.startsWith("warning:")) {
            // record the warnings and display them to users!!!
            warning += msg;
            ignoreCount = 2;
            // This type of warning generates an additional two lines:
            // one is a duplicate of the source line, the next is empty
            // other than a single caret (^) indicating the position in the line.
            if(message.startsWith("warning: [unchecked] unchecked cast"))
                ignoreCount = 4;
            if(!hasWarnings)
                hasWarnings = true;           
            return;
        }

        if (message.equals("cannot resolve symbol") || message.equals("incompatible types"))
            hasfollowup = true;
        else
            haserror = true;
    }

    /**
     * Map println to print - we are not interested in the line break anyway.
     */
    public void println(String msg)
    {
        print(msg);
    }

    /**
     * JDK 1.4 seems to use write rather than print
     */
    public void write(byte[] buf, int off, int len)
    {
        print(new String(buf, off, len));
    }
}
