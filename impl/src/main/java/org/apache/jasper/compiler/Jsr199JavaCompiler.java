/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2010 Sun Microsystems, Inc. All rights reserved.
 * 
 * Portions Copyright Apache Software Foundation.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.apache.jasper.compiler;

import javax.tools.DiagnosticCollector;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.ToolProvider;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Reader;
import java.io.Writer;
import java.io.CharArrayWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.net.URI;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Set;

import org.apache.jasper.JasperException;
import org.apache.jasper.Constants;
import org.apache.jasper.JspCompilationContext;


/**
 * Invoke Java Compiler per JSR 199, using in-memory storage for both the
 * input Java source and the generated bytecodes.
 *
 * @author Kin-man Chung
 */
public class Jsr199JavaCompiler implements JavaCompiler {

    private List<File> cpath;
    private JspRuntimeContext rtctxt;
    private ArrayList<BytecodeFile> classFiles;
        // a JSP compilation can produce multiple class files, we need to
        // keep track of all generated bytecodes..

    private ArrayList<String> options = new ArrayList<String>();
    private CharArrayWriter charArrayWriter;
    private JspCompilationContext ctxt;
    private String javaFileName;
    private String javaEncoding;
    private ErrorDispatcher errDispatcher;

    public void init(JspCompilationContext ctxt,
                     ErrorDispatcher errDispatcher,
                     boolean suppressLogging) {
        this.ctxt = ctxt;
        this.errDispatcher = errDispatcher;
        rtctxt = ctxt.getRuntimeContext();
        options.add("-proc:none");  // Disable annotation processing
    }

    public void release() {
        classFiles = null; // release temp bytecodes
    }

    public void setClassPath(List<File> path) {
        // Jsr199 does not expand jar manifest Class-Path (JDK bug?), we
        // need to do it here
        List<String> paths = new ArrayList<String>();
        for (File f: path) {
            paths.add(f.toString());
        }
        List<String> files = JspUtil.expandClassPath(paths);
        this.cpath = new ArrayList<File>();
        for (String file: files) {
            this.cpath.add(new File(file));
        }
    }

    public void setExtdirs(String exts) {
        options.add("-extdirs");
        options.add(exts);
    }

    public void setSourceVM(String sourceVM) {
        options.add("-source");
        options.add(sourceVM);
    }

    public void setTargetVM(String targetVM) {
        options.add("-target");
        options.add(targetVM);
    }

    public void saveClassFile(String className, String classFileName) {
        for (BytecodeFile bytecodeFile: classFiles) {
            String c = bytecodeFile.getClassName();
            String f = classFileName;
            if (!className.equals(c)) {
                // Compute inner class file name
                f = f.substring(0, f.lastIndexOf(File.separator)+1) +
                    c.substring(c.lastIndexOf('.')+1) + ".class";
            }
            rtctxt.saveBytecode(c, f);
        }
    }

    public void doJavaFile(boolean keep) throws JasperException {

        if (! keep) {
            charArrayWriter = null;
            return;
        }

        try {
            Writer writer = new OutputStreamWriter(
                                    new FileOutputStream(javaFileName),
                                    javaEncoding);
            writer.write(charArrayWriter.toString());
            writer.close();
            charArrayWriter = null;
        } catch (UnsupportedEncodingException ex) {
            errDispatcher.jspError("jsp.error.needAlternateJavaEncoding",
                                   javaEncoding);
        } catch (IOException ex) {
            throw new JasperException(ex);
        }
    }

    public void setDebug(boolean debug) {
        if (debug) {
            options.add("-g");
        } else {
            options.add("-g:none");
        }
    }

    public Writer getJavaWriter(String javaFileName, String javaEncoding) {
        this.javaFileName = javaFileName;
        this.javaEncoding = javaEncoding;
        this.charArrayWriter = new CharArrayWriter();
        return this.charArrayWriter;
    }

    public long getClassLastModified() {
        String className = ctxt.getFullClassName();
        return rtctxt.getBytecodeBirthTime(className);
    }

    public JavacErrorDetail[] compile(String className, Node.Nodes pageNodes)
            throws JasperException {

        final String source = charArrayWriter.toString();
        classFiles = new ArrayList<BytecodeFile>();

        javax.tools.JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        if (javac == null) {
            errDispatcher.jspError("jsp.error.nojdk");
        }

        DiagnosticCollector<JavaFileObject> diagnostics =
            new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager stdFileManager =
                    javac.getStandardFileManager(diagnostics, null, null);

        String name = className.substring(className.lastIndexOf('.')+1);

        JavaFileObject[] sourceFiles = {
            new SimpleJavaFileObject(
                    URI.create("string:///" + name.replace('.','/') +
                               Kind.SOURCE.extension),
                    Kind.SOURCE) {
                public CharSequence getCharContent(boolean ignore) {
                    return source;
                }
            }
        };

        try {
            stdFileManager.setLocation(StandardLocation.CLASS_PATH, this.cpath);
        } catch (IOException e) {
        }

        JavaFileManager javaFileManager = getJavaFileManager(stdFileManager);
        javax.tools.JavaCompiler.CompilationTask ct =
            javac.getTask(null,
                          javaFileManager,
                          diagnostics,
                          options,
                          null, 
                          Arrays.asList(sourceFiles));

        try {
            javaFileManager.close();
        } catch (IOException ex) {
        }

        if (ct.call()) {
            for (BytecodeFile bytecodeFile: classFiles) {
                rtctxt.setBytecode(bytecodeFile.getClassName(),
                                   bytecodeFile.getBytecode());
            }
            return null;
        }

        // There are compilation errors!
        ArrayList<JavacErrorDetail> problems =
            new ArrayList<JavacErrorDetail>();
        for (Diagnostic dm: diagnostics.getDiagnostics()) {
            problems.add(ErrorDispatcher.createJavacError(
                javaFileName,
                pageNodes,
                new StringBuffer(dm.getMessage(null)),
                (int) dm.getLineNumber()));
        }
        return problems.toArray(new JavacErrorDetail[0]);
    }


    private static class BytecodeFile extends SimpleJavaFileObject {

        private byte[] bytecode;
        private String className;

        BytecodeFile(URI uri, String className) {
            super(uri, Kind.CLASS);
            this.className = className;
        }

        String getClassName() {
            return this.className;
        }

        byte[] getBytecode() {
            return this.bytecode;
        }

        public OutputStream openOutputStream() {
            return new ByteArrayOutputStream() {
                public void close() {
                    bytecode = this.toByteArray();
                }
            };
        }

        public InputStream openInputStream() {
            return new ByteArrayInputStream(bytecode);
        }
    }


    private JavaFileObject getOutputFile(final String className,
                                         final URI uri) {

        BytecodeFile classFile = new BytecodeFile(uri, className);

        // File the class file away, by its package name
        String packageName = className.substring(0, className.lastIndexOf("."));
        Map<String, Map<String, JavaFileObject>> packageMap =
            rtctxt.getPackageMap();
        Map<String, JavaFileObject> packageFiles = packageMap.get(packageName);
        if (packageFiles == null) {
            packageFiles = new HashMap<String, JavaFileObject>();
            packageMap.put(packageName, packageFiles);
        }
        packageFiles.put(className, classFile);
        classFiles.add(classFile);
        return classFile;
    }

    private JavaFileManager getJavaFileManager(JavaFileManager fm) {

        return new ForwardingJavaFileManager<JavaFileManager>(fm) {

/*
            @Override
            public FileObject getFileForOutput(Location location,
                                               String packageName,
                                               String relativeName,
                                               FileObject sibling){
                System.out.println(" At getFileForOutput: location = " +
                    location + " pachageName = " + packageName +
                    " relativeName = " + relativeName +
                    " sibling = " + sibling);
                return getOutputFile(relativeName, null);
            }
*/

            @Override
            public JavaFileObject getJavaFileForOutput(Location location,
                                                       String className,
                                                       Kind kind,
                                                       FileObject sibling){
                return getOutputFile(className,
                    URI.create("file:///" + className.replace('.','/') + kind));
            }

            @Override
            public String inferBinaryName(Location location,
                                          JavaFileObject file) {

                if (file instanceof BytecodeFile) {
                    return ((BytecodeFile)file).getClassName();
                }
                return super.inferBinaryName(location, file);
            }

            @Override
            public Iterable<JavaFileObject> list(Location location,
                                         String packageName,
                                         Set<Kind> kinds,
                                         boolean recurse)
                    throws IOException {

                if (location == StandardLocation.CLASS_PATH &&
                        packageName.startsWith(Constants.JSP_PACKAGE_NAME)) {

		    // TODO: Need to handle the case where some of the classes
                    // are on disk

                    Map<String, JavaFileObject> packageFiles
                            = rtctxt.getPackageMap().get(packageName);
                    if (packageFiles != null) {
                        return packageFiles.values();
                    }
                }
                Iterable<JavaFileObject> lst =
                    super.list(location, packageName, kinds, recurse);

                return lst;
            }
        };

    }
}
