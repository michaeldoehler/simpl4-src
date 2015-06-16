package org.ms123.common.system.compile.java;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.FileObject;
import java.io.IOException;
import java.io.File;
import java.io.FilenameFilter;

import javax.tools.JavaFileObject.Kind;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import javax.tools.SimpleJavaFileObject;
import org.apache.commons.io.FilenameUtils;
import javax.tools.StandardLocation;

/**
 * Created by trung on 5/3/15.
 */
public class ExtendedStandardJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {

	private CompiledCode compiledCode;
	private DynamicClassLoader cl;
	private File[] locations;
	private JavaFileManager fileManager;

	/**
	 */
	protected ExtendedStandardJavaFileManager(JavaFileManager fileManager, File[] locations, CompiledCode compiledCode, DynamicClassLoader cl) {
		super(fileManager);
		this.fileManager = fileManager;
		this.compiledCode = compiledCode;
		this.cl = cl;
		this.cl.setCode(compiledCode);
		this.locations = locations;
	}

	@Override
	public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
		return compiledCode;
	}

	@Override
	public ClassLoader getClassLoader(JavaFileManager.Location location) {
		return cl;
	}

	@Override
	public String inferBinaryName(Location location, JavaFileObject file) {
		if ((location == StandardLocation.CLASS_PATH) && (file instanceof FileSystemJavaFileObject)) {
			FileSystemJavaFileObject fileSystemJavaFileObject = (FileSystemJavaFileObject) file;
			System.err.println("Infering binary name from " + fileSystemJavaFileObject);
			return fileSystemJavaFileObject.inferBinaryName();
		}
		return this.fileManager.inferBinaryName(location, file);
	}

	@Override
	public Iterable<JavaFileObject> list(JavaFileManager.Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
		if (!kinds.contains(Kind.CLASS) || !"CLASS_PATH".equals(location.getName())) {
			return super.list(location, packageName, kinds, recurse);
		}
		List<JavaFileObject> classFileObjects = new ArrayList<>();
		for (File loc : this.locations) {
			File dir = new File(loc, packageName);
			if (dir.exists()) {
				System.err.println("filemanager.location exists:" + packageName);
				for (File f : getClassFileList(dir)) {
					classFileObjects.add(new FileSystemJavaFileObject(f.toURI(), FilenameUtils.getBaseName(f.toString())));
				}
			}
		}
		if (classFileObjects.size() > 0) {
			System.err.println("filemanager.classFileObjects:" + classFileObjects);
			return classFileObjects;
		}
		return super.list(location, packageName, kinds, recurse);
	}

	private File[] getClassFileList(File directory) {
		File[] myFiles = directory.listFiles(new FilenameFilter() {
			public boolean accept(File directory, String fileName) {
				return fileName.endsWith(".class");
			}
		});
		return myFiles;
	}

}

