package bluej.pkgmgr.tagger;

import java.util.List;

import bluej.debugger.gentype.JavaType;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.entity.TypeArgumentEntity;
import bluej.pkgmgr.Package;

public class TaggerPackage extends PackageOrClass
{
    private Package pkg;
    
    public TaggerPackage(Package pkg)
    {
        this.pkg = pkg;
    }
    
    @Override
    public PackageOrClass getPackageOrClassMember(String name)
    {
        //if ()
        return null;
    }

    @Override
    public String getName()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JavaEntity getSubentity(String name)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JavaType getType()
    {
        return null;
    }

    @Override
    public JavaEntity setTypeArgs(List<TypeArgumentEntity> tparams)
    {
        return null;
    }

}
