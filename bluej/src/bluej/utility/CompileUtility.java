package bluej.utility;

import bluej.Config;
import bluej.utility.Debug;
import java.io.*;
import java.util.*;

/**
 * A Compile utility class, initially for reading user specified
 * compiler options.
 *
 * @author  Bruce QUig
 * @version $Id: CompileUtility.java 1133 2002-02-21 05:05:29Z bquig $
 */
public class CompileUtility
{
    
    public static final String COMPILER_OPTIONS = "bluej.compiler.options";
    
    //========================= STATIC METHODS ============================
    
    /**
     * returns user specified compiler options from bluej defs.
     *
     * @return compiler options
     */
    public static List getUserCompilerOptions()
    {
        ArrayList options = new ArrayList();   
        String compilerOptions = Config.getPropString(COMPILER_OPTIONS, null);
        if(compilerOptions != null) {
            StringTokenizer st = new StringTokenizer(compilerOptions);
            while(st.hasMoreTokens()) 
                options.add(st.nextToken());
        }
        return options;
    }
    
}
