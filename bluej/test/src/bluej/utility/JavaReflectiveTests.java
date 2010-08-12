package bluej.utility;

import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.MethodReflective;

public class JavaReflectiveTests extends TestCase
{
    @Override
    protected void setUp()
    {
        // nothing to do
    }
    
    @Override
    protected void tearDown()
    {
        // nothing to do
    }

    public void testGetMethods()
    {
        JavaReflective reflective = new JavaReflective(java.util.Arrays.class);
        Map<String, Set<MethodReflective>> methods = reflective.getDeclaredMethods();
        
        Set<MethodReflective> methodRs = methods.get("asList");
        MethodReflective asListR = null;
        for (MethodReflective method : methodRs) {
            List<JavaType> ptypes = method.getParamTypes();
            if (ptypes.size() == 1) {
                if (ptypes.get(0).toString().equals("T[]")) {
                    asListR = method;
                    break;
                }
            }
        }
        
        assertNotNull(asListR);
        JavaType ptype = asListR.getParamTypes().get(0);
        JavaType pctype = ptype.getArrayComponent();
        assertNotNull(pctype);
        assertEquals("java.lang.Object", pctype.getErasedType().toString());
    }
}
