package bluej.compiler;

import bluej.classmgr.BPClassLoader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import bluej.Config;

/**
 * Compiler class - an abstract interface to a source -> bytecode compiler. This
 * can be implemented by different compiler implementations.
 * 
 * Currently known implementations: JavacCompiler, JikesCompiler.
 * 
 * @author Michael Cahill
 * @author Michael Kolling
 * @author Poul Henriksen
 * @version $Id: Compiler.java 4599 2006-09-07 02:40:51Z davmac $
 */
abstract class Compiler
{
    public static final String COMPILER_OPTIONS = "bluej.compiler.options";

    private File destDir;
    private BPClassLoader bpClassLoader;
    private boolean debug;
    private boolean deprecation;

    public void setDestDir(File destDir)
    {
        this.destDir = destDir;
    }

    public void setProjectClassLoader(BPClassLoader bpClassLoader)
    {
        this.bpClassLoader = bpClassLoader;
    }

    public void setDebug(boolean debug)
    {
        this.debug = debug;
    }

    public void setDeprecation(boolean deprecation)
    {
        this.deprecation = deprecation;
    }

    public BPClassLoader getProjectClassLoader()
    {
        return bpClassLoader;
    }

    public boolean isDebug()
    {
        return debug;
    }

    public boolean isDeprecation()
    {
        return deprecation;
    }

    public File getDestDir()
    {
        return destDir;
    }

    /**
     * Creates a list of the options that should be used for the compilation.
     * 
     * @return A list of compile options.
     */
    protected List getCompileOptions()
    {
        List args = new ArrayList();

        if (getDestDir() != null) {
            args.add("-d");
            args.add(getDestDir().getPath());
        }

        if (getProjectClassLoader() != null) {
            args.add("-classpath");
            args.add(getProjectClassLoader().getClassPathAsString());
        }

        if (isDebug())
            args.add("-g");

        if (isDeprecation())
            args.add("-deprecation");
            
        String majorVersion = System.getProperty("java.specification.version");        
        args.add("-source");
        args.add(majorVersion);

        if (Config.systemContainsKey(COMPILER_OPTIONS)) {
            addUserSpecifiedOptions(args);
        }

        return args;
    }

    private void addUserSpecifiedOptions(List args)
    {
        String compilerOptions = Config.getPropString(COMPILER_OPTIONS, null);
        if (compilerOptions != null) {
            StringTokenizer st = new StringTokenizer(compilerOptions);
            while (st.hasMoreTokens()) {
                args.add(st.nextToken());
            }
        }
    }

    //  ========================= ABSTRACT METHODS ==========================

    /**
     * Compile some source files.
     * 
     * @param sources
     *            The files to compile
     * @param observer
     *            The compilation observer
     * @param internal
     *            True if compiling BlueJ-generated code (shell files) False if
     *            compiling user code
     * @return
     */
    public abstract boolean compile(File[] sources, CompileObserver observer, boolean internal);

}
