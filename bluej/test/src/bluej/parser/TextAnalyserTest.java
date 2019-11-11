/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2019  Michael Kolling and John Rosenberg 
 
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import bluej.JavaFXThreadingRule;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugger.gentype.GenTypeWildcard;
import bluej.debugger.gentype.JavaPrimitiveType;
import bluej.debugger.gentype.JavaType;
import bluej.parser.TextAnalyzer.MethodCallDesc;
import bluej.parser.entity.ClassLoaderResolver;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.entity.PackageResolver;
import bluej.parser.entity.TypeEntity;
import bluej.parser.nodes.ParsedCUNode;
import bluej.utility.JavaReflective;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TextAnalyserTest
{
    @Rule
    public JavaFXThreadingRule javafxRule = new JavaFXThreadingRule();

    @BeforeClass
    public static void initConfig()
    {
        InitConfig.init();
    }
    
    private TestEntityResolver resolver;
    
    @Before
    public void setUp() throws Exception
    {
        resolver = new TestEntityResolver(new ClassLoaderResolver(this.getClass().getClassLoader()));
    }
    
    /**
     * Generate a compilation unit node based on some source code.
     */
    private ParsedCUNode cuForSource(String sourceCode, String pkg)
    {
        EntityResolver resolver = new PackageResolver(this.resolver, pkg);
        TestableDocument document = new TestableDocument(resolver);
        document.enableParser(true);
        document.insertString(0, sourceCode);
        return document.getParser();
    }

    @Test
    public void test1() throws Exception
    {
        List<GenTypeParameter> tpars = new ArrayList<GenTypeParameter>();
        
        // create type par: '? super String'
        tpars.add(new GenTypeWildcard(null, new GenTypeClass(new JavaReflective(String.class))));
        // 'List<? super String>'
        GenTypeClass listClass = new GenTypeClass(new JavaReflective(List.class), tpars);
        
        JavaType argType = JavaPrimitiveType.getInt();
        
        
        List<MethodCallDesc> choices = TextAnalyzer.getSuitableMethods("get", listClass.getCapture(),
                new JavaType[] {argType},
                Collections.<GenTypeParameter>emptyList(), new JavaReflective(Object.class));
        
        assertEquals(1, choices.size());
        MethodCallDesc mcd = choices.get(0);
        
        // The return should be a capture of '? super String'
        assertEquals("java.lang.Object", mcd.retType.getErasedType().toString());
    }

    @Test
    public void test2() throws Exception
    {
        String aClassSrc = "import java.util.List;\n" +
                "abstract class Test1 {\n" +
                "  abstract <T> T foo(List<List<? extends T>> f);\n" +
                "}\n";
              
         ParsedCUNode aNode = cuForSource(aClassSrc, "");
         resolver.addCompilationUnit("", aNode);

         PackageOrClass poc = aNode.resolvePackageOrClass("Test1", null);
         TypeEntity tent = poc.resolveAsType();

         GenTypeClass test1class = tent.getClassType();

         // Argument: List<List<? extends String>>
         GenTypeClass stringClass = new GenTypeClass(new JavaReflective(String.class));
         GenTypeParameter extendsString = new GenTypeWildcard(stringClass, null);
         List<GenTypeParameter> params = new ArrayList<GenTypeParameter>();
         params.add(extendsString);
         GenTypeClass listExtendsString = new GenTypeClass(new JavaReflective(List.class), params);
         params = new ArrayList<GenTypeParameter>();
         params.add(listExtendsString);
         GenTypeClass listListExtendsString = new GenTypeClass(new JavaReflective(List.class), params);

         // Check call to foo returns String:
         List<MethodCallDesc> choices = TextAnalyzer.getSuitableMethods("foo", test1class,
                 new JavaType[] {listListExtendsString},
                 Collections.<GenTypeParameter>emptyList(), test1class.getReflective());

         assertEquals(1, choices.size());
         MethodCallDesc mcd = choices.get(0);
         assertEquals("java.lang.String", mcd.retType.toString());
    }

    @Test
    public void test3() throws Exception
    {
        String aClassSrc = "import java.util.List;\n" +
                "abstract class Test1 {\n" +
                "  abstract <T> T foo(List<List<? super T>> f);\n" +
                "}\n";
              
         ParsedCUNode aNode = cuForSource(aClassSrc, "");
         resolver.addCompilationUnit("", aNode);

         PackageOrClass poc = aNode.resolvePackageOrClass("Test1", null);
         TypeEntity tent = poc.resolveAsType();

         GenTypeClass test1class = tent.getClassType();

         // Argument: List<List<? super String>>
         GenTypeClass stringClass = new GenTypeClass(new JavaReflective(String.class));
         GenTypeParameter extendsString = new GenTypeWildcard(null, stringClass);
         List<GenTypeParameter> params = new ArrayList<GenTypeParameter>();
         params.add(extendsString);
         GenTypeClass listExtendsString = new GenTypeClass(new JavaReflective(List.class), params);
         params = new ArrayList<GenTypeParameter>();
         params.add(listExtendsString);
         GenTypeClass listListExtendsString = new GenTypeClass(new JavaReflective(List.class), params);

         // Check call to foo returns String:
         List<MethodCallDesc> choices = TextAnalyzer.getSuitableMethods("foo", test1class,
                 new JavaType[] {listListExtendsString},
                 Collections.<GenTypeParameter>emptyList(), test1class.getReflective());

         assertEquals(1, choices.size());
         MethodCallDesc mcd = choices.get(0);
         assertEquals("java.lang.String", mcd.retType.toString());
    }

    @Test
    public void testEagerReturnTypeResolutionA1() throws Exception
    {
        String aClassSrc = "class Test1<T> {\n" +
          "  <S> S foo(S x, S y) { return x; }\n" +
          "  <S extends Number & Comparable<? extends Number>> S baz(Test1<S> a) { return null; }\n" +
          "  void bar(Test1<Long> x, Test1<Integer> y) {\n" +
          "    baz(foo(x, y));\n" +
          "  }\n" +
          "}\n";
        
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        PackageOrClass poc = aNode.resolvePackageOrClass("Test1", null);
        TypeEntity tent = poc.resolveAsType();
        
        GenTypeClass test1class = tent.getClassType();
        
        HashMap<String,GenTypeParameter> targMap = new HashMap<String,GenTypeParameter>();
        
        targMap.put("T", new GenTypeClass(new JavaReflective(Long.class)));
        GenTypeClass arg1 = new GenTypeClass(test1class.getReflective(), targMap);
        
        targMap.clear();
        targMap.put("T", new GenTypeClass(new JavaReflective(Integer.class)));
        GenTypeClass arg2 = new GenTypeClass(test1class.getReflective(), targMap);
        
        // Call context is raw, type inference not performed:
        
        List<MethodCallDesc> choices = TextAnalyzer.getSuitableMethods("foo", test1class,
                new JavaType[] {arg1, arg2},
                Collections.<GenTypeParameter>emptyList(), test1class.getReflective());
        
        assertEquals(1, choices.size());
        MethodCallDesc mcd = choices.get(0);
        assertEquals("java.lang.Object", mcd.retType.toString());
        
        // Call context is not raw, type inference performed:
        
        targMap.clear();
        targMap.put("T", new GenTypeClass(new JavaReflective(String.class)));
        GenTypeClass test1classNotRaw = new GenTypeClass(test1class.getReflective(), targMap);

        choices = TextAnalyzer.getSuitableMethods("foo", test1classNotRaw.getCapture(),
                new JavaType[] {arg1, arg2},
                Collections.<GenTypeParameter>emptyList(), test1class.getReflective());
        
        assertEquals(1, choices.size());
        mcd = choices.get(0);
        assertEquals("Test1", mcd.retType.asClass().getErasedType().toString());
        List<? extends GenTypeParameter> tparams =  mcd.retType.asClass().getTypeParamList();
        assertEquals(1, tparams.size());
        
        // The result is a capture, check the supertypes are correct:
        GenTypeClass[] superTypes = tparams.get(0).asSolid().getReferenceSupertypes();
        assertEquals(2, superTypes.length);
        List<GenTypeClass> superTypesList = new ArrayList<GenTypeClass>(superTypes.length);
        Collections.addAll(superTypesList, superTypes);
        GenTypeClass numberClass = new GenTypeClass(new JavaReflective(Number.class));
        assertTrue(superTypesList.contains(numberClass));
        // java.lang.Comparable<? extends Number>
        List<GenTypeParameter> ctparams = new ArrayList<GenTypeParameter>(1);
        ctparams.add(new GenTypeWildcard(numberClass, null));
        GenTypeClass comparable = new GenTypeClass(new JavaReflective(Comparable.class), ctparams);
        // The actual type is recursive, so we just make sure we have it basically right:
        boolean foundComparable = false;
        for (GenTypeClass possible : superTypes) {
            foundComparable = comparable.isAssignableFrom(possible);
            if (foundComparable) break;
        }
        assertTrue(foundComparable);
    }

}
