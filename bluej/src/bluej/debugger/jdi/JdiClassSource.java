package bluej.debugger.jdi;

import java.util.Iterator;
import java.util.List;

import com.sun.jdi.ClassLoaderReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;

/**
 * This class is intended to represent a virtual machine/class loader combo,
 * which can be used to locate a class (and its generic signature information)
 * using the class name. 
 * @author Davin McCall
 * @version $Id: JdiClassSource.java 2547 2004-05-26 05:17:29Z davmac $
 */
public class JdiClassSource {

    private ClassLoaderReference cl;
    private VirtualMachine vm;
    
    /**
     * 
     */
    public JdiClassSource(VirtualMachine vm, ClassLoaderReference cl)
    {
        this.cl = cl;
        this.vm = vm;
    }
    
    public ReferenceType classByName(String name)
    {
        List l = vm.classesByName(name);
        for(Iterator i = l.iterator(); i.hasNext(); ) {
            ReferenceType rt = (ReferenceType)i.next();
            if( rt.classLoader() == cl )
                return rt;
        }
        return null;
    }
}
