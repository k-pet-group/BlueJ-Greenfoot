   package bluej.debugger;

   import java.awt.*;
   import java.awt.event.*;

   import java.util.*;

   import java.io.*;

/**
 * The event which occurs when an object is to be retrieved in an Inspector.
 * 
 * @author Duane Buck
 * @version $Id: InspectorEvent.java 710 2000-11-22 06:33:42Z dbuck $
 */
    public class InspectorEvent extends EventObject {
   
      public final static int INSPECT = 1;
      public final static int GET = 2;
   
      protected DebuggerObject obj;
      protected int id;
   
       public InspectorEvent(Object source, int id,  DebuggerObject obj)
      {
         super(source);
      
         this.id = id;
         this.obj = obj;                
      }
   
       public int getID()
      {
         return id;        
      }
   
       public DebuggerObject getDebuggerObject()
      {
         return obj;        
      }
   }
