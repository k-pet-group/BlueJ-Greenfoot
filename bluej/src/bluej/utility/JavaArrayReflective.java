package bluej.utility;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import bluej.debugger.gentype.Reflective;

/*
 * A reflective for generic arrays (GenericArrayType). "Normal" arrays
 * are a Class, so they can use JavaReflective instead, but so called
 * generic arrays have no class type.
 * 
 * This is a "java 1.5-only" class.
 * 
 * @author Davin McCall
 * @version $Id: JavaArrayReflective.java 2617 2004-06-17 01:07:36Z davmac $
 */
public class JavaArrayReflective extends Reflective {

    Type rtype;
    
    public JavaArrayReflective(Type t)
    {
        rtype = t;
    }
    
    public String getName()
    {
        return "";
    }

    // TODO. Are these correct? What are the supertypes of a generic array?
    // Should we infer supertypes based on the supertypes of the component
    // type?
    
    public List getTypeParams()
    {
        return Collections.EMPTY_LIST;
    }

    public List getSuperTypesR()
    {
        return Collections.EMPTY_LIST;
    }

    public List getSuperTypes()
    {
        return Collections.EMPTY_LIST;
    }

}
