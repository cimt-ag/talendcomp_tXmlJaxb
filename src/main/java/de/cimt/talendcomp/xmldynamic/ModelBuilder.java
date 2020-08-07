package de.cimt.talendcomp.xmldynamic;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.jar.JarFile;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.xml.sax.SAXParseException;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.writer.FileCodeWriter;
import com.sun.tools.xjc.AbortException;
import com.sun.tools.xjc.ErrorReceiver;
import com.sun.tools.xjc.ModelLoader;
import com.sun.tools.xjc.model.Model;
import com.sun.tools.xjc.outline.Outline;
import com.sun.tools.xjc.outline.PackageOutline;
import com.sun.tools.xjc.util.ErrorReceiverFilter;

/**
 * Builds a {@link Model} object.
 *
 * This is an utility class that makes it easy to load a grammar object from
 * various sources.
 *
 * @author Daniel Koch (daniel.koch@cimt-ag.de)
 *
 */
public final class ModelBuilder {

    private static final Logger LOG = LoggerFactory.getLogger("de.cimt.talendcomp.xmldynamic");
    private static final Set<String> MODELS = new HashSet<String>();
    public static final Object LOCK = new Object();

    public static boolean isModelAlreadyBuild(String grammarFilePath) {
    	if (grammarFilePath == null) {
            // when grammarFilePath is null then the model should be empty and is available, or? so reture true must be ok
            return true;
    	}
    	return MODELS.contains(new File(grammarFilePath).getAbsolutePath());
    }

    public static boolean isModelAlreadyBuild(File grammarFile) {
    	if (grammarFile == null) {
            // when grammarFilePath is null then the model should be empty and is available, or? so reture true must be ok
            return true;
    	}
    	return MODELS.contains(grammarFile.getAbsolutePath());
    }

    private static final ErrorReceiver ERR = new ErrorReceiver() {
        @Override
        public void error(SAXParseException saxpe) throws AbortException {
        	LOG.error(saxpe.getMessage(), saxpe);
        }

        @Override
        public void fatalError(SAXParseException saxpe) throws AbortException {
        	LOG.error(saxpe.getMessage(), saxpe);
        }

        @Override
        public void warning(SAXParseException saxpe) throws AbortException {
            LOG.warn( saxpe.getMessage(), saxpe);
        }

        @Override
        public void info(SAXParseException saxpe) {
            LOG.info(saxpe.getMessage(), saxpe);
        }
    };

    private final XJCOptions opt;
    @SuppressWarnings("unused")
	private final ErrorReceiverFilter errorReceiver;
    private final JCodeModel codeModel;

    public ModelBuilder(XJCOptions _opt) {
        this(_opt, null);
    }

    public ModelBuilder(XJCOptions _opt, JCodeModel _codeModel) {
        this.opt = _opt;
        opt.pluginURIs.add( InlineSchemaPlugin.PNS.getNamespaceURI() );
        opt.activePlugins.add( new InlineSchemaPlugin() );
        codeModel = (_codeModel != null ? _codeModel : new JCodeModel());
        if (opt.compatibilityMode != 2) {
            LOG.warn(Messages.format(Messages.COMPATIBILITY_REQUIRED, ""));
            opt.compatibilityMode = 2;
        }
        // @FIXME:aufräumen
        opt.strictCheck = false;
        opt.noFileHeader = true;
        opt.enableIntrospection = true;
        opt.debugMode = true;
        this.errorReceiver = new ErrorReceiverFilter(ERR);
    }

    /**
     * @param _opt
     * @param model
     * @throws Exception
     * @deprecated: model is not used any more
     */
    public static void generate(XJCOptions _opt, JCodeModel model) throws Exception {
        new ModelBuilder(_opt).generate();
    }

    public static void generate(XJCOptions _opt) throws Exception {
        new ModelBuilder(_opt).generate();
    }

    private boolean testUpdateRequired(){
        if (opt.forceGenerate || opt.targetDir == null || !opt.targetDir.exists() ) {
            return true;
        }
        if (opt.createJar) {
            if ( opt.jarFilePath==null) {
                LOG.debug("No Model found, build is required.");
                return true;
            }
            final File jar=new File(opt.jarFilePath);

            if (!jar.exists() || jar.lastModified() < opt.newestGrammar) {
                LOG.debug("Grammar is newer than generated Model, build is required.");
                return true;
            }
        } else {
            final List<File> listFiles = listFiles(opt.targetDir, true, "TXMLBinding");
            if (listFiles.isEmpty()) {
                LOG.debug("No Model found, build is required.");
                return true;
            }
            for (File f : listFiles) {
                if (f.lastModified()<opt.newestGrammar) {
                    LOG.debug("Grammar is newer than generated Model, build is required.");
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized boolean compile(boolean extendClasspath) {
//        final String classNotFound = "package de.cimt.talendcomp.xmldynamic does not exist";
        JavaCompiler jc;
        try {
            jc = ToolProvider.getSystemJavaCompiler();
        } catch (Throwable t) { // may throw an exception when jre is used
            jc = null;
        }

        if (jc == null) {
            String message = "Cannot access the javac compiler. Take care you use a JDK instead of a JRE.\n"
                    + "java.home: " + System.getProperty("java.home") + "\n"
                    + "java.class.path: " + System.getProperty("java.class.path");
            LOG.error(message);
            throw new IllegalStateException(message);
        }

        DiagnosticListener dl = new DiagnosticListener() {
            @Override
            public void report(Diagnostic diagnostic) {
                String msg = diagnostic.getMessage(Locale.getDefault()) + "(" + diagnostic.getSource()
                        + ((diagnostic.getPosition() != Diagnostic.NOPOS) ? ("[Line " + diagnostic.getLineNumber() + ", Col " + diagnostic.getColumnNumber() + "]") : "");

                switch (diagnostic.getKind()) {
                    case ERROR:
                    	LOG.error(msg);
                        break;
                    case WARNING:
                        LOG.warn(msg);
                        break;
                    default:
                        LOG.info(msg);
                }
            }
        };
        StandardJavaFileManager sjfm = jc.getStandardFileManager(dl, null, null);
        if(extendClasspath){
            final ArrayList<File> files = new ArrayList<File>();

            Arrays.stream(System.getProperty("java.class.path").split(System.getProperty("os.name").toUpperCase().startsWith("WINDOWS ") ? ";" : ":")).forEach(f -> {
                try {
                    files.add(new File(f));
                    if (f.toUpperCase().endsWith(".JAR")) {
                        String cp = new JarFile(f).getManifest().getMainAttributes().getValue("Class-Path").replaceAll("\n\\s", "");

                        StringTokenizer stok = new StringTokenizer(cp, " \'\"", true);
                        while (stok.hasMoreTokens()) {
                            final String starttok = stok.nextToken();
                            if (starttok.equals(" ")) {
                                continue;
                            }

                            if (starttok.equals("\'") || starttok.equals("\"")) {
                                StringBuilder value = new StringBuilder();
                                String curTok;
                                while (!(curTok = stok.nextToken()).equals(starttok)) {
                                    value.append(curTok);
                                }

                                files.add(new File(value.toString()));
                            } else {
                                files.add(new File(starttok));
                            }
                        }
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });

            try {
                sjfm.setLocation(StandardLocation.CLASS_PATH, files);
            } catch (IOException ex) {
            	LOG.error("error extending classpath");
                return false;
            }
        }
//                }
        return jc.getTask(null, sjfm, dl, null, null, sjfm.getJavaFileObjectsFromFiles(listFiles(opt.targetDir, true, ".java"))).call();
//        {
//            return boolean;
//            throw new Exception(Messages.COMPILATION_FAILED);
//        }

    }
    /**
     * generates code model to java sources, compiles classes and extends current
     * system classloader
     *
     * @throws Exception
     */
    public synchronized void generate() throws Exception {
    	if(Util.isOsgi())
    	{
    		String message = "Model generation ist not available in OSGi environment";
    		LOG.error(message);
    		throw new Exception(message);
    	}
        if (!MODELS.contains(opt.grammarFilePath)) {
            LOG.info("Generate Model using Plugin Version "+ XJCOptions.VERSION + "("+XJCOptions.LASTUPDATE+")" );
            if (testUpdateRequired()) {
                setupModelDir(opt.targetDir);
                Model model = ModelLoader.load(opt, codeModel, ERR);
                Outline ouln = model.generateCode(opt, ERR);
                if (ouln == null) {
                    throw new Exception("failed to compile a schema");
                }
                if (opt.checksum) {
                    for (PackageOutline co : ouln.getAllPackageContexts()) {
                        JClass jc = model.codeModel.directClass("de.cimt.talendcomp.xmldynamic.Checksum");

                        co.objectFactory().annotate(jc).param("key", opt.checksumValue);
                    }
                }
                if (opt.targetDir == null) {
                    opt.targetDir = createTemporaryFolder();
                }
                if (LOG.isDebugEnabled() ) {
                    LOG.debug("Output folder for generated classes: " + opt.targetDir.getAbsolutePath());
                }
                if (opt.targetDir.exists() == false) {
                    opt.targetDir.mkdirs();
                }
                if (opt.targetDir.exists() == false) {
                    throw new Exception("Cannot create/use target folder: " + opt.targetDir);
                }
                model.codeModel.build( new FileCodeWriter(opt.targetDir) );
                if (!opt.compileSource) {
                    return;
                }

//                // fix missing classpath in azul jdk
//                LOG.fatal( System.getProperty( "java.class.path" ) );
//                List<JarFile> classpathentries= Arrays.stream(System.getProperty("java.class.path").split(System.getProperty("os.name").toUpperCase().startsWith("WINDOWS ") ? ";" : ":")).map(f -> {
//                    try {
//                        return new JarFile(f);
//                    } catch (IOException ex) {
//                        throw new RuntimeException(ex);
//                    }
//                } ).collect(Collectors.toList());
//
                if(!compile(false))
                    if(!compile(true))
                        throw new Exception(Messages.COMPILATION_FAILED);


                if(opt.createJar){
                    // TODO: there is no check if option jarFilePath is set an valid
                    JarUtil jarBuilder = new JarUtil();
                    jarBuilder.setJarFilePath( opt.jarFilePath );
                    jarBuilder.setGrammarFilePath( opt.grammarFilePath );
                    jarBuilder.setClassFilesRootDir(opt.targetDir.getAbsolutePath() );
                    jarBuilder.create();
                }
            }

            MODELS.add(opt.grammarFilePath);
            if (!opt.extendClasspath) {
                return;
            }
            URI uri= (opt.createJar && opt.jarFilePath!=null) ? new File(opt.jarFilePath).toURI() : opt.targetDir.toURI();


            LOG.info("extend Classpath using " + ( (opt.createJar && opt.jarFilePath!=null) ? opt.jarFilePath : opt.targetDir) );
            Util.register(uri, (opt.createJar && opt.jarFilePath!=null) );
//
//            Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
//
//            method.setAccessible(true);
//            try{
//                method.invoke((URLClassLoader) ClassLoader.getSystemClassLoader(), new Object[]{uri.toURL()});
//            }catch(ClassCastException cce){
//                final String name = ClassLoader.getSystemClassLoader().getClass().getName();
//                if(name.contains("osgi") || name.contains("ModuleClassLoader") ){
////                    ((ModuleClassLoader) ClassLoader.getSystemClassLoader())
//                }
////                if("osgi")
//            }
//            URLClassLoader.newInstance(urls, parent)
    	} else {
            LOG.debug("Model for schema file: " + opt.grammarFilePath + " already generated, skip generate step.");
    	}

    }

    public static List<File> listFiles(File root, boolean recursive, String extension) {
        List<File> files = new ArrayList<File>();
        for (File f : root.listFiles()) {
            if (f.isDirectory() && recursive) {
                files.addAll(listFiles(f, recursive, extension));
            } else if (extension == null ||  f.getName().toLowerCase().endsWith(extension.toLowerCase())) {
                files.add(f);
            }
        }
        return files;
    }

    public static File createTemporaryFolder() throws IOException {
        File f = File.createTempFile("de.cimt.talendcomp.xmldynamic", "temp");
        File tf = new File(f.getParent(), UUID.randomUUID().toString().replaceAll("[\\.:-]+", ""));
        f.delete();
        tf.mkdirs();
        tf.deleteOnExit();
        return tf;
    }

	private static void delete(File f) throws IOException {
		if (f.isDirectory()) {
			for (File c : f.listFiles()) {
				delete(c);
			}
		}
		if (f.delete() == false) {
			throw new IOException("Failed to delete file: " + f.getAbsolutePath());
		}
	}

    private static File setupModelDir(File modelDir) throws Exception {
        if (modelDir.exists()) {
            if (modelDir.isFile() && !modelDir.delete()) {
                throw new Exception("At the location of the model dir a file already exists: " + modelDir.getAbsolutePath() + " and this cannot be deleted!");
            }
            delete(modelDir); // more simple approach to delete a filled directory
        }
        modelDir.mkdirs();
        if (modelDir.exists() == false) {
            throw new Exception("Cannot create model base dir: " + modelDir.getAbsolutePath());
        }
        return modelDir;
    }

    public static void info(String message) {
    	LOG.info(message);
    }

    public static void debug(String message) {
    	LOG.debug(message);
    }

}
