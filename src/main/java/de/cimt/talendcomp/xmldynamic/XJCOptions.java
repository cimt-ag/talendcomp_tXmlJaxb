package de.cimt.talendcomp.xmldynamic;

import de.cimt.talendcomp.xmldynamic.plugins.InlineSchemaPlugin;
import java.io.File;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.colllib.datastruct.Pair;
import org.colllib.filter.Filter;
import org.colllib.util.CollectionUtil;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.sun.tools.xjc.Options;
import de.cimt.talendcomp.xmldynamic.filter.ChecksumFilter;

import de.cimt.talendcomp.xmldynamic.filter.DependencyFilter;
import de.cimt.talendcomp.xmldynamic.filter.GraphFilter;
import de.cimt.talendcomp.xmldynamic.filter.PluginFilter;
import de.cimt.talendcomp.xmldynamic.filter.PrintingFilter;
import de.cimt.talendcomp.xmldynamic.filter.RemoveAnnotationsFilter;
import de.cimt.talendcomp.xmldynamic.filter.RootElementFilter;
import de.cimt.talendcomp.xmldynamic.filter.TypeReadHandler;
import de.cimt.talendcomp.xmldynamic.filter.WSDLSchemaFilter;
import de.cimt.talendcomp.xmldynamic.filter.XMLFilterChain;
import de.cimt.talendcomp.xmldynamic.plugins.VisualisationPlugin;
import java.io.FileInputStream;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import javax.xml.namespace.QName;
import javax.xml.transform.Transformer;

/**
 *
 * @author dkoch
 */
public class XJCOptions extends Options {

    private static final Logger LOG = Logger.getLogger(XJCOptions.class.getName());
    public boolean extendClasspath = true;
    public boolean compileSource = true;
    public boolean casesensitive = false;
    public boolean createGraph = false; // paints the structure
    public boolean addJavadocs = true;  // add javadoc for newly generated model
    public boolean addModeldocs = true;  // add element documentation for newly generated model - see VisualisationPlugin
    private AtomicInteger grammarCount=new AtomicInteger(0);
    
    public boolean ignoreAnnotations = false;
    public boolean enableBasicSubstitution  = false; // replaces some data types with more usual data types, for example time, date etc
    public boolean checksum = false;
    public boolean forceGenerate = false;
    public boolean printGrammar = false;
    public boolean createJar = false;
    public String  jarFilePath = null;
    public String  checksumValue = "";
    public String  grammarFilePath = null;
    public long    newestGrammar = 0l;
    public static final String VERSION;
    public static final String LASTUPDATE;
    private List<QName> rootElements=new ArrayList<QName>();
    
    static {
	/**
	 * compute version numnber and date of component from manifest to be 
	 * displaed while generating model
	 */
        String versionString="unknown";
        String d="";
        try {
            JarInputStream jis=new JarInputStream(XJCOptions.class.getProtectionDomain().getCodeSource().getLocation().openStream());
            final Manifest manifest = jis.getManifest();
            try {
                Date date=new SimpleDateFormat("yyyyMMdd-HHmm").parse( manifest.getMainAttributes().getValue("Implementation-Timestamp") );
                d=DateFormat.getDateInstance(2, Locale.getDefault()).format(date);
            } catch (Throwable t) {}
            versionString=manifest.getMainAttributes().getValue("Implementation-Version");
        } catch (Throwable t) {}
        VERSION   =versionString;
        LASTUPDATE=d;
    }
    
    // stores relations between source and alias
    private final Map<String, String> grammarCache = new HashMap<String, String>();
    
    // temporary directory used to store modified grammars
    private final List<Pair<String, String>> _complexTypes = new ArrayList<Pair<String, String>>();
    private final List<Pair<String, String>> _simpleTypes = new ArrayList<Pair<String, String>>();
    private final Map<Pair<String, String>, AtomicInteger> _usageCount = new HashMap<Pair<String, String>, AtomicInteger>() {
        private static final long serialVersionUID = 1L;

        @SuppressWarnings("unchecked")
        @Override
        public AtomicInteger get(Object key) {
            AtomicInteger val = super.get(key);
            if (val == null) {
                val = new AtomicInteger(0);
                put((Pair<String, String>) key, val);
            }
            return val;
        }
    };
    
    /**
     * used to activate printing of manipulated grammars before generating code model
     */
    public XJCOptions() {
        super();
        pluginURIs.add(InlineSchemaPlugin.PNS.getNamespaceURI());
        this.activePlugins.add( new InlineSchemaPlugin() );
	
        pluginURIs.add(VisualisationPlugin.PNS.getNamespaceURI());
        this.activePlugins.add( new VisualisationPlugin() );
	
        strictCheck = false;
        noFileHeader = true;
        compatibilityMode = 2;
        enableIntrospection = true;
        verbose = true;
    }
    
    private Set<Pair<String, String>> getComplexTypes() {
	
        return CollectionUtil.filterMap(_usageCount, new Filter<Pair<String, String>>() {
                @Override
                public boolean matches(Pair<String, String> t) {
                    return _complexTypes.contains(t) && !_simpleTypes.contains(t);
                }
            },
            new Filter<AtomicInteger>() {
                @Override
                public boolean matches(AtomicInteger t) {
                    return t.get() > 1;
                }
            }).keySet();
    }
    
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    @Override
    public synchronized void addGrammar(final InputSource source) throws RuntimeException {
        /**
         * Changed behavior as this method never send the original inputsource 
         * to the superclass. 
         * It uses a set of filters to pre-process the original input and to parse 
         * it as one or multiple expands sources to a in-memory-source instance.
         * this one will be transferred to the superclass.
         */
        try {
            
            URI rootURI;
            try {
                rootURI = new URI(source.getSystemId());
            } catch (java.net.URISyntaxException use) {
                rootURI = new File(source.getSystemId()).toURI();
            }
	    if(grammarCache.containsKey(rootURI.toString())){
		return;
	    }
	    
            String alias = "grammar" + String.format("%04d", grammarCount.incrementAndGet() ) + ".xsd";
            XMLFilterChain chain = new XMLFilterChain();
            if (source.getSystemId().toLowerCase().endsWith(".wsdl")) {
                WSDLSchemaFilter wsdlfilter = new WSDLSchemaFilter() {
                    @Override
                    public synchronized String createInputSource(String xmlbuffer) {
                        final String id = Util.uniqueString() + ".xsd";
                        addGrammar( new InMemorySource(xmlbuffer, id) );
                        return id;
                    }

                };
                chain.add(wsdlfilter);
            }
            if (source.getClass().equals(InMemorySource.class)) {
                alias = ((InMemorySource) source).alias;
            } 
            
            chain.add( new TypeReadHandler(){
                @Override
                public int incrementUsageCount(Pair<String, String> type) {
                    return _usageCount.get(type).incrementAndGet();
                }

                @Override
                public void registerComplexType(Pair<String, String> complexType) {
                    _complexTypes.add(complexType);
                }
		
                @Override
                public void registerSimpleType(Pair<String, String> complexType) {
                    _simpleTypes.add(complexType);
                }
            });
            
            RootElementFilter rif=new RootElementFilter();
            chain.add( rif );
            DependencyFilter df = new DependencyFilter(rootURI) {
                
                @Override
                protected synchronized String getRelocatedSchemaLocation(URI root, String location) {
                    if (location == null || location.length() == 0) {
                        return null;
                    }
                    try {
                        URI nestedUri = new URI(location);

                        if (!nestedUri.isAbsolute()) {
                            // memory sources are already analyzed...
                            if (root.getScheme().equalsIgnoreCase("mem") && grammarCache.containsValue(location) ) {
                                return location;
                            }
                            nestedUri = root.resolve(nestedUri);
                        }
                        String systemID = nestedUri.toString();
                                
                        // only not handled sources must be analyzed...
                        if (!grammarCache.containsKey(systemID)) {
                            InputSource source = new InputSource(systemID);
                            source.setSystemId(systemID);
                            addGrammar(source);
                        }
                        return grammarCache.get(systemID);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
            chain.add(df);

            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            spf.setXIncludeAware(true);

            XMLReader reader = new XMLFilteredReader(
                    spf.newSAXParser().getXMLReader(),
                    chain
            );

            StringWriter w = new StringWriter();
            TransformerFactory.newInstance().newTransformer().transform(new SAXSource(reader, source), new StreamResult(w));

            InMemorySource ims = new InMemorySource(w.toString(), alias) ;
            if (ims.isEmtpy()) {
                return;
            }
            grammarCache.put(rootURI.toString(), alias);

            rootElements.addAll( rif.getRootElements() );
            super.addGrammar( new InMemorySource(w.toString(), alias) );
        } catch (Throwable ex) {
            LOG.error(Messages.format("PRELOAD.ANALYSE.FAILED"), ex);
            throw new RuntimeException(Messages.format("PRELOAD.ANALYSE.FAILED"), ex);
        }
    }
    
    /**
     * Input schema files.
     */
    @Override
    public synchronized InputSource[] getGrammars() {
        try {
            XMLFilterChain chain = new XMLFilterChain();//ew WSDLSchemaFilter() ,);
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            spf.setXIncludeAware(true);
            
            final Set<Pair<String, String>> typeBindings = getComplexTypes();
            PluginFilter pluginFilter = new PluginFilter() {
                @Override
                public boolean testManipulationRequired(Pair<String, String> fqtype) {
                    return typeBindings.contains(fqtype);
                }

                @Override
                public boolean isRootelement(Pair<String, String> fqtype) {
//                    LOG.trace("test rootelement {"+fqtype.x+"}:"+ fqtype.y);
                    return rootElements.contains( new QName(fqtype.x, fqtype.y) );
                }
                
            };
            if (createGraph) {
                chain.add(new GraphFilter());
            }
            
            if (ignoreAnnotations) {
            	chain.add(new RemoveAnnotationsFilter());
            }
            chain.add(pluginFilter);
            ChecksumFilter csf = new ChecksumFilter();
            chain.add(csf);
            if (printGrammar) {
                chain.add(new PrintingFilter());
            }

            List<InputSource> ng = new ArrayList<InputSource>();
            XMLFilteredReader reader = new XMLFilteredReader(spf.newSAXParser().getXMLReader(), chain);
            final Transformer transformer = TransformerFactory.newInstance().newTransformer();
	    
	    /**
	     * generate grammar to targetDir to be shipped with jar. so it's more 
	     * easy to create a common schema validator and to find any problem
	     * with the model
	     */
	    File schemaExportDir=new File(targetDir + "/META-INF/grammar"  );
	    schemaExportDir.mkdirs();
	    
            for (InputSource source : super.getGrammars()) {
		File res = new File( schemaExportDir , ((InMemorySource) source).alias);
		
	
                if (LOG.isDebugEnabled()) {
                	LOG.debug("Use as temporary xsd result file: " + res.getAbsolutePath());
                }
                transformer.transform(
                    new SAXSource(reader, source),
                    new StreamResult(res)
                );
                InputSource exportedGrammar = new InputSource(new FileInputStream(res));
                exportedGrammar.setSystemId( res.toURI().toString() );
                ng.add( exportedGrammar );
            }
            
            checksumValue = csf.toString();
            return ng.toArray( new InputSource[ng.size()] );
        } catch (Exception ex) {
            LOG.error(ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * Recursively scan directories and add all XSD files in it.
     * @param dir folder or file 
     */
    @Override
    public void addGrammarRecursive(File dir) {
        if (dir == null || !dir.exists() || !dir.canRead()) {
            return;
        }
        if (dir.isFile()) {
            addGrammar(dir);
            return;
        }
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                addGrammarRecursive(f);
            } else if (f.getName().toLowerCase().endsWith(".xsd") || f.getName().toLowerCase().endsWith(".xsd")) {
                addGrammar(f);
            }
        }
        this.grammarFilePath = dir.getAbsolutePath();
    }

    @Override
    public InputSource[] getBindFiles() {
        InputSource[] bindfiles = super.getBindFiles();
        if (!enableBasicSubstitution) {
            return bindfiles;
        }
        /**
         * when enableBasicSubstitution is set, activate replacement for xsd:duration, 
         * xsd:date, xsd:dateTime and enable plugin xjc-simple
         */
        InputSource bind = new InputSource(XJCOptions.class.getResourceAsStream("bindings.xml"));
        bind.setSystemId(XJCOptions.class.getResource("bindings.xml").toString());
        InputSource[] allbindfiles = (InputSource[]) Array.newInstance(InputSource.class, bindfiles.length + 1);
        System.arraycopy(bindfiles, 0, allbindfiles, 1, bindfiles.length);
        allbindfiles[0] = bind;
        return allbindfiles;
    }

    @Override
    public void addGrammar(File source) {
            if (source == null) {
                    throw new IllegalArgumentException("source file must not be null");
            }
            this.grammarFilePath = source.getAbsolutePath();
            super.addGrammar(source);

            if(source.lastModified()>newestGrammar){
                newestGrammar=source.lastModified();
            }
    }

    
}
