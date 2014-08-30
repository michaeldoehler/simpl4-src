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

import java.util.List;
import java.io.FileInputStream;
import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;
import java.util.Iterator;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.apache.commons.io.FileUtils.readFileToString;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.*;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.*;
import java.nio.file.attribute.*;

public class FileObject {

	private Path m_path = null;

	public FileObject(File file) throws IOException{
		this(file,null);
	}
	public FileObject(File file, String type) throws IOException{
		m_path = file.toPath();
		if( file.exists() && getType() == null){
			convertFile();
		}
		if( type != null){
			setType(type);
		}
	}

	private final File getFile() {
		return m_path.toFile();
	}

	private final Path getPath() {
		return m_path;
	}

	public String getType(){
		try{
			return getString("sw.type");
		}catch(Exception e){
			return null;
		}
	}
	public void setType(String type)  throws IOException{
		setString("sw.type",type);
	}

	public void setString(String key,String value) throws IOException{
		System.out.println("setString:"+key+"="+value);
		Path path = getPath();
		UserDefinedFileAttributeView view = Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
		view.write(key, Charset.defaultCharset().encode(value));
	}

	public String getString(String key) throws IOException{
		Path path = getPath();
		UserDefinedFileAttributeView view = Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
		ByteBuffer bb = ByteBuffer.allocateDirect(64);
		int num = view.read(key,bb);
		bb.flip();
		return Charset.defaultCharset().decode(bb).toString();
	}

	public String getContent() throws IOException {
		return readFileToString(getFile());
	}

	public void putContent(String content) throws IOException {
		writeStringToFile(getFile(),content);
	}

	public void putContent(String type, String content) throws IOException {
		writeStringToFile(getFile(),content);
		setType(type);
	}

	@Override
	public String toString() {
		return "[" + getFile().getPath()+"/"+getType() + "]";
	}


	private void convertFile(){
		FileHolder fr = new FileHolder(getFile());
		try{
			String type = fr.getType();
			String content = fr.getContent();
			putContent(type,content);
		}catch(Exception e){
			throw new RuntimeException("cannot convert '"+getFile().toString()+"':"+e.getMessage());
		}
	}
}
