
package bluej.parser.symtab;

/*******************************************************************************
 * This is a PrintWriter that adds indentation at the beginning 
 *    of each line that is printed.
 *  It overloads all of the println() and print() methods to add
 *    this indentation
 *  It also provides two new methods: indent() and dedent() to
 *    increase and decrease the indentation (respectively)
 ******************************************************************************/
public class IndentingPrintWriter extends java.io.PrintWriter {
    //==========================================================================
    //==  Class Variables
    //==========================================================================
    
    /** The current amount of space to use as indentation */
    private String indentString = "";
    
    /** Have we written anything on the current line yet? */
    private boolean lineInProgress=false;


    //==========================================================================
    //==  Methods
    //==========================================================================
    

    /**
     * IndentingPrintWriter constructor comment.
     * @param out java.io.OutputStream
     */
    public IndentingPrintWriter(java.io.OutputStream out) {
        super(out);
    }


    /**
     * IndentingPrintWriter constructor comment.
     * @param out java.io.OutputStream
     * @param autoFlush boolean
     */
    public IndentingPrintWriter(java.io.OutputStream out, boolean autoFlush) {
        super(out, autoFlush);
    }


    /**
     * IndentingPrintWriter constructor comment.
     * @param out java.io.Writer
     */
    public IndentingPrintWriter(java.io.Writer out) {
        super(out);
    }


    /**
     * IndentingPrintWriter constructor comment.
     * @param out java.io.Writer
     * @param autoFlush boolean
     */
    public IndentingPrintWriter(java.io.Writer out, boolean autoFlush) {
        super(out, autoFlush);
    }


    public void dedent() {
        indentString = indentString.substring(2);
    }   


    public void indent() {
        indentString += "  ";
    }   


    /** Print an array of chracters. */
    public void print(char s[]) {
        if (!lineInProgress) {
            super.write(indentString);
            lineInProgress = true;
        }   
        super.print(s);
    }


    /** Print a character. */
    public void print(char c) {
        if (!lineInProgress) {
            super.write(indentString);
            lineInProgress = true;
        }   
        super.print(c);
    }


    /** Print a double. */
    public void print(double d) {
        if (!lineInProgress) {
            super.write(indentString);
            lineInProgress = true;
        }   
        super.print(d);
    }


    /** Print a float. */
    public void print(float f) {
        if (!lineInProgress) {
            super.write(indentString);
            lineInProgress = true;
        }   
        super.print(f);
    }


    /** Print an integer. */
    public void print(int i) {
        if (!lineInProgress) {
            super.write(indentString);
            lineInProgress = true;
        }   
        super.print(i);
    }


    /** Print a long. */
    public void print(long l) {
        if (!lineInProgress) {
            super.write(indentString);
            lineInProgress = true;
        }   
        super.print(l);
    }


    /** Print an object. */
    public void print(Object obj) {
        if (!lineInProgress) {
            super.write(indentString);
            lineInProgress = true;
        }   
        super.print(obj);
    }


    /** Print a String. */
    public void print(String s) {
        if (!lineInProgress) {
            super.write(indentString);
            lineInProgress = true;
        }   
        super.print(s);
    }


    /** Print a boolean. */
    public void print(boolean b) {
        if (!lineInProgress) {
            super.write(indentString);
            lineInProgress = true;
        }   
        super.print(b);
    }


    public void println() {
        if (!lineInProgress) {
            super.write(indentString);
            lineInProgress = true;
        }   
        super.println();
        lineInProgress = false;
    }


    public void println(char x[]) {
        if (!lineInProgress) {
            super.write(indentString);
            lineInProgress = true;
        }   
        super.println(x);
        lineInProgress = false;
    }


    /** Print a character, and then finish the line. */
    public void println(char x) {
        if (!lineInProgress) {
            super.write(indentString);
            lineInProgress = true;
        }   
        super.println(x);
        lineInProgress = false;
    }


    /** Print a double, and then finish the line. */
    public void println(double x) {
        if (!lineInProgress) {
            super.write(indentString);
            lineInProgress = true;
        }   
        super.println(x);
        lineInProgress = false;
    }


    /** Print a float, and then finish the line. */
    public void println(float x) {
        if (!lineInProgress) {
            super.write(indentString);
            lineInProgress = true;
        }   
        super.println(x);
        lineInProgress = false;
    }


    /** Print an integer, and then finish the line. */
    public void println(int x) {
        if (!lineInProgress) {
            super.write(indentString);
            lineInProgress = true;
        }   
        super.println(x);
        lineInProgress = false;
    }


    /** Print a long, and then finish the line. */
    public void println(long x) {
        if (!lineInProgress) {
            super.write(indentString);
            lineInProgress = true;
        }   
        super.println(x);
        lineInProgress = false;
    }


    /** Print an Object, and then finish the line. */
    public void println(Object x) {
        if (!lineInProgress) {
            super.write(indentString);
            lineInProgress = true;
        }   
        super.println(x);
        lineInProgress = false;
    }


    /** Print a String, and then finish the line. */
    public void println(String x) {
        if (!lineInProgress) {
            super.write(indentString);
            lineInProgress = true;
        }   
        super.println(x);
        lineInProgress = false;
    }


    /** Print a boolean, and then finish the line. */
    public void println(boolean x) {
        if (!lineInProgress) {
            super.write(indentString);
            lineInProgress = true;
        }   
        super.println(x);
        lineInProgress = false;
    }
}
