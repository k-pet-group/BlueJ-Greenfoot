package bluej.parser.symtab;

import java.util.Iterator;
import java.util.List;

/**
 * A scope for a type. This can save method comments in a ClassInfo structure.
 * 
 * @author Davin McCall
 * @version $Id$
 */
public class ClassScope extends Scope
{
    private ClassInfo info;
    
    public ClassScope(ClassInfo info, Scope parent)
    {
        super(parent);
        this.info = info;
    }
    
    public void addMethod(String name, String tpars, String retType, List paramTypes, List paramNames, String comment)
    {
        String target = "";
        String paramNamesString = null;
        
        // type parameters
        if (tpars != null)
            target = tpars + " ";
        
        if (retType != null)
            target += retType + " ";
        
        target += name + "(";
        if (paramTypes != null) {
            Iterator i = paramTypes.iterator();
            while (i.hasNext()) {
                target += i.next();
                if (i.hasNext()) {
                    target += ",";
                }
            }
        }
        target += ")";
        
        // parameter names
        if (paramNames != null && ! paramNames.isEmpty()) {
            Iterator i = paramNames.iterator();
            paramNamesString = i.next().toString();
            while (i.hasNext()) {
                paramNamesString += " " + i.next();
            }
        }
        
        info.addComment(target, comment, paramNamesString);
    }
}
