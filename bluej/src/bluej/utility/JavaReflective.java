package bluej.utility;

import java.util.ArrayList;
import java.util.List;

import bluej.debugger.gentype.GenType;
import bluej.debugger.gentype.Reflective;

/*
 * 
 * @author Davin McCall
 */
public class JavaReflective extends Reflective {

    private Class c;
    
    public JavaReflective(Class c)
    {
        this.c = c;
    }
    
    public String getName()
    {
        return c.getName();
    }

    public List getTypeParams()
    {
        return JavaUtils.getJavaUtils().getTypeParams(c);
    }

    public List getSuperTypesR() {
        List l = new ArrayList();
        
        Class superclass = c.getSuperclass();
        if( superclass != null )
            l.add(new JavaReflective(superclass));

        Class [] interfaces = c.getInterfaces();
        for( int i = 0; i < interfaces.length; i++ ) {
            l.add(new JavaReflective(interfaces[i]));
        }
        return l;
    }

    public List getSuperTypes() {
        List l = new ArrayList();
        
        GenType superclass = JavaUtils.getJavaUtils().getSuperclass(c);
        if( superclass != null )
            l.add(superclass);
        
        GenType [] interfaces = JavaUtils.getJavaUtils().getInterfaces(c);
        for( int i = 0; i < interfaces.length; i++ ) {
            l.add(interfaces[i]);
        }
        return l;
    }

}
