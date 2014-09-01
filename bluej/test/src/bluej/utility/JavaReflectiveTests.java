package bluej.utility;

import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import bluej.debugger.gentype.FieldReflective;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.MethodReflective;

/**
 * Tests for JavaReflective
 * 
 * @author Davin McCall
 */
public class JavaReflectiveTests extends TestCase
{
    /**
     * This field is used by one of the tests below.
     */
    @SuppressWarnings("unused")
    private int testIntField;
    
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
    
    public void testPrimitiveFieldAccess()
    {
        JavaReflective jref = new JavaReflective(JavaReflectiveTests.class);
        Map<String,FieldReflective> fields = jref.getDeclaredFields();
        
        FieldReflective intField = fields.get("testIntField");
        assertNotNull(intField);
        assertTrue(intField.getType().isPrimitive());
        assertEquals("int", intField.getType().toString());
    }
    
    class Inner { }
    
    static class StaticInner { }
    
    public void testNestedClass()
    {
        JavaReflective innerR = new JavaReflective(Inner.class);
        assertEquals(this.getClass().getName(), innerR.getOuterClass().getName());
        
        JavaReflective sinnerR = new JavaReflective(StaticInner.class);
        assertEquals(this.getClass().getName(), sinnerR.getOuterClass().getName());
    }
}
