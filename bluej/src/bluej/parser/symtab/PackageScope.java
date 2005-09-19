package bluej.parser.symtab;

import java.util.ArrayList;
import java.util.List;

public class PackageScope extends Scope
{
    // private ClassInfo info;
    private List references = new ArrayList();
    
    public PackageScope()
    {
        super(null);
        // this.info = info;
    }
    
    public boolean checkType(String name)
    {
        boolean r = super.checkType(name);
        if (r) {
            references.add(name);
        }
        return r;
    }
    
    public List getReferences()
    {
        return references;
    }
}
