package de.cimt.talendcomp.xmldynamic;

import org.apache.log4j.Logger;

import javax.tools.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class StandardJavaFileManagerOverwrite extends ForwardingJavaFileManager<StandardJavaFileManager> implements StandardJavaFileManager {
    private static final Logger LOG = Logger.getLogger("de.cimt.talendcomp.xmldynamic");
    //private final URL packageLocation;
    //private final boolean packed;

    private List<String> listEntries(File jar) throws IOException {
        List<String> classNames = new ArrayList<String>();
        ZipInputStream zip = new ZipInputStream(new FileInputStream( jar ));
        for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
            if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                classNames.add(entry.getName());
            }
        }
        return classNames;
    }
    private Iterable< JavaFileObject> getInternalClasses(String packageName){
        final URL packageLocation= this.getClass().getProtectionDomain().getCodeSource().getLocation();
        boolean isPackaged=false;
        File file=null;
        try {
            isPackaged =  new File( packageLocation.toURI() ).isFile();
            LOG.fatal( new File( packageLocation.toURI() ).getAbsolutePath() );

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        final boolean packaged=isPackaged;
        String [] classes=new String[]{};

        if(packageName.equals("de.cimt.talendcomp.xmldynamic")){
            classes = new String[]{
                    "de/cimt/talendcomp/xmldynamic/ExtPropertyAccessor.class",
                    "de/cimt/talendcomp/xmldynamic/InlineSchemaPlugin$1.class",
                    "de/cimt/talendcomp/xmldynamic/InlineSchemaPlugin$2.class",
                    "de/cimt/talendcomp/xmldynamic/InlineSchemaPlugin$3.class",
                    "de/cimt/talendcomp/xmldynamic/InlineSchemaPlugin$4.class",
                    "de/cimt/talendcomp/xmldynamic/InlineSchemaPlugin.class",
                    "de/cimt/talendcomp/xmldynamic/InMemorySource.class",
                    "de/cimt/talendcomp/xmldynamic/JarUtil.class",
                    "de/cimt/talendcomp/xmldynamic/Messages.class",
                    "de/cimt/talendcomp/xmldynamic/ModelBuilder$1.class",
                    "de/cimt/talendcomp/xmldynamic/ModelBuilder.class",
                    "de/cimt/talendcomp/xmldynamic/ReflectUtil$1.class",
                    "de/cimt/talendcomp/xmldynamic/ReflectUtil$2.class",
                    "de/cimt/talendcomp/xmldynamic/ReflectUtil$3.class",
                    "de/cimt/talendcomp/xmldynamic/ReflectUtil.class",
                    "de/cimt/talendcomp/xmldynamic/StandardJavaFileManagerOverwrite.class",
                    "de/cimt/talendcomp/xmldynamic/TXMLBinding.class",
                    "de/cimt/talendcomp/xmldynamic/TXMLObject$1$1.class",
                    "de/cimt/talendcomp/xmldynamic/TXMLObject$1.class",
                    "de/cimt/talendcomp/xmldynamic/TXMLObject$2.class",
                    "de/cimt/talendcomp/xmldynamic/TXMLObject$3.class",
                    "de/cimt/talendcomp/xmldynamic/TXMLObject$4.class",
                    "de/cimt/talendcomp/xmldynamic/TXMLObject$MissingAttribute.class",
                    "de/cimt/talendcomp/xmldynamic/TXMLObject.class",
                    "de/cimt/talendcomp/xmldynamic/TXMLStream$1.class",
                    "de/cimt/talendcomp/xmldynamic/TXMLStream$XMLStreamWriter.class",
                    "de/cimt/talendcomp/xmldynamic/TXMLStream.class",
                    "de/cimt/talendcomp/xmldynamic/Util$OSGIClassLoader.class",
                    "de/cimt/talendcomp/xmldynamic/Util.class",
                    "de/cimt/talendcomp/xmldynamic/XJCOptions$1.class",
                    "de/cimt/talendcomp/xmldynamic/XJCOptions$2.class",
                    "de/cimt/talendcomp/xmldynamic/XJCOptions$3.class",
                    "de/cimt/talendcomp/xmldynamic/XJCOptions$4.class",
                    "de/cimt/talendcomp/xmldynamic/XJCOptions$5.class",
                    "de/cimt/talendcomp/xmldynamic/XJCOptions$6.class",
                     "de/cimt/talendcomp/xmldynamic/XJCOptions$7.class",
                     "de/cimt/talendcomp/xmldynamic/XJCOptions.class",
                    "de/cimt/talendcomp/xmldynamic/XMLFilteredReader.class"
            };
        } else if(packageName.equals("de.cimt.talendcomp.xmldynamic.annotations")){
            classes = new String[]{
                    "de/cimt/talendcomp/xmldynamic/annotations/Checksum.class",
                    "de/cimt/talendcomp/xmldynamic/annotations/Jetcode.class",
                    "de/cimt/talendcomp/xmldynamic/annotations/QNameRef.class",
                    "de/cimt/talendcomp/xmldynamic/annotations/TXMLTypeHelper.class"
            };
        }

        class InlineJavaFileObject extends javax.tools.SimpleJavaFileObject implements javax.tools.JavaFileObject{

            InlineJavaFileObject(URI u){
                super(u, JavaFileObject.Kind.CLASS);
            }

        }
        return Arrays.asList(classes)
                .stream()
                .map( c -> {
                    try {
                        URI nu=new URI(packageLocation.toString() + (packaged ? "!/" : "") + c);
                        LOG.warn("register " + nu);
                        return new InlineJavaFileObject(nu);
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .filter( fo -> fo!=null )
                .collect( Collectors.toList() );
    }


    /**
     *
     * @param manager
     */
    public StandardJavaFileManagerOverwrite(StandardJavaFileManager manager) {
        super(manager);
    }





/*
    @Override
    public ClassLoader getClassLoader(Location location) {
        LOG.warn( "get ClassLoader for " + location.getName() );
        ClassLoader cl=super.getClassLoader(location);
        LOG.info("--inline classloader------------------");


        return cl;
    }
*/
    //@Override
    public Iterable<JavaFileObject> list1(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
        LOG.warn("list( " + location + ", " + packageName + ", " + kinds.stream().map(k -> k.name()).collect(Collectors.toList()) + ", " + recurse + " )");
        return super.list(location, packageName, kinds, recurse);
    }

    public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
        LOG.warn("list( " + location + ", " + packageName + ", " + kinds.stream().map(k -> k.name()).collect(Collectors.toList()) + ", " + recurse + " )");
        if( location!= StandardLocation.CLASS_PATH || !packageName.startsWith("de.cimt.talendcomp.xmldynamic") ) {
            return super.list(location, packageName, kinds, recurse);
        }

//            return super.list(location, packageName, kinds, recurse);;

        Iterable<JavaFileObject> i=super.list(location, packageName, kinds, recurse);
        //SimpleJavaFileObject
        //list( CLASS_PATH, de.cimt.talendcomp, [SOURCE, CLASS], false )


        LOG.warn("packageName =" + packageName );
            StringBuffer b = new StringBuffer();
            final AtomicInteger cnt=new AtomicInteger();
            i.forEach(e -> {
                b.append("\t").append(e.getName()).append(" - ").append(e.getClass()).append(e.getKind().name()).append("\n");
                cnt.incrementAndGet();
            });

        if(LOG.isDebugEnabled())

            LOG.warn( packageName + " found elements:\n" + b.toString());


        if(cnt.get() != 0){
            return i;
        }

        LOG.error("package "+packageName+" without elements!");
        return getInternalClasses(packageName);

    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(Iterable<? extends File> files) {
        return fileManager.getJavaFileObjectsFromFiles(files);
    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjects(File... files) {
        return fileManager.getJavaFileObjects(files);
    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(Iterable<String> names) {
        return fileManager.getJavaFileObjectsFromStrings(names);
    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjects(String... names) {
        return fileManager.getJavaFileObjects(names);
    }

    @Override
    public void setLocation(Location location, Iterable<? extends File> path) throws IOException {
        fileManager.setLocation(location, path);
    }

    @Override
    public Iterable<? extends File> getLocation(Location location) {
        return fileManager.getLocation(location);
    }

    /**
     * @throws IllegalStateException {@inheritDoc}
     */
    public String inferBinaryName(Location location, JavaFileObject file) {
        try{
            return super.inferBinaryName(location, file);
        }catch(Throwable t){
            return super.inferBinaryName(location, file);

        }
    }

}