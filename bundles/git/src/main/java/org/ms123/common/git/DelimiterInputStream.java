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
package org.ms123.common.git;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

/**
 * 
 */
public class DelimiterInputStream extends PushbackInputStream {

	static int MAX_DELIMITER_SIZE = 300;

	/**
	 * @param in
	 */
	public DelimiterInputStream(InputStream in) {
		super(in, MAX_DELIMITER_SIZE);
	}

	/**
	 * reads till delimiter is found
	 * 
	 * @param delimiter
	 * @return the bytes read till the beginning of delimiter
	 * @throws IOException
	 * @throws DelimiterNotFoundException
	 */
	public byte[] readTill(byte[] delimiter) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int posInDelimiter = 0;
		while (true) {
			int ch = read();
			if (ch == -1) {
				return baos.toByteArray();
			}
			if (delimiter != null && ch == delimiter[posInDelimiter]) {
				posInDelimiter++;
				if (posInDelimiter == delimiter.length) {
					return baos.toByteArray();
				}
			} else {
				if (posInDelimiter > 0) {
					unread(ch);
					unread(delimiter, 1, posInDelimiter - 1);
					posInDelimiter = 0;
					ch = delimiter[0];
				}
				baos.write(ch);
			}
		}
	}
}
