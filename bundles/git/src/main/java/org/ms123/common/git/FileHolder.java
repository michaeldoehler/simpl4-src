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
import org.ms123.common.rpc.RpcException;
import org.eclipse.jgit.storage.file.*;
import org.eclipse.jgit.util.*;
import java.io.FileInputStream;
import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedInputStream;
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
import org.eclipse.jgit.internal.storage.file.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import org.eclipse.jgit.errors.LockFailedException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.IO;
import org.eclipse.jgit.util.RawParseUtils;
import java.nio.charset.Charset;

public class FileHolder extends StoredConfig implements FileHolderApi{

	public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	private File configFile = null;

	private FS fs;

	private boolean utf8Bom;

	private volatile FileSnapshot snapshot;

	private volatile ObjectId hash;

	private File m_file;

	private String m_content;

	private Boolean m_isUnknown;

	/**
	 * Create a configuration with no default fallback.
	 *
	 * @param cfgLocation
	 *            the location of the configuration file on the file system
	 * @param fs
	 *            the file system abstraction which will be necessary to perform
	 *            certain file system operations.
	 */
	public FileHolder(File cfgLocation) {
		this(null, cfgLocation);
	}

	/**
	 * The constructor
	 *
	 * @param base
	 *            the base configuration file
	 * @param cfgLocation
	 *            the location of the configuration file on the file system
	 * @param fs
	 *            the file system abstraction which will be necessary to perform
	 *            certain file system operations.
	 */
	public FileHolder(Config base, File cfgLocation) {
		super(base);
		configFile = cfgLocation;
		this.fs = FS.detect();
		this.snapshot = FileSnapshot.DIRTY;
		this.hash = ObjectId.zeroId();
		m_isUnknown = false;
	}

	@Override
	protected boolean notifyUponTransientChanges() {
		// we will notify listeners upon save()
		return false;
	}

	/** @return location of the configuration file on disk */
	public final File getFile() {
		return configFile;
	}

	public String getType() throws Exception {
		if (m_isUnknown)
			return "sw.unknown";
		load();
		if (m_isUnknown)
			return "sw.unknown";
		String type = getString("sw", null, "type");
		return type;
	}

	public void setString(String key,String value){
		setString("sw", null, key,value);
	}

	public String getString(String key){
		return getString("sw", null, key);
	}

	public String getContent() throws Exception {
		load();
		return m_content;
	}

	public void putContent(String content) throws Exception {
		load();
		unset("sw", null, "content");
		m_content = content;
		save();
	}

	public void putContent(String type, String content) throws Exception {
		putContent(type, content, null);
	}

	public void putContent(String type, String content, Map<String, Object> meta) throws Exception {
		clear();
		setString("sw", null, "type", type);
		//if (content != null) {
		//	setString("sw", null, "content", content);
		//}
		if (meta != null) {
			for (String key : meta.keySet()) {
				Object value = meta.get(key);
				if (value instanceof String) {
					setString("meta", null, key, (String) value);
				}
				if (value instanceof Boolean) {
					setBoolean("meta", null, key, (Boolean) value);
				}
				if (value instanceof Integer) {
					setInt("meta", null, key, (Integer) value);
				}
				if (value instanceof Long) {
					setLong("meta", null, key, (Integer) value);
				}
			}
		}
		m_content = content;
		save();
		m_isUnknown = false;
	}

	/*private void load()throws Exception{
		if( m_isUnknown ) throw new RuntimeException("GitFileReader.unknown_filetype");
		try{
			FileInputStream fis = new FileInputStream(m_file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			readText(getFirstPart(fis));
			br.close();
		}catch(org.eclipse.jgit.errors.ConfigInvalidException e){
			m_isUnknown = true;
		}
	}*/
	/**
	 * Load the configuration as a Git text style configuration file.
	 * <p>
	 * If the file does not exist, this configuration is cleared, and thus
	 * behaves the same as though the file exists, but is empty.
	 *
	 * @throws IOException
	 *             the file could not be read (but does exist).
	 * @throws ConfigInvalidException
	 *             the file is not a properly formatted configuration file.
	 */
	@Override
	public void load() throws IOException, ConfigInvalidException {
		final FileSnapshot oldSnapshot = snapshot;
		final FileSnapshot newSnapshot = FileSnapshot.save(getFile());
		DelimiterInputStream dis = null;
		try {
			dis = new DelimiterInputStream(new BufferedInputStream(new FileInputStream(getFile())));
			byte[] array = new byte[] { 45, 45, 10 };
			byte[] in = dis.readTill(array);
			final ObjectId newHash = hash(in);
			if (hash.equals(newHash)) {
				if (oldSnapshot.equals(newSnapshot))
					oldSnapshot.setClean(newSnapshot);
				else
					snapshot = newSnapshot;
			} else {
				final String decoded;
				if (in.length >= 3 && in[0] == (byte) 0xEF && in[1] == (byte) 0xBB && in[2] == (byte) 0xBF) {
					decoded = RawParseUtils.decode(UTF8_CHARSET, in, 3, in.length);
					utf8Bom = true;
				} else {
					decoded = RawParseUtils.decode(in);
				}
				String content = null;
				try {
					fromText(decoded);
					content = getString("sw", null, "content");
				} catch (org.eclipse.jgit.errors.ConfigInvalidException c) {
					dis = new DelimiterInputStream(new FileInputStream(getFile()));
					m_isUnknown = true;
				}
				if( content != null){
					m_content = content;
				}else{
					in = dis.readTill(null);
					m_content = RawParseUtils.decode(in);
				}
				snapshot = newSnapshot;
				hash = newHash;
			}
		} catch (FileNotFoundException noFile) {
			clear();
			snapshot = newSnapshot;
		} catch (IOException e) {
			final IOException e2 = new IOException(MessageFormat.format(JGitText.get().cannotReadFile, getFile()));
			e2.initCause(e);
			throw e2;
		}finally{
			if( dis != null) dis.close();
		}
	}

	/**
	 * Save the configuration as a Git text style configuration file.
	 * <p>
	 * <b>Warning:</b> Although this method uses the traditional Git file
	 * locking approach to protect against concurrent writes of the
	 * configuration file, it does not ensure that the file has not been
	 * modified since the last read, which means updates performed by other
	 * objects accessing the same backing file may be lost.
	 *
	 * @throws IOException
	 *             the file could not be written.
	 */
	public void save() throws IOException {
		byte[] out1;
		byte[] out2 = new byte[0];
		byte[] outTotal;
		final String text = toText();
		if (utf8Bom) {
			final ByteArrayOutputStream bos = new ByteArrayOutputStream();
			bos.write(0xEF);
			bos.write(0xBB);
			bos.write(0xBF);
			bos.write(text.getBytes(UTF8_CHARSET.name()));
			out1 = bos.toByteArray();
		} else {
			out1 = encode(text);
		}
		if (m_content != null) {
			out2 = encode("--\n" + m_content);
		}
		outTotal = concat(out1, out2);
		final LockFile lf = new LockFile(getFile(), fs);
		if (!lf.lock())
			throw new LockFailedException(getFile());
		try {
			lf.setNeedSnapshot(true);
			lf.write(outTotal);
			if (!lf.commit()) {
				throw new IOException(MessageFormat.format(JGitText.get().cannotCommitWriteTo, getFile()));
			}
		} finally {
			lf.unlock();
		}
		snapshot = lf.getCommitSnapshot();
		hash = hash(outTotal);
		fireConfigChangedEvent();
	}

	public static byte[] encode(final String str) {
		final ByteBuffer bb = Constants.CHARSET.encode(str);
		final int len = bb.limit();
		if (bb.hasArray() && bb.arrayOffset() == 0) {
			final byte[] arr = bb.array();
			if (arr.length == len)
				return arr;
		}
		final byte[] arr = new byte[len];
		bb.get(arr);
		return arr;
	}

	byte[] concat(byte[]... arrays) {
		int totalLength = 0;
		for (int i = 0; i < arrays.length; i++) {
			totalLength += arrays[i].length;
		}
		byte[] result = new byte[totalLength];
		int currentIndex = 0;
		for (int i = 0; i < arrays.length; i++) {
			System.arraycopy(arrays[i], 0, result, currentIndex, arrays[i].length);
			currentIndex += arrays[i].length;
		}
		return result;
	}

	@Override
	public void clear() {
		hash = hash(new byte[0]);
		super.clear();
	}

	private static ObjectId hash(final byte[] rawText) {
		return ObjectId.fromRaw(Constants.newMessageDigest().digest(rawText));
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + getFile().getPath() + "]";
	}

	/**
	 * @return returns true if the currently loaded configuration file is older
	 * than the file on disk
	 */
	public boolean isOutdated() {
		return snapshot.isModified(getFile());
	}

	public static void main(String[] args) throws Exception {
		FileHolder fh = new FileHolder(new File("./XXX"));
		System.out.println("Content1:" + fh.getContent() + "/" + fh.getType());
		fh.putContent("sw.ttt", fh.getContent());
		fh = new FileHolder(new File("./XXX"));
		System.out.println("Content2:" + fh.getContent() + "/" + fh.getType());
	}
}
