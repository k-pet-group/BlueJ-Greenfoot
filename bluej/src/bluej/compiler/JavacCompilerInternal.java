package bluej.compiler;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.lang.reflect.*;

import bluej.prefmgr.PrefMgr;
import bluej.utility.*;

/**
 * An implementation for the BlueJ "Compiler"
 * class providing an interface to Sun's javac
 * compiler by executing the com.sun.tools methods directly.
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @author  Andrew Patterson
 * @author  Bruce Quig
 * @version $Id: JavacCompilerInternal.java 1502 2002-11-12 01:51:57Z ajp $
 */
public class JavacCompilerInternal extends Compiler
{
    private ErrorStream firstStream = null;

	private String destdir;
	private String classpath;
	private boolean debug;
	private boolean deprecation;

	public JavacCompilerInternal()
	{
		setDebug(true);
	}

	public void setDestDir(String destdir)
	{
		this.destdir = destdir;
	}

	public void setClassPath(String classpath)
	{
		this.classpath = classpath;
	}

	public void setDebug(boolean debug)
	{
		this.debug = debug;
	}

	public void setDeprecation(boolean deprecation)
	{
		this.deprecation = deprecation;
	}

	public boolean compile(String[] sources, CompileObserver watcher)
	{
		List args = new ArrayList();

		if(destdir != null) {
			args.add("-d");
			args.add(destdir);
		}

		if(classpath != null) {
			args.add("-classpath");
			args.add(classpath);
		}

		if(debug)
			args.add("-g");

		//if(deprecation)
        // always use -deprecation option
		args.add("-deprecation");

        if(! System.getProperty("java.vm.version").startsWith("1.3"))
            if(PrefMgr.getFlag(PrefMgr.ENABLE_JDK14)) {
                args.add("-source");
                args.add("1.4");
            }

        /** Not used at present...
        // add user specified compiler options
        List userOptions = CompileUtility.getUserCompilerOptions();
        if(userOptions != null && userOptions.size() > 0) {
            Iterator it = userOptions.iterator();
            while(it.hasNext()) {
                args.add((String)it.next());
            }
        }
        */
                
        for(int i = 0; i < sources.length; i++)
            args.add(sources[i]);

        int length = args.size();
        String[] params = new String[length];
        args.toArray(params);

        Class compiler = null;
        Method compileMethod = null;

        /* problem number one is that between 1.3 and 1.4 the compile
           method changed from an instance method to a static method.
           use reflection to fix this.
           based on an idea from the JDEE code by jslopez@alum.mit.edu */

        try {
            compiler = Class.forName("com.sun.tools.javac.Main");

            if (compiler == null)
                return false;

            Class[] p = new Class[] {String[].class};

            compileMethod = compiler.getMethod("compile", p);

        } catch (ClassNotFoundException e) {
            Debug.message("com.sun.tools.javac.Main compiler is not available");
            return false;
        } catch (NoSuchMethodException e) {
            Debug.message("com.sun.tools.javac.Main compile method could not be found");
            return false;
        }

        if (compileMethod == null)
            return false;

        PrintStream systemErr = System.err;
        ErrorStream output = new ErrorStream();

        /* second problem with the jdk1.4 beta. It seems to use a
           PrintWriter wrapped around the system error stream. It also
           seems to cache the creation of this, so on subsequent compiles,
           the original output stream is still being used. To cope with
           both 1.3 and 1.4, we check the results of both the first stream
           and the newly created stream */

        if (firstStream == null)
            firstStream = output;

        System.setErr(output);      // redirect errors to our stream

        int result = 1;
        try {
            Object objResult;
            Object[] arguments = new Object[] { params };
            objResult = compileMethod.invoke(compiler.newInstance(), arguments);

            result = ((Integer) objResult).intValue();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        // only one of 'output' or 'firstStream' should be receiving output so
        // we will only generate one error message

		if (output.hasError()) {
			watcher.errorMessage(output.getFilename(),
						output.getLineNo(),
						output.getMessage(),true);
		}

        if (firstStream != null && firstStream != output && firstStream.hasError()) {
			watcher.errorMessage(firstStream.getFilename(),
						firstStream.getLineNo(),
						firstStream.getMessage(),true);
		}

        System.setErr(systemErr);   // restore

        // in case we are reusing the first stream, reset the hasError boolean
        if (firstStream != null)
            firstStream.reset();

		return result==0;
	}
}

/**
 * An OutputStream that parses javac output.
 *
 * @author  Michael Cahill
 */
class ErrorStream extends PrintStream
{
    private boolean haserror = false, hasfollowup = false;
    private int ignoreCount = 0;    // when > 0, indicates number of lines to ignore

    private String filename, message;
    private int lineno;

    public ErrorStream()
    {
        // we do not actually intend to use an actual OutputStream from
        // within this class yet our superclass requires us to pass a
        // non-null OutputStream
        // we pass it the system error stream
        super(System.err);
    }

    public void reset()
    {
        haserror = false;
        hasfollowup = false;
        ignoreCount = 0;
    }

    public boolean hasError()
    {
        return haserror;
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

        Debug.message("Compiler message: " + msg);

        int first_colon = msg.indexOf(':', 0);
        if(first_colon == -1) {
            // no colon may mean we are processing the end of compile msgs
            // of the form
            // x warning(s)

            if (msg.trim().endsWith("warnings") || msg.trim().endsWith("warning"))
                return;

            // otherwise, cannot read format of error message
            DialogManager.showErrorWithText(null, "compiler-error", msg);
            return;
        }

        filename = msg.substring(0, first_colon);

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
            // System.out.println(lineno + " " + message.substring(8));
            ignoreCount = 2;           
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
