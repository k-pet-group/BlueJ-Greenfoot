/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2017  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.parser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.UnresolvedEntity;
import bluej.parser.lexer.LocatableToken;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A parser to handle "import" statements for the Code Pad.
 * 
 * @author Davin McCall
 */
@OnThread(Tag.FXPlatform)
public class CodepadImportParser extends JavaParser
{
    private EntityResolver resolver;
 
    private boolean importIsStatic = false;
    private boolean importIsWildcard = false;
    private JavaEntity importEntity;
    private String memberName;  // for static imports
    
   /**
    * Construct a codepad import parser. The next step is usually to
    * call "parseImportStatement()".
    */
    public CodepadImportParser(EntityResolver resolver, Reader r)
    {
        super(r);
        this.resolver = resolver;
    }
    
    /**
     * If import statement parse succeeded, this method reveals whether the
     * import was a static import ("import static xyz.abc")
     */
    public boolean isStaticImport()
    {
        return importIsStatic;
    }
    
    /**
     * If import statement parse succeeded, this method reveals whether the
     * import was a wildcard import ("import xyz.*", "import static xyz.*")
     */
    public boolean isWildcardImport()
    {
        return importIsWildcard;
    }
    
    /**
     * If import statement parse succeeded, this method returns an entity
     * describing the imported entity. For wildcard imports this is the entity
     * before the wildcard; for (non-wildcard) static imports this is the containing
     * entity, and the imported member(s) name can be determined using getMemberName().
     */
    public JavaEntity getImportEntity()
    {
        return importEntity;
    }
    
    /**
     * If import statement parse succeeded, and the import is a non-wildcard static
     * import, this method returns the imported member(s) name (otherwise returns
     * null).
     */
    public String getMemberName()
    {
        return memberName;
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    protected void gotImport(List<LocatableToken> tokens, boolean isStatic, LocatableToken importToken, LocatableToken semiColonToken)
    {
        importIsStatic = isStatic;
        
        if (isStatic) {
            // Apparently static classes can be imported with or without the "static" keyword
            // So, a static import imports a field and/or method and/or class.
            // That's right - the same import statement pulls in all three.
            
            // We want to pull the name out
            int newSize = tokens.size() - 2;
            memberName = tokens.get(newSize + 1).getText();
            
            List<LocatableToken> newList = new ArrayList<LocatableToken>(newSize);
            Iterator<LocatableToken> i = tokens.iterator();
            while (newSize > 0) {
                newList.add(i.next());
                newSize--;
            }
            tokens = newList;
        }
        else {
            memberName = tokens.get(tokens.size() - 1).getText();
        }
        
        importEntity = getEntityForTokens(tokens);
    }
    
    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    protected void gotWildcardImport(List<LocatableToken> tokens,
                                     boolean isStatic, LocatableToken importToken, LocatableToken semiColonToken)
    {
        importEntity = getEntityForTokens(tokens);
        importIsWildcard = true;
        importIsStatic = isStatic;
    }
    
    /**
     * Get an entity for the given tokens. The tokens should be a dotted identifier,
     * eg "java.lang.String", "java.awt.Color.BLACK", etc.
     */
    @OnThread(Tag.FXPlatform)
    protected JavaEntity getEntityForTokens(List<LocatableToken> tokens)
    {
        Iterator<LocatableToken> i = tokens.iterator();
        String name = i.next().getText();
        JavaEntity entity = UnresolvedEntity.getEntity(resolver, name, null);
        while (entity != null && i.hasNext()) {
            i.next(); // should be the '.' token
            name = i.next().getText();
            entity = entity.getSubentity(name, null);
        }
        return entity;
    }
}
