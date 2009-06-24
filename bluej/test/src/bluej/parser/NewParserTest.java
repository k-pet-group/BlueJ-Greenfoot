package bluej.parser;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

public class NewParserTest extends TestCase
{
	/**
	 * Test array as type parameter
	 */
	public void test1()
	{
		StringReader sr = new StringReader(
				"LinkedList<String[]>"
		);
		InfoParser ip = new InfoParser(sr);
		List ll = new LinkedList();
		assertTrue(ip.parseTypeSpec(false, ll));
		// 6 tokens: LinkedList, '<', String, '[', ']', '>'
		assertEquals(6, ll.size());
	}
	
	/**
	 * Test handling of '>>' sequence in type spec
	 */
	public void test2()
	{
		StringReader sr = new StringReader(
				"LinkedList<List<String[]>>"
		);
		InfoParser ip = new InfoParser(sr);
		List ll = new LinkedList();
		assertTrue(ip.parseTypeSpec(false, ll));
		// 6 tokens: LinkedList, '<', String, '[', ']', '>'
		assertEquals(9, ll.size());
	}
	
}
