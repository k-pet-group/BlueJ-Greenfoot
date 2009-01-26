package greenfoot.collision;

import greenfoot.TestObject;
import greenfoot.TestUtilDelegate;
import greenfoot.World;
import greenfoot.WorldCreator;
import greenfoot.util.GreenfootUtil;

import java.util.List;

import junit.framework.TestCase;

/**
 * Tests whether the collision checker works correctly when dealing with sub classes.
 * 
 * @author Poul Henriksen
 */
public class SubClassTests extends TestCase
{

    class SuperClass extends TestObject {

        public SuperClass(int w, int h)
        {
            super(w, h);
        }
    }

    class SubClass extends SuperClass {

        public SubClass(int w, int h)
        {
            super(w, h);
        }
        
    }
    
    class IndependentClass extends TestObject {

        public IndependentClass(int w, int h)
        {
            super(w, h);
        }
        
    }

    @Override
    protected void setUp()
        throws Exception
    {
        GreenfootUtil.initialise(new TestUtilDelegate());        
    }
    
    public void testHierarchy()
    {
        World world = WorldCreator.createWorld(10, 10, 10);

        TestObject superObj = new SuperClass(20, 20);
        world.addObject(superObj, 2, 2);
        TestObject subObj = new SubClass(10, 10);
        world.addObject(subObj, 2, 2);
        TestObject indepObj = new IndependentClass(10, 10);
        world.addObject(indepObj, 2, 2);
        
        List res = indepObj.getIntersectingObjectsP(SuperClass.class);
        assertEquals(2,res.size());        

        res = indepObj.getIntersectingObjectsP(SubClass.class);
        assertEquals(1,res.size());        

        res = indepObj.getIntersectingObjectsP(SuperClass.class);
        assertEquals(2,res.size());       

    }  
    
    public void testHierarchy2()
    {
        World world = WorldCreator.createWorld(10, 10, 10);

        TestObject superObj = new SuperClass(20, 20);
        world.addObject(superObj, 2, 2);
        TestObject subObj = new SubClass(10, 10);
        world.addObject(subObj, 2, 2);
        TestObject indepObj = new IndependentClass(10, 10);
        world.addObject(indepObj, 2, 2);
        
       
        
        List res = superObj.getIntersectingObjectsP(IndependentClass.class);
        assertEquals(1,res.size());        

        res = indepObj.getIntersectingObjectsP(SuperClass.class);
        assertEquals(2,res.size());        
    

    }

}
