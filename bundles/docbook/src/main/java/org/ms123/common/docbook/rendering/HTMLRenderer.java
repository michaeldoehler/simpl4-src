/**
 * This file is part of SIMPL4(http://simpl4.org).
 *
 * 	Copyright [2014] [Manfred Sattler] <manfred@ms123.org>
 *
 * SIMPL4 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SIMPL4 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SIMPL4.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ms123.common.docbook.rendering;

import java.io.*;

public class HTMLRenderer extends BaseRenderer<HTMLRenderer> {

	private static final String defaultXslStylesheet = "res:xsl/docbook/xhtml/docbook.xsl";

	private HTMLRenderer() {
	}

	public HTMLRenderer css(String cssResource) {
		super.parameter("html.stylesheet", cssResource);
		return this;
	}

	@Override
	protected InputStream getDefaultXslStylesheet() throws Exception {
		return null;
	}

	public static final HTMLRenderer create() {
		return new HTMLRenderer();
	}

	public static final HTMLRenderer create(InputStream xmlResource) {
		return new HTMLRenderer().xml(xmlResource);
	}

	public static final HTMLRenderer create(InputStream xmlResource, String cssResource) {
		return new HTMLRenderer().xml(xmlResource).css(cssResource);
	}
}
