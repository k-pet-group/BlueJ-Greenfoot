   package bluej.debugger;

   import java.io.*;
   import java.util.*;

    class InspectorClassLoader extends ClassLoader {
   
      File dir;
   
       private InspectorClassLoader(){}
   
       public InspectorClassLoader(File dir){
         this.dir=dir;
      }
   
       public Class findClass(String name) {
         byte[] b = loadClassData(name);
         return defineClass(name, b, 0, b.length);
      }
   
       private byte[] loadClassData(String name) {
                  // load the class data from the connection
         try {
            InputStream is=new FileInputStream(new File(dir,name+".class"));
            Vector bytes=new Vector();
            byte[] bary=new byte[1024];
            int bread=is.read(bary);
            int btotal=0;
            int blast=0;
            while (bread>0) {
               btotal+=bread;
               blast=bread;
               bytes.add(bary);
               bary=new byte[1024];
               bread=is.read(bary);
            }
            Enumeration e = bytes.elements();
            bary=((byte[])e.nextElement());
            int target=0;
            byte[] output=new byte[btotal];
            while (e.hasMoreElements()) {
               System.arraycopy(bary,0,output,target,bary.length);
               target+=bary.length;
               bary=((byte[])e.nextElement());
            }
            System.arraycopy(bary,0,output,target,blast);
            return output;
         }
             catch (IOException e) {
               System.out.println("IOException in class loader");
            }
         return new byte[0];
      }   
   }