package bluej.debugger.gentype;

import java.util.*;

import junit.framework.TestCase;
import bluej.utility.JavaReflective;

/**
 * Tests for the GenType classes.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeTests.java 3077 2004-11-09 04:33:53Z davmac $
 */
public class GenTypeTests extends TestCase
{
    protected void setUp()
    {
        // nothing to do
    }
    
    protected void tearDown()
    {
        // nothing to do
    }
    
    /**
     * Test "map to derived" functionality for a non-immediate derived class.
     *
     * fails in 2.0.2.
     */
    public void test1()
    {
        TestReflective baseReflective = new TestReflective("base");
        TestReflective derivedR = new TestReflective("derived1");
        TestReflective derivedR2 = new TestReflective("derived2");
        
        // Create genType for java.lang.Object
        Class c = Object.class;
        JavaReflective objectR = new JavaReflective(c);
        GenTypeSolid oBound = new GenTypeClass(objectR);

        // each of the three classes will have a type parameter T
        derivedR.typeParams.add(new GenTypeDeclTpar("T", new GenTypeSolid [] {oBound}));
        derivedR2.typeParams.add(new GenTypeDeclTpar("T", new GenTypeSolid [] {oBound}));
        baseReflective.typeParams.add(new GenTypeDeclTpar("T", new GenTypeSolid[] {oBound}));
        
        List tpars = new ArrayList();
        tpars.add(new GenTypeTpar("T"));
        derivedR.superTypes.add(new GenTypeClass(baseReflective, tpars));
        derivedR2.superTypes.add(new GenTypeClass(derivedR, tpars));
        
        List basePars = new ArrayList();
        basePars.add(oBound);
        GenTypeClass baseClass = new GenTypeClass(baseReflective, basePars);
        Map m = baseClass.mapToDerived(derivedR2);
        
        assertTrue(m.get("T").equals(oBound));
    }
    
    /**
     * Must be able to map tpars of a wildcard to a wildcard. The result is
     * not a legal java type, but its string representation must be a legal
     * type.
     */
    public void test2()
    {
        // Create genType for java.lang.Object
        JavaReflective objectR = new JavaReflective(Object.class);
        GenTypeSolid object = new GenTypeClass(objectR);
        
        // Create a wildcard ('wildcard1') "? extends T"
        GenTypeExtends wildcard1 = new GenTypeExtends(new GenTypeTpar("T"));
        
        // create a wildcard ('wildcard2') "? extends Object"
        GenTypeExtends wildcard2 = new GenTypeExtends(object);
        
        // create a mapping "T -> wildcard2"
        Map m = new HashMap();
        m.put("T", wildcard2);
        
        // Apply mapping to wildcard1
        String st = wildcard1.mapTparsToTypes(m).toString(true);
        
        // check that result is a legal java type (when as a string)
        assertEquals("?", st);
    }
}
