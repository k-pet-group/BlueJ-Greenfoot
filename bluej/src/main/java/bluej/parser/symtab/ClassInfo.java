/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2013,2014,2016,2023  Michael Kolling and John Rosenberg

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
package bluej.parser.symtab;

import java.util.*;

import bluej.parser.entity.JavaEntity;
import bluej.utility.JavaUtils;
import bluej.utility.SortedProperties;
import bluej.utility.StreamUtils;
import org.jetbrains.annotations.NotNull;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Information about a class found in a source file. The information is
 * gathered and stored in an object of this class by a parser.<p>
 * 
 * The information includes:<ul> 
 * <li>what is the name of this class or interface;
 * <li>is it a class or an interface or an enum;
 * <li>is it declared abstract;
 * <li>what classes are extended and interfaces are implemented;
 * <li>what are the type parameters;
 * <li>what other types are referenced from within this type;
 * <li>what javadoc comments are present; 
 * <li>the selection (location in text) for the superclass, superinterfaces,
 * and various other things including type parameters.
 * </ul><p>
 * 
 * Some other information, such as classes which are imported, is stored but
 * not used in BlueJ.
 */
public final class ClassInfo
{
    private static final String[] unitTestClasses = { "junit.framework.TestCase" };

    @OnThread(value = Tag.Any, ignoreParent = true)
    private String name;
    private String superclass;

    private Set<String> implemented = new HashSet<>();
    private Set<String> used = new HashSet<>();

    private Set<String> permits = new HashSet<>();
    @OnThread(value = Tag.Any, ignoreParent = true)
    private List<SavedComment> comments = new LinkedList<SavedComment>();

    private List<String> typeParameterTexts = new ArrayList<String>();
    private Selection typeParametersSelection;
    private Selection extendsReplaceSelection;

    // how we would replace the superclass name in a class
    private Selection superReplaceSelection;

    private Map<String, MethodDesc> methods = new HashMap<>();

    /**
     * Adds or updates a method description
     *
     * @param methodDesc The method description to add/update
     */
    public void addMethod(MethodDesc methodDesc) {
        methods.put(methodDesc.name(), methodDesc);
    }

    /**
     * Gets a method description by name
     *
     * @param name The method name
     * @return Optional containing the method description if found
     */
    public Optional<MethodDesc> getMethod(String name) {
        return Optional.ofNullable(methods.get(name));
    }

    public boolean hasMethod(String name) {
        return methods.containsKey(name);
    }

    public boolean hasMethods() {
        return ! methods.isEmpty();
    }

    private boolean isInterface = false;
    private boolean isAbstract = false;
    private boolean isUnitTest = false;
    private boolean isPublic = false;
    private boolean isEnum = false;
    private boolean isKotlinTopLevelFacade = false;

    private boolean hadParseError = false;

    public boolean isKotlinTopLevelFacade() {
        return isKotlinTopLevelFacade;
    }

    public void setKotlinTopLevelFacade(boolean kotlinTopLevelFacade) {
        isKotlinTopLevelFacade = kotlinTopLevelFacade;
    }

    public class SavedComment
    {
        @OnThread(value = Tag.Any, ignoreParent = true)
        public final String target; // the method signature of the item we have a
                                    // comment for. Can be class name or interface
                                    // name in the case of a comment for a whole
                                    // class/interface

        @OnThread(value = Tag.Any, ignoreParent = true)
        public final String comment;  // the actual text of the comment

        @OnThread(value = Tag.Any, ignoreParent = true)
        public final String paramnames;  // if this is a method or constructor, then
                                         // this is a comma seperated list of namehadParseError
                                         // associated with the parameters

        public SavedComment(String target, String comment, String paramnames)
        {
            if (target == null)
                throw new NullPointerException();
            this.target = target;
            this.comment = comment;
            this.paramnames = paramnames;
        }

        @OnThread(value = Tag.Any, ignoreParent = true)
        public void save(Properties p, String prefix)
        {
            p.put(prefix + ".target", target);
            if(comment != null)
                p.setProperty(prefix + ".text", comment);
            if(paramnames != null)
                p.setProperty(prefix + ".params", paramnames);
        }
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    /**
     * Set the name of the class/interface/enum.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    public void setSuperclass(String name)
    {
        if(name.equals(this.name)) {
            return;
        }

        superclass = name;
        if(used.contains(name)) {
            used.remove(name);
        }

        for (int i = 0; i < unitTestClasses.length; i++) {
            if(name.equals(unitTestClasses[i])) {
                isUnitTest = true;
            }
        }
    }

    public void setEnum(boolean isEnum)
    {
        this.isEnum = isEnum;
    }

    public void addImplements(String name)
    {
        if(name.equals(this.name)) {
            return;
        }

        implemented.add(name);
    }

    public void addPermits(String name)
    {
        if(name.equals(this.name)) {
            return;
        }

        permits.add(name);
    }

    public void addUsed(String name)
    {
        if(name.equals(this.name)) {
            return;
        }

        // don't add predefined types (int, boolean, String, etc)
        //if(SymbolTable.getPredefined().contains(name))
        //    return;

        // don't add superclass
        if(name.equals(superclass)) {
            return;
        }

        // don't add if already there
        used.add(name);
    }

    /**
     * Add a method/constructor description (with optional javadoc comment) to this
     * class. The target specifies the method or constructor which the comment applies
     * to. It takes the form:<p>
     * 
     *  <code>&lt;type-pars&gt; return_type method_name(arg_type_1,arg_type2,arg_type3)</code>
     * 
     * <p>Where:
     * <ul>
     * <li>type-pars are the type parameters, in the form
     *    "&ltT extends bound-type,U extends bound-type&gt;". Should not be present if there are no
     *    type parameters.
     * <li>return_type is the generic return type, or null for a constructor
     * <li>method_name is the name of the method (or the class name for a constructor)
     * <li>arg_type_X is the generic parameter type, followed by "[]" if an array type
     *     (eg. List&lt;Thread&gt;[][]), followed by " ..." for a vararg parameter.
     * </ul>
     * 
     * @param target  The method/constructor the comment applies to (see description above)
     * @param comment   The comment text (may be null)
     * @param paramnames  The parameter names from the method definition, as a space-seperated
     *                    list. May be null if there are no parameter names.
     */
    public void addComment(String target, String comment, String paramnames)
    {
        // remove asterisks (*) from beginning of comment
        comment = JavaUtils.javadocToString(comment);
        comments.add(new SavedComment(target, comment, paramnames));
    }

    public void setInterface(boolean b)
    {
        isInterface = b;
    }

    public void setAbstract(boolean b)
    {
        isAbstract = b;
    }

    public void setParseError(boolean err)
    {
        hadParseError = err;
    }

    /**
     * Where we would insert the string "extends" in a class/interface
     */
    private Selection extendsInsertSelection;

    /**
     * Record where we would insert the string "extends" in a class or interface.
     * For a class/interface which already extends other classes/interfaces, should
     * be set to null.
     *
     * @param s the Selection object which records a location to
     *          insert the "extends" keyword or additional interface
     */
    public void setExtendsInsertSelection(Selection s)
    {
        extendsInsertSelection = s;
    }

    /**
     * Returns where we would insert the string "extends" in a class/interface.
     * For a class which already extends another classes, returns null.
     * 
     * For an interface which extends no other interfaces, returns where to
     * insert "extends {super-interface-name}". For an interface which extends
     * one or more other interfaces already, returns where to insert
     * ", {additional-interface-name}".
     *
     * @returns s the Selection object which records a location to
     *          insert the "extends" keyword
     */
    public Selection getExtendsInsertSelection()
    {
        return extendsInsertSelection;
    }

    /**
     * Where we would insert the string " implements " in a class, or, if the
     * class has existing interfaces, where we would add a new one in
     * (as ", [interfacename]").
     */
    private Selection implementsInsertSelection;

    /**
     * Where we would insert the string " implements " in a class, or, if the
     * class has existing interfaces, where we would add a new one in
     * (as ", [interfacename]").
     */
    public void setImplementsInsertSelection(Selection s)
    {
        implementsInsertSelection = s;
    }

    /**
     * Where we would insert the string " implements " in a class, or, if the
     * class has existing interfaces, where we would add a new one in
     * (as ", [interfacename]").
     */
    public Selection getImplementsInsertSelection()
    {
        return implementsInsertSelection;
    }

    /**
     * Record how we would replace the string "extends" in a class.
     * (For an interface, this is the first selection in the
     *  InterfaceSelections list - see setInterfaceSelections)
     *
     * @param s the Section object which records the location of
     *          the "extends" keyword for a class
     */
    public void setExtendsReplaceSelection(Selection s)
    {
        extendsReplaceSelection = s;
    }

    /**
     * How we would replace the string "extends" in a class.
     * (For an interface, this is the first selection in the
     *  InterfaceSelections list - see setInterfaceSelections)
     */
    public Selection getExtendsReplaceSelection()
    {
        return extendsReplaceSelection;
    }

    public void setSuperReplaceSelection(Selection s)
    {
        superReplaceSelection = s;
    }

    public Selection getSuperReplaceSelection()
    {
        return superReplaceSelection;
    }

    // a vector of Selections of all the elements in a classes
    // "implements" clause ie.
    //     "implements" "InterfaceA" "," "InterfaceB"
    // ... or an interface's "extends" clause ie.
    //     "extends" "InterfaceA" "," "InterfaceB"
    // ... or null if there is no clause
    private List<Selection> interfaceSelections;

    /**
     * Set the selections for the interfaces, including the "implements" clause (or "extends"
     * for interfaces), the interfaces themselves, and the commas between them. Eg:
     * 
     * "extends"  "InterfaceA"  ","  "InterfaceB"
     */
    public void setInterfaceSelections(List<Selection> selections)
    {
        interfaceSelections = selections;
    }

    public void addTypeParameterText(String typeParameterText)
    {
        typeParameterTexts.add(typeParameterText);
    }

    public List<String> getTypeParameterTexts()
    {
        return typeParameterTexts;
    }

    public List<Selection> getInterfaceSelections()
    {
        return interfaceSelections;
    }

    public boolean hasInterfaceSelections()
    {
        return (interfaceSelections != null) &&
                (interfaceSelections.size() > 0);
    }

    /**
     * Record the locations of the tokens in a source files "package" statement.
     *
     * These locations start off at the first line and column of a file.
     * If a package line exists, they are updated, otherwise they are
     * left pointing the very start of the file (which is where we would
     * want to insert a package line if we were to add one)
     */
    private boolean packageStatementExists = false;
    private Selection packageStatementSelection = new Selection(1,1);
    private Selection packageNameSelection = new Selection(1,1);
    private Selection packageSemiSelection = new Selection(1,1);
    private String packageName = "";

    /**
     * Set the selections for the "package" line of the source file, including the "pakage"
     * keyword (pkgStatement), the named package (pkgName), and the trailing semicolon
     * (pkgSemi).
     * 
     * @param pkgStatement
     * @param pkgName
     * @param pkgNameText
     * @param pkgSemi
     */
    public void setPackageSelections(Selection pkgStatement, Selection pkgName, String pkgNameText,
                                        Selection pkgSemi)
    {
        packageStatementSelection = pkgStatement;
        packageNameSelection = pkgName;
        packageName = pkgNameText;
        packageSemiSelection = pkgSemi;

        packageStatementExists = true;
    }

    public boolean hasPackageStatement()
    {
        return packageStatementExists;
    }

    public Selection getPackageStatementSelection()
    {
        return packageStatementSelection;
    }

    public Selection getPackageNameSelection()
    {
        return packageNameSelection;
    }

    public Selection getPackageSemiSelection()
    {
        return packageSemiSelection;
    }

    public String getPackage()
    {
        return packageName;
    }



    // accessors:

    /**
     * Get the (fully-qualified) name of the superclass of the represented class.
     * Returns null if the superclass is not established or unspecified (i.e. is
     * "java.lang.Object").
     */
    public String getSuperclass()
    {
        return superclass;
    }

    @OnThread(value = Tag.Any, ignoreParent = true)
    public String getName()
    {
        return name;
    }

    /**
     * Get a list of the (fully-qualified) interface names that the represented
     * class implements.
     */
    public List<String> getImplements()
    {
        return List.copyOf(implemented);
    }

    public void setTypeParametersSelection(Selection s)
    {
        typeParametersSelection = s;
    }

    public boolean hasTypeParameter()
    {
        return (typeParametersSelection != null);
    }

    /**
     * Get the list of referenced classes (a list of String).
     */
    public List<String> getUsed()
    {
        return List.copyOf(used);
    }

    /**
     * Get the list of classes in the permits clause, if any (a list of String).
     * Returns an empty list if there are none.
     */
    public List<String> getPermits()
    {
        return List.copyOf(permits);
    }

    @OnThread(value = Tag.Any, ignoreParent = true)
    public Properties getComments()
    {
        Properties props = new SortedProperties();
        props.setProperty("numComments", String.valueOf(comments.size()));
        Iterator<SavedComment> it = comments.iterator();
        for(int i = 0; it.hasNext(); i++)
        {
            SavedComment c = it.next();
            c.save(props, "comment" + i);
        }
        return props;
    }

    public List<SavedComment> getCommentsAsList()
    {
        return Collections.unmodifiableList(comments);
    }

    public boolean isInterface()
    {
        return this.isInterface;
    }

    public boolean isAbstract()
    {
        return this.isAbstract;
    }

    public boolean isUnitTest()
    {
        return this.isUnitTest;
    }

    public boolean isEnum()
    {
        return this.isEnum;
    }

    public boolean hadParseError()
    {
        return hadParseError;
    }

    public boolean isEquivalentTo(Object other)
    {
        if (!(other instanceof ClassInfo)) { return false; }
        ClassInfo otherClass = (ClassInfo) other;

        if (!getFqdn().equals(otherClass.getFqdn())) { return false; }

        var equivalentModifiers = isInterface() == otherClass.isInterface()
            && isAbstract() == otherClass.isAbstract()
            && isUnitTest() == otherClass.isUnitTest()
            && isEnum() == otherClass.isEnum()
            && isPublic() == otherClass.isPublic()
            && isKotlinTopLevelFacade() == otherClass.isKotlinTopLevelFacade();

        if (!equivalentModifiers) { return false; }

        if (!implemented.equals(otherClass.implemented)) { return false; }
        if (!permits.equals(otherClass.permits)) { return false; }
        if (!used.equals(otherClass.used)) { return false; }
        if (!typeParameterTexts.equals(otherClass.typeParameterTexts)) { return false; }

        var methodsEquivalent = this.methods.keySet().stream().allMatch(key -> {
            var thisMethod = this.methods.get(key);
            var otherMethod = otherClass.methods.get(key);

            return thisMethod.isEquivalentTo(otherMethod);
        });

        if (!methodsEquivalent) { return false; }

        return true;
    }

    public String getFqdn() {
        var packageName = getPackage();
        var name = getName();

        if (packageName != null && !packageName.isEmpty()) { return packageName + "." + name; }
        return name;
    }

    /**
     * Represents a method description
     */
    public record MethodDesc(
            @NotNull String name,
            @NotNull Optional<JavaEntity> returnType, // none for constructors
            @NotNull List<JavaEntity> paramTypes,
            @NotNull List<String> paramNames,
            @NotNull Optional<String> javadocText
    ) {
        public boolean isEquivalentTo(Object other) {
            if (!(other instanceof MethodDesc otherMethod)) { return false; }

            if (!name().equals(otherMethod.name())) { return false; }
            if (!returnType().flatMap(type -> otherMethod.returnType().map(type::isEquivalentTo)).orElse(false)) { return false; }
            if (!StreamUtils.zip(paramTypes(), otherMethod.paramTypes()).allMatch(pair -> pair.getKey().isEquivalentTo(pair.getValue()))) { return false; }
            if (!paramNames().equals(otherMethod.paramNames())) { return false; }
            if (!javadocText().equals(otherMethod.javadocText())) { return false; }

            return true;
        }
    }
}
