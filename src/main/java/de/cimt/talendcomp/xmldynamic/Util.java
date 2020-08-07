package de.cimt.talendcomp.xmldynamic;

import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import de.cimt.talendcomp.xmldynamic.annotations.Jetcode;
import de.cimt.talendcomp.xmldynamic.annotations.QNameRef;
import de.cimt.talendcomp.xmldynamic.annotations.TXMLTypeHelper;

public final class Util {
    private static final Logger LOG = LoggerFactory.getLogger("de.cimt.talendcomp.xmldynamic");
    private static final ClassLoader LOADER;
    private static final Method METH;
    private static final List<TXMLBinding> BINDINGS;
    public static final boolean OSGI;

    public static void printClassLoader(ClassLoader classLoader) {
        printClassLoader(classLoader, true);
    }

    public static void printClassLoader(ClassLoader classLoader, boolean showparent) {

        if (null == classLoader) {
            return;
        }
        LOG.info("--------------------");
        LOG.info(classLoader.toString());
        if (classLoader instanceof URLClassLoader) {
            URLClassLoader ucl = (URLClassLoader) classLoader;
            int i = 0;
            for (URL url : ucl.getURLs()) {
                LOG.info("url[" + (i++) + "]=" + url);
            }

        }
        if(showparent)
            printClassLoader(classLoader.getParent());
        LOG.info("--------------------");
    }

    static{
        Method m = null;
        boolean isOSGI = runningInOSGi();
        ClassLoader cl = null;
        try{
        	if(!isOSGI)
        	{
                m = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
                m.setAccessible(true);
                cl=(URLClassLoader) Util.class.getClassLoader();
        	}
        	else
        	{
        		cl = Util.class.getClassLoader();
        	}
        }catch(Throwable t){
            LOG.error("failed to init environment",t);
        }
        /**
         * when osgi class is of type moduleclassloader and nested classloader doesn't use parent classload for resolving
         * to avoid this problem a colcal classloader must be used
         */

        OSGI=isOSGI;
        BINDINGS= new ArrayList<TXMLBinding>();
        LOG.info("OSGI = "+OSGI);
        METH=m;
        LOG.info("METHOD = "+METH);
        LOADER=cl;
    }

    private static boolean runningInOSGi()
    {
		Class<?> clazz = Util.class.getClassLoader().getClass();
		for(Class<?> current = clazz; !Object.class.equals(current); current = current.getSuperclass())
		{
			if(current.getName().startsWith("org.osgi"))
			{
				return true;
			}
			Class<?>[] interfaces = current.getInterfaces();
			for(Class<?> interfaze : interfaces)
			{
				if(interfaze.getName().startsWith("org.osgi"))
				{
					return true;
				}
			}
		}
		return false;
    }

    private static Iterator<TXMLBinding> load(){
        return ServiceLoader.load(de.cimt.talendcomp.xmldynamic.TXMLBinding.class, Util.class.getClassLoader()).iterator();
    }

    public static TXMLObject createTXMLObject(String name) throws Exception{
        try {
            return ((Class<TXMLObject>) findClass(name)).newInstance();
        } catch (Exception ex) {
        	LOG.error("Error instantiating class "+name, ex);
            throw ex;
        }
    }

    public static Class<?> findClass(String name) throws ClassNotFoundException{
        return LOADER.loadClass(name);
    }

    static void register(URI uri, boolean jar) throws Exception{
        URI serviceuri;
        if(jar){
            serviceuri= new URI( "jar:" + uri.toASCIIString() +"!/META-INF/services/de.cimt.talendcomp.xmldynamic.TXMLBinding" );
        } else {
            serviceuri=uri.resolve((jar ? "!/" : "./")+"META-INF/services/de.cimt.talendcomp.xmldynamic.TXMLBinding");
        }

        try{
            METH.invoke(LOADER, new Object[]{uri.toURL()});
        }catch(Throwable t){
            LOG.error("adding class failed",t);
        }

        InputStream in=null;
        try{
            in=serviceuri.toURL().openStream();
            int size;
            byte[] buffer = new byte[2048];
            StringBuilder buf = new StringBuilder();
            while ((size = in.read(buffer)) > 0) {
                buf.append(new String(buffer, 0, size));
            }
            String[] names = buf.toString().split("\n");
            for (int i = 0, max = names.length; i < max; i++) {
                final String value = (names[i].contains("#") ? names[i].substring(0, names[i].indexOf("#")) : names[i]).trim();
                LOG.warn("lookup service "+value);
                if(value.length()==0)
                    continue;
                if(OSGI){
                    TXMLBinding bindingInstance=((Class<TXMLBinding>) findClass( value )).newInstance();
                    LOG.warn("bindingInstance="+bindingInstance);
                    BINDINGS.add( bindingInstance );
                }
            }
        }finally{
            if(in!=null)
                in.close();
        }
    }

    public static String buildSQLInClause(Collection<? extends Object> keys) {
        StringBuilder sb = new StringBuilder();
        boolean firstLoop = true;
        for (Object key : keys) {
            if (key instanceof String) {
                if (firstLoop) {
                    firstLoop = false;
                    sb.append(" in (");
                } else {
                    sb.append(",");
                }
                sb.append("'");
                sb.append(((String) key).trim());
                sb.append("'");
            } else if (key != null) {
                if (firstLoop) {
                    firstLoop = false;
                    sb.append(" in (");
                } else {
                    sb.append(",");
                }
                sb.append(String.valueOf(key));
            }
        }
        if (firstLoop == false) {
            sb.append(") ");
        } else {
            sb.append(" is not null "); // a dummy condition to enforce a
            // reasonable filter
        }
        return sb.toString();
    }

    public static void printContexts() {
        printContexts(true);
    }

    private static void extractClassInfo(boolean includeAbstract, StringBuilder builder, Class<TXMLObject> clazz) {
        if (includeAbstract == false && ((clazz.getModifiers() & Modifier.ABSTRACT) == Modifier.ABSTRACT)) {
            return;
        }
        builder.append("class ");
        builder.append(clazz.getName());
        if (ReflectUtil.isRootClass(clazz)) {
            builder.append(" - is ROOT");
        }
        builder.append("\n");
        List<ExtPropertyAccessor> listProperties = ReflectUtil.introspect(clazz, TXMLObject.class);
        for (ExtPropertyAccessor prop : listProperties) {
            builder.append("    ").append(prop.getName()).append(" type: ").append(prop.getPropertyType().getName());
            if(Collection.class.isAssignableFrom(prop.getPropertyType()) ){
                final TXMLTypeHelper helper = prop.findAnnotation(TXMLTypeHelper.class);
                if(helper!=null){
                    builder.append("<");
                    boolean first=true;
                    for(QNameRef ref : helper.refs()){
                        if(!first){
                            builder.append(" or ");
                        }
                        builder.append(ref.type().getName());
                        first=false;
                    }
                    builder.append(">");
                }
            }

            builder.append("\n");
        }
        builder.append("\n----------------------------\n");
    }

    public static void printContexts(boolean includeAbstract) {
        final Iterator<TXMLBinding> iterator = load();
        StringBuilder builder = new StringBuilder();
        builder.append("\nJAX-B Contexts start ###################################\n");
        while (iterator.hasNext()) {
            for (Class<TXMLObject> clazz : iterator.next().getClasses()) {
                extractClassInfo(includeAbstract, builder, clazz);
            }
        }
        builder.append("\nJAX-B Contexts end ###################################\n");
        System.out.println(builder.toString());
    }

    public static void printElements() {
        final Iterator<TXMLBinding> iterator = load();
        StringBuilder builder = new StringBuilder();
        while (iterator.hasNext()) {
            for (Class<TXMLObject> clazz : iterator.next().getElements()) {
                extractClassInfo(true, builder, clazz);
            }
        }
        System.out.println(builder.toString());
    }

    public static JAXBContext createJAXBContext() throws JAXBException {
        final Iterator<TXMLBinding> iterator = load();
        List<Class<TXMLObject>> classes = new ArrayList<Class<TXMLObject>>();
        while (iterator.hasNext()) {
            classes.addAll(iterator.next().getClasses());
        }
        return JAXBContext.newInstance(classes.toArray(new Class[classes.size()]));
    }

    public static Class<TXMLObject> findClassFor(QName qn) throws JAXBException {
        final Iterator<TXMLBinding> iterator = load();
        while (iterator.hasNext())
        {
            TXMLBinding bind=iterator.next();

            final Class<TXMLObject> impl = bind.find(qn);
            if(impl!=null){
                return impl;
            }
        }
        return null;
    }

    @Jetcode
    public static List<TXMLObject> getTXMLObjects(TXMLObject parent, String attrPath, boolean ignoreMissing, boolean nullable) throws Exception {
        if (parent == null) {
            throw new IllegalArgumentException("parent cannot be null!");
        }
        if (attrPath == null || attrPath.trim().isEmpty()) {
            throw new IllegalArgumentException("attrPath cannot be null or empty!");
        }
        List<TXMLObject> result = new ArrayList<TXMLObject>();
        List<String> attrList = getDirectPathTokens(attrPath);
        TXMLObject currentObject = parent;
        for (int i = 0; i < attrList.size(); i++) {
            String attr = attrList.get(i);
            Object value = currentObject.get(attr);
            if (value instanceof TXMLObject.MissingAttribute) {
                if (ignoreMissing == false) {
                    throw new Exception("Starting from the object: " + parent.toString() + " following the path: " + attrPath + " - the attribute: " + value + " is missing!");
                }
            } else if (value instanceof TXMLObject) {
                if (i < attrList.size() - 1) {
                    // continue because we are not at the end of the path
                    currentObject = (TXMLObject) value;
                    continue; // continue with the next child
                } else {
                	// the referenced object is simply a TXMLObject
                	result.add((TXMLObject) value);
                }
            } else if (value instanceof List) {
                // check the collection if the elements are TXMLObjects
                if (i < attrList.size() - 1) {
                    // nothing left in the path but we do not got a TXMLObject!
                    throw new Exception("Starting from the object: " + parent.toString() + " following the path: " + attrPath + " - there is List at: " + attr + " but we are not at the end of the path! Reduce your path to this attribute and start from this component with an iteration to the next level!");
                }
                List<?> list = (List<?>) value;
                for (Object element : list) {
                    if (element instanceof TXMLObject) {
                        result.add((TXMLObject) element);
                    } else if (element != null) {
                        throw new Exception("In the list of the attribute " + attr + " there is an object which is not an TXMLObject (a complex xml element). We found this class: " + element.getClass().getName());
                    }
                }
            } else if (value != null) {
                if (i == attrList.size() - 1) {
                    // nothing left in the path but we do not got a TXMLObject!
                    throw new Exception("Starting from the object: " + parent.toString() + " following the path: " + attrPath + " - there is no TXMLObject (a complex xml element) but a value: " + value + ". Reduce your path to address the parent object!");
                }
            } else if (nullable == false) {
                throw new Exception("Starting from the object: " + parent.toString() + " following the path: " + attrPath + " - value is missing but mandatory!");
            }
        }
        return result;
    }

    private static List<String> getDirectPathTokens(String attrPath) {
        List<String> tokens = new ArrayList<String>();
        attrPath = attrPath.replace('/', '.'); // allow a bit XMLPath
        fillDirectPathToken(attrPath, tokens);
        return tokens;
    }

    private static void fillDirectPathToken(String attrPath, List<String> tokens) {
        int pos = attrPath.indexOf('.');
        if (pos != -1) {
            char pc = ' ';
            while (true) {
                if (pos > 0) {
                    pc = attrPath.charAt(pos - 1);
                }
                if (pc == '@' || pc == '\\') {
                    // skip over a filter condition or escaped dot
                    // find next position
                    int nextPos = attrPath.indexOf('.', pos + 1);
                    if (nextPos > pos) {
                        // take this new position
                        pos = nextPos;
                    } else {
                        pos = -1;
                        break;
                    }
                } else {
                    break;
                }
            }
            if (pos != -1 && pos < attrPath.length() - 1) {
                if (pos > 0) {
                    // only use not empty tokens
                    String token = attrPath.substring(0, pos);
                    tokens.add(token);
                }
                attrPath = attrPath.substring(pos + 1);
                fillDirectPathToken(attrPath, tokens);
            }
        } else {
            tokens.add(attrPath);
        }
    }

    public static TXMLObject unmarshall(String xml) throws Exception {
        if (xml == null || xml.trim().isEmpty()) {
            throw new IllegalArgumentException("xml input cannot be null or empty!");
        }
        JAXBContext s = createJAXBContext();
        Unmarshaller um = s.createUnmarshaller();
        StringReader xmlsr = new StringReader(xml);
        Object root = um.unmarshal(xmlsr);
        if (root == null) {
            throw new Exception("XML input does not contain any object!");
        } else if (root instanceof TXMLObject) {
            return (TXMLObject) root;
        } else {
            throw new Exception("The given xml input does not have an TXMLObject as root! We got a class: " + root.getClass().getName());
        }
    }

    public static TXMLObject unmarshall(File xml) throws Exception {
        if (xml == null) {
            throw new IllegalArgumentException("xml input cannot be null or empty!");
        } else if (xml.exists() == false) {
            throw new Exception("input file: " + xml.getAbsolutePath() + " does not exists!");
        }
        JAXBContext s = createJAXBContext();
        Unmarshaller um = s.createUnmarshaller();
        Object root = um.unmarshal(xml);
        if (root == null) {
            throw new Exception("XML input does not contain any object!");
        } else if (root instanceof TXMLObject) {
            return (TXMLObject) root;
        } else {
            throw new Exception("The given xml input does not have an TXMLObject as root! We got a class: " + root.getClass().getName());
        }
    }

    public static String uniqueString() {
        return UUID.randomUUID().toString().replaceAll("[:\\.-]+", "");
    }

	public static boolean isOsgi()
	{
		return OSGI;
	}
}
