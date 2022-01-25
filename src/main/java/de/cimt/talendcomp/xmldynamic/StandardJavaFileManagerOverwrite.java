package de.cimt.talendcomp.xmldynamic;

import org.apache.log4j.Logger;

import javax.tools.*;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
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
    private Map<String, List<String>> clazzes=null;

    private List<URI> listClasses(File folder){
        List<URI> classes=new ArrayList<URI>();
        for( File path : folder.listFiles()){
            if(path.isDirectory()){
                classes.addAll( listClasses(path));
            } else if (path.getName().toLowerCase().endsWith(".class")){
                classes.add(path.toURI());
            }
        }
        return classes;
    }
    
    public Map<String, List<String>> getClasses() throws IOException {
        if(clazzes!=null)
            return clazzes;
        
        clazzes=new HashMap<String, List<String>>(){
            @Override
            public List<String> get(Object key) {
                if(!super.containsKey(key))
                    super.put((String) key, new ArrayList<String>());
                return super.get(key);  
            }
        };
        
        if(InlineJavaFileObject.packed){
            ZipInputStream zip = new ZipInputStream(new FileInputStream( new File(InlineJavaFileObject.location) ));
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if(!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    String clazzName=entry.getName();
                    String packageName="";
                    final int pos=clazzName.lastIndexOf("/");
                    
                    if(pos>0)
                        packageName=clazzName.substring(0,pos).replace('/', '.');
                    
                    clazzes.get(packageName).add(clazzName);
                }
            }
        }else{
            for(URI uri : listClasses( new File(InlineJavaFileObject.location) )){
                    final String clazzName=InlineJavaFileObject.location.relativize(uri).toString();
                    String packageName="";
                    final int pos=clazzName.lastIndexOf("/");
                    
                    if(pos>0)
                        packageName=clazzName.substring(0,pos).replace('/', '.');
                    
                    clazzes.get(packageName).add(clazzName);
            }
        }
        
        return clazzes;
    }
    
    private Iterable< JavaFileObject> getInternalClasses(String packageName) throws IOException{
        return getClasses().get(packageName).stream()
                .map( c -> new InlineJavaFileObject(c) )
                .collect( Collectors.toList() );
    }


    /**
     *
     * @param manager
     */
    public StandardJavaFileManagerOverwrite(StandardJavaFileManager manager) {
        super(manager);
    }
 
    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
        if( location!= StandardLocation.CLASS_PATH || !packageName.startsWith("de.cimt.talendcomp.xmldynamic") ) {
            return super.list(location, packageName, kinds, recurse);
        }

        Iterable<JavaFileObject> i=super.list(location, packageName, kinds, recurse);
        final AtomicInteger cnt=new AtomicInteger();
        i.forEach(e -> cnt.incrementAndGet());

        if(cnt.get() != 0){
            return i;
        }

        LOG.info("internal package "+packageName+" without classes!");
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
}