package org.ms123.common.system.compile.java;

import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import javax.tools.DiagnosticCollector;
import javax.tools.Diagnostic;
import java.util.Arrays;
import java.util.List;
import java.io.File;
import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.apache.commons.io.FileUtils.readFileToString;
import org.phidias.compile.BundleJavaManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

/**
 * Created by trung on 5/3/15.
 */
public class JavaCompiler {
	static javax.tools.JavaCompiler javac = ToolProvider.getSystemJavaCompiler();


	private static String workspace   = System.getProperty("workspace");
	private static File[] locations = {
		new File(workspace, "jooq/build"),
		new File("locatation2")
	};
	public static Class<?> compile(Bundle bundle, String className, String sourceCodeInText) throws Exception {
		SourceCode sourceCode = new SourceCode(className, sourceCodeInText);
		CompiledCode compiledCode = new CompiledCode(className);
		Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(sourceCode);

		ClassLoader loader = bundle.adapt(BundleWiring.class).getClassLoader();
		DynamicClassLoader cl = new DynamicClassLoader(loader);
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
		ExtendedStandardJavaFileManager fileManager = new ExtendedStandardJavaFileManager(javac.getStandardFileManager(diagnostics, null, null), locations, compiledCode, cl);

 // the OSGi aware file manager
    BundleJavaManager bundleFileManager = new BundleJavaManager( bundle, fileManager, null);

		javax.tools.JavaCompiler.CompilationTask task = javac.getTask(null, bundleFileManager, diagnostics, null, null, compilationUnits);
		fileManager.close();
		if (task.call()) {
			return cl.loadClass(className);
		} else {
			StringBuffer error = new StringBuffer();
			for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
				error.append(diagnostic.toString());
			}
			throw new Exception(error.toString());
		}
	}

	public static void compile(Bundle bundle, String className, File sourceFile, File destinationDirectory) throws Exception {
		compile(bundle, className, readFileToString(sourceFile), destinationDirectory);
	}

	public static void compile(Bundle bundle, String className, String sourceCodeInText, File destinationDirectory) throws Exception {
		SourceCode sourceCode = new SourceCode(className, sourceCodeInText);
		CompiledCode compiledCode = new CompiledCode(className);
		Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(sourceCode);

		ClassLoader loader = bundle.adapt(BundleWiring.class).getClassLoader();
		DynamicClassLoader cl = new DynamicClassLoader(loader);
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
		ExtendedStandardJavaFileManager fileManager = new ExtendedStandardJavaFileManager(javac.getStandardFileManager(diagnostics, null, null), locations, compiledCode, cl);

 // the OSGi aware file manager
    BundleJavaManager bundleFileManager = new BundleJavaManager( bundle, fileManager, null);


		javax.tools.JavaCompiler.CompilationTask task = javac.getTask(null, bundleFileManager, diagnostics, null, null, compilationUnits);
		fileManager.close();
		if (task.call()) {
			writeByteArrayToFile(new File(destinationDirectory, className + ".class"), compiledCode.getByteCode());
		} else {
			StringBuffer error = new StringBuffer();
			for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
				error.append(diagnostic.toString());
			}
			throw new Exception(error.toString());
		}
	}
}

