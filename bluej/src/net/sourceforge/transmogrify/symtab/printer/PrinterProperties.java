/*
Copyright (C) 2001  ThoughtWorks, Inc

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
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package net.sourceforge.transmogrify.symtab.printer;

import java.io.*;
import java.util.*;

public class PrinterProperties {

  private static Properties properties;
  public static String printerSettingsLocation = "printerSettings";

  private static final int defaultSpacesPerIndent = 2;
  private static final boolean defaultNewLineBetweenExpressionGroups = true;
  private static final boolean defaultOpenBraceOnNewLine = true;
  private static final boolean defaultSiblingBlockOnNewLine = true;
  private static final boolean defaultSpaceBeforeBlockExpression = true;
  private static final boolean defaultSpaceInsideParens = false;
  private static final boolean defaultSpaceOutsideBraces = true;
  private static final boolean defaultSpaceAfterComma = true;
  private static final boolean defaultSpaceBeforeArrayDeclarator = false;
  private static final boolean defaultArrayDeclaratorOnType = true;

  private PrinterProperties() {}            
  
  static {
		try {
		  properties = new Properties();
		  properties.load(ClassLoader.getSystemResourceAsStream("printer.properties"));
		}
		catch (Exception e) {
		  System.out.println("Unable to find properties file.  Using defaults.");
		}
  }  

  public static int getSpacesPerIndent() {
		String value = properties.getProperty("spacesPerIndent", String.valueOf(defaultSpacesPerIndent));			
		int result = new Integer(value).intValue();
		if (result < 0) {
			result = defaultSpacesPerIndent;
		}
		
		return result;
  }                
  
  public static boolean hasNewLineBetweenExpressionGroups() {
	  String value = properties.getProperty("newLineBetweenExpressionGroups", String.valueOf(defaultNewLineBetweenExpressionGroups));
		boolean result = new Boolean(value).booleanValue();
		
		return result;
  }                

  public static boolean hasOpenBraceOnNewLine() {
	  String value = properties.getProperty("openBraceOnNewLine", String.valueOf(defaultOpenBraceOnNewLine));
		boolean result = new Boolean(value).booleanValue();

		return result;
  }                

  public static boolean hasSiblingBlockOnNewLine() {
	  String value = properties.getProperty("siblingBlockOnNewLine", String.valueOf(defaultSiblingBlockOnNewLine));
		boolean result = new Boolean(value).booleanValue();

		return result;
  }                

  public static boolean hasSpaceBeforeBlockExpression() {
	  String value = properties.getProperty("spaceBeforeBlockExpression", String.valueOf(defaultSpaceBeforeBlockExpression));
	  boolean result = new Boolean(value).booleanValue();

		return result;
  }                

  public static boolean hasSpaceInsideParens() {
	  String value = properties.getProperty("spaceInsideParens", String.valueOf(defaultSpaceInsideParens));
		boolean result = new Boolean(value).booleanValue();

		return result;
  }                

  public static boolean hasSpaceOutsideBraces() {
	  String value = properties.getProperty("spaceOutsideBraces", String.valueOf(defaultSpaceOutsideBraces));
		boolean result = new Boolean(value).booleanValue();

		return result;
  }                

  public static boolean hasSpaceAfterComma() {
	  String value = properties.getProperty("spaceAfterComma", String.valueOf(defaultSpaceAfterComma));
		boolean result = new Boolean(value).booleanValue();
		
		return result;
  }                

  public static boolean hasSpaceBeforeArrayDeclarator() {
	  String value = properties.getProperty("spaceBeforeArrayDeclarator", String.valueOf(defaultSpaceBeforeArrayDeclarator));
		boolean result = new Boolean(value).booleanValue();
	
		return result;
  }                

  public static boolean hasArrayDeclaratorOnType() {
		String value = properties.getProperty("arrayDeclaratorOnType", String.valueOf(defaultArrayDeclaratorOnType));
		boolean result = new Boolean(value).booleanValue();

		return result;
  }                
}
