package bluej.debugger.gentype;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.Reflective;

/**
 * A version of Reflective which can be easily customised to suit the needs
 * of a test.
 * 
 * @author Davin McCall
 * @version $Id: TestReflective.java 3077 2004-11-09 04:33:53Z davmac $
 */
public class TestReflective extends Reflective
{
    public String name;
    public List typeParams;
    public List superTypes; // list of GenTypeClass
    
    public TestReflective(String name)
    {
        this.name = name;
        typeParams = new ArrayList();
        superTypes = new ArrayList();
    }
    
    public String getName()
    {
        return name;
    }
    
    public List getTypeParams()
    {
        return typeParams;
    }
    
    public List getSuperTypesR()
    {
        List n = new ArrayList();
        Iterator i = superTypes.iterator();
        while (i.hasNext()) {
            n.add(((GenTypeClass)i.next()).getReflective());
        }
        return n;
    }
    
    public List getSuperTypes()
    {
        return superTypes;
    }
    
    public Reflective getArrayOf()
    {
        return null;
    }
    
    public boolean isAssignableFrom(Reflective r)
    {
        return false;
    }
}
