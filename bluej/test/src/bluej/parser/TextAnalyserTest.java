package bluej.parser;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.swing.text.BadLocationException;

import junit.framework.TestCase;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugger.gentype.JavaType;
import bluej.editor.moe.MoeSyntaxDocument;
import bluej.parser.TextAnalyzer.MethodCallDesc;
import bluej.parser.entity.ClassLoaderResolver;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.entity.PackageResolver;
import bluej.parser.entity.TypeEntity;
import bluej.parser.nodes.ParsedCUNode;
import bluej.utility.JavaReflective;

public class TextAnalyserTest extends TestCase
{
    private TestEntityResolver resolver;
    
    {
        InitConfig.init();
    }
    
    @Override
    protected void setUp() throws Exception
    {
        resolver = new TestEntityResolver(new ClassLoaderResolver(this.getClass().getClassLoader()));
    }
    
    /**
     * Generate a compilation unit node based on some source code.
     */
    private ParsedCUNode cuForSource(String sourceCode, String pkg)
    {
        EntityResolver resolver = new PackageResolver(this.resolver, pkg);
        MoeSyntaxDocument document = new MoeSyntaxDocument(resolver);
        document.enableParser(true);
        try {
            document.insertString(0, sourceCode, null);
        }
        catch (BadLocationException ble) {}
        return document.getParser();
    }

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

        choices = TextAnalyzer.getSuitableMethods("foo", test1classNotRaw,
                new JavaType[] {arg1, arg2},
                Collections.<GenTypeParameter>emptyList(), test1class.getReflective());
        
        assertEquals(1, choices.size());
        mcd = choices.get(0);
        assertEquals("Test1", mcd.retType.asClass().getErasedType().toString());
        List<? extends GenTypeParameter> tparams =  mcd.retType.asClass().getTypeParamList();
        assertEquals(1, tparams.size());
        assertTrue(tparams.get(0).contains(new GenTypeClass(new JavaReflective(Long.class))));
        assertTrue(tparams.get(0).contains(new GenTypeClass(new JavaReflective(Integer.class))));
    }

}
