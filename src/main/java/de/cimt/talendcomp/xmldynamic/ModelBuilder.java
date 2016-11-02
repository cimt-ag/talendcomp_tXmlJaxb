package de.cimt.talendcomp.xmldynamic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.log4j.Logger;
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
import java.net.URI;

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

    private static final Logger LOG = Logger.getLogger("de.cimt.talendcomp.xmldynamic");
    private static final Set<String> models = new HashSet<String>();
    public static final Object lock = new Object(); 
    
    public static boolean isModelAlreadyBuild(String grammarFilePath) {
    	if (grammarFilePath == null) {
            // when grammarFilePath is null then the model should be empty and is available, or? so reture true must be ok
            return true;
    	}
    	return models.contains(new File(grammarFilePath).getAbsolutePath());
    }
    
    public static boolean isModelAlreadyBuild(File grammarFile) {
    	if (grammarFile == null) {
            // when grammarFilePath is null then the model should be empty and is available, or? so reture true must be ok
            return true;
    	}
    	return models.contains(grammarFile.getAbsolutePath());
    }

    private static final ErrorReceiver ERR = new ErrorReceiver() {
        @Override
        public void error(SAXParseException saxpe) throws AbortException {
            LOG.error(saxpe.getMessage(), saxpe);
        }

        @Override
        public void fatalError(SAXParseException saxpe) throws AbortException {
            LOG.fatal(saxpe.getMessage(), saxpe);
        }

        @Override
        public void warning(SAXParseException saxpe) throws AbortException {
            LOG.warn(saxpe.getMessage(), saxpe);
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
                return true;
            }
            final File jar=new File(opt.jarFilePath);
            
            if (!jar.exists() || jar.lastModified() < opt.newestGrammar) {
                return true;
            }
        } else {
            final List<File> listFiles = listFiles(opt.targetDir, true, "TXMLBinding");    
            if (listFiles.isEmpty()) {
                return true;
            }
            for (File f : listFiles) {
                if (f.lastModified()<opt.newestGrammar) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * generates code model to java sources, compiles classes and extends current
     * system classloader
     *
     * @throws Exception
     */
    public synchronized void generate() throws Exception {
                
        if (!models.contains(opt.grammarFilePath)) {

            if (testUpdateRequired()) { 
                LOG.info("Generate Model using Plugin Version "+ opt.VERSION + "("+opt.LASTUPDATE+")" );
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
                if (LOG.isDebugEnabled()) {
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
                JavaCompiler jc;
                try{
                    jc = ToolProvider.getSystemJavaCompiler();
                } catch(Throwable t){ // may throw an exception when jre is used
                    jc = null;
                }

                if (jc == null) {
                    String message = "Cannot access the javac compiler. Take care you use a JDK instead of a JRE.\n"
                            + "java.home: " + System.getProperty("java.home") + "\n"
                            + "java.class.path: " + System.getProperty("java.class.path");
                    LOG.error(message);
                    throw new IllegalStateException( message );
                }
                StandardJavaFileManager sjfm = jc.getStandardFileManager(null, null, null);
                if (!jc.getTask(null, sjfm, null, null, null, sjfm.getJavaFileObjectsFromFiles(listFiles(opt.targetDir, true, ".java"))).call()) {
                    throw new Exception(Messages.COMPILATION_FAILED);
                }
                if(opt.createJar){
                    // TODO: there is no check if option jarFilePath is set an valid
                    JarUtil jarBuilder = new JarUtil();
                    jarBuilder.setJarFilePath( opt.jarFilePath );
                    jarBuilder.setGrammarFilePath( opt.grammarFilePath );
                    jarBuilder.setClassFilesRootDir(opt.targetDir.getAbsolutePath() );
                    jarBuilder.create();
                }
            }
            
            models.add(opt.grammarFilePath);
            if (!opt.extendClasspath) {
                return;
            }
            URI uri= (opt.createJar && opt.jarFilePath!=null) ? new File(opt.jarFilePath).toURI() : opt.targetDir.toURI();
            
            
            LOG.warn("extend Classpath using " + ( (opt.createJar && opt.jarFilePath!=null) ? opt.jarFilePath : opt.targetDir) );
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
            } else if (extension == null || (extension != null && f.getName().toLowerCase().endsWith(extension.toLowerCase()))) {
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
