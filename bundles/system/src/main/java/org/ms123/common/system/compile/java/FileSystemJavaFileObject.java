package org.ms123.common.system.compile.java;

import java.io.IOException;
import java.io.InputStream;

import java.net.URI;

import javax.tools.SimpleJavaFileObject;

/**
 */
public class FileSystemJavaFileObject extends SimpleJavaFileObject {

	public FileSystemJavaFileObject(URI uri, String className) {
		super(uri, Kind.CLASS);

		_className = className;
	}

	public String inferBinaryName() {
		return _className;
	}

	@Override
	public InputStream openInputStream() throws IOException {
		return toUri().toURL().openStream();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName().concat("[").concat(toUri().toString()).concat("]");
	}

	private String _className;

}

