/*
 * XMLTokenMarker.java - XML token marker
 * Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package org.syntax.jedit.tokenmarker;

/**
 * XML token marker.
 *
 * @author Slava Pestov
 * @version $Id: XMLTokenMarker.java 3074 2004-11-08 04:24:58Z bquig $
 */
public class XMLTokenMarker extends HTMLTokenMarker
{
	public XMLTokenMarker()
	{
		super(false);
	}
}
