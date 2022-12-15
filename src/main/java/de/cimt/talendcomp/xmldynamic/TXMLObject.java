package de.cimt.talendcomp.xmldynamic;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlTransient;
import javax.xml.datatype.XMLGregorianCalendar;

import de.cimt.talendcomp.xmldynamic.annotations.QNameRef;
import de.cimt.talendcomp.xmldynamic.annotations.TXMLTypeHelper;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class represents the base class for the generated jax-b classes
 * It will be used in the Talend components to set or add the values from the flows.
 * @author daniel.koch@cimt-ag.de, jan.lolling@cimt-ag.de
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlTransient
public abstract class TXMLObject implements Serializable, Cloneable {
    private static final Logger LOG = Logger.getLogger("de.cimt.talendcomp.xmldynamic");

    private static final long serialVersionUID = 1L;
    
    public static class MissingAttribute {
    	
    	private String attrName = null;
    	
    	public MissingAttribute(String attrName) {
    		this.attrName = attrName;
    	}
    	
    	public String getName() {
    		return attrName;
    	}
    	
    	@Override
    	public String toString() {
    		return attrName;
    	}
    	
    }

    private ExtPropertyAccessor getPropertyAccessorByName(String attr){
        final Collection<ExtPropertyAccessor> props = CACHE.get(this.getClass()).values();
        
        ExtPropertyAccessor pa= props.stream()
                    .filter( e -> e.getName().equalsIgnoreCase(attr) )
                    .findFirst().orElse(null);
        
        if(pa!=null)
            return pa;
        
        /*
            Handle Collections like:
            @TXMLTypeHelper(collection = true, refs = {
                @QNameRef(name = "address", type = Customer.Address.class, uri = "http://cimt.de/customer/"),
                @QNameRef(name = "poaddress", type = Customer.Poaddress.class, uri = "http://cimt.de/customer/")
            })
            protected List<de.cimt.customer.Address> addressOrPoaddress;
        */
        return props.stream()
                .collect( Collectors.toMap( e -> e, e -> e.findAnnotation(TXMLTypeHelper.class)))
                .entrySet().stream()
                .filter( e -> e.getValue()!=null 
                        && Arrays.asList( e.getValue().refs() )
                                .stream().filter( ref -> ref.name().equalsIgnoreCase(attr)).findFirst().orElse(null)!=null )
                .map( e -> e.getKey() )
                .findFirst().orElse(null);
    }
    
    @XmlTransient
    private static final Map<Class<?>, Map<String, ExtPropertyAccessor>> CACHE = new AutoMap<Class<?>, Map<String, ExtPropertyAccessor>>(){
        @Override
        public Map<String, ExtPropertyAccessor> create(Class<?> key) {
            return ReflectUtil.introspect(key, Object.class).stream()
                    .collect( Collectors.toMap(pa -> pa.getName(), pa -> pa));
        }
    };
 
    @XmlTransient
    private Pair<Class<? extends TXMLObject>, Object> _xmlParentBinding;

    @XmlTransient
    private Object _xmlID;

    Pair<Class<? extends TXMLObject>, Object> get_XmlParent() {
        return _xmlParentBinding;
    }

    public Class<? extends TXMLObject> get_XmlParentClass() {
        return _xmlParentBinding != null ? _xmlParentBinding.x : null;
    }

    public void set_XmlParentClass(Class<? extends TXMLObject> _xmlParentClass) {
        _xmlParentBinding.x = _xmlParentClass;
    }

    public Object get_XmlParentID() {
        return _xmlParentBinding != null ? _xmlParentBinding.y : null;
    }

    public void set_XmlParentID(Object _xmlParentID) {
        _xmlParentBinding.y = _xmlParentID;
    }

    public void set_XmlParent(TXMLObject _xmlParent) {
        _xmlParentBinding = new Pair<Class<? extends TXMLObject>, Object>(_xmlParent.getClass(), _xmlParent._xmlID);
    }

    public Object get_XmlID() {
        return _xmlID;
    }

    public void set_XmlID(Object _xmlID) {
        this._xmlID = _xmlID;
    }
    
    public boolean addOrSet(TXMLObject childObject) {
        if(childObject==null){
           LOG.finer("child is null");
           return false;
        }
        
    	ExtPropertyAccessor attr = findFirstyAccessorByType(childObject.getClass());
    	if (attr == null) {
            LOG.finer("no property found in class "+getClass().getName()+ " matching type " +childObject.getClass().getName() );
            return false;
    	}
        if (Collection.class.isAssignableFrom(attr.getPropertyType()))  
            return add(attr, childObject.getClass(), childObject);
        return set(attr, childObject.getClass(), childObject);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public boolean set(String attr, Object value) {
        if (attr == null || attr.trim().isEmpty()) {
            throw new IllegalArgumentException("attribute name cannot be null or empty!");
        }
        ExtPropertyAccessor pa = getPropertyAccessorByName(attr);
        if(pa==null){
            return false;
        }
        if (Collection.class.isAssignableFrom(pa.getPropertyType())) {
            return set(pa , getCollectionTargetType(pa, attr), value);
        }
        return set(pa, pa.getPropertyType(), value);
    }
    
    private Class<?> getCollectionTargetType(final ExtPropertyAccessor accessor, final String name){
        
        final TXMLTypeHelper anno = accessor.findAnnotation(TXMLTypeHelper.class);
        if(anno==null)
            return Object.class;
        
        return Stream.of( anno.refs() ).filter( ref -> ref.name().equalsIgnoreCase(name) ).map( ref -> ref.type() ).findFirst().orElse(null);
    }
    
    private boolean set(ExtPropertyAccessor pa, Class<?> type, Object value){
        /**
         * jaxb never generates setter for collections, so set must be get and
         * add....
         */
        if (Collection.class.isAssignableFrom(pa.getPropertyType())) {
            ((Collection) pa.getPropertyValue(this)).clear();
            return add(pa, type, value);
        }
        pa.setPropertyValue(this, ReflectUtil.convert(value, type));
        return true;
    }
   private boolean add(ExtPropertyAccessor pa, Class<?> type, Object value){
        /**
         * jaxb never generates setter for collections, so set must be get and
         * add....
         */
        if (Collection.class.isAssignableFrom(pa.getPropertyType())) {
            Collection currentValue = (Collection) pa.getPropertyValue(this);
            if (value != null && Collection.class.isAssignableFrom(value.getClass())) {
                currentValue.addAll(((Collection) value));
            } else {
                currentValue.add( ReflectUtil.convert(value, type) );
            }
            return true;
        }
        return false;
    }

    public Class<?> getType(String attr) {
        if (attr == null || attr.trim().isEmpty()) {
            throw new IllegalArgumentException("attribute name cannot be null or empty!");
        }
        return CACHE.get(this.getClass()).get(attr).getPropertyType();
    }

    public Object get(String attr) {
        if (attr == null || attr.trim().isEmpty()) {
            throw new IllegalArgumentException("attribute name cannot be null or empty!");
        }
        ExtPropertyAccessor pa = getPropertyAccessorByName(attr);
        if (pa == null) {
            return new MissingAttribute(attr);
        }
        Class<?> targetClass = pa.getPropertyType();
        if (targetClass.isAssignableFrom(XMLGregorianCalendar.class)) {
        	targetClass = Date.class; // we expect actually nobody will work with XMLGregorianCalendar!
        }
        return ReflectUtil.convert(pa.getPropertyValue(this), targetClass);
    }

    public Object get(String attr, Class<?> targetClass, boolean ignoreMissing, boolean nullable) throws Exception {
        if (attr == null || attr.trim().isEmpty()) {
            throw new IllegalArgumentException("attribute name cannot be null or empty!");
        }
//    	attr = ReflectUtil.camelizeName(attr);
        ExtPropertyAccessor pa = getPropertyAccessorByName(attr);
        Object value = null;
        if (pa == null) {
            if (ignoreMissing == false) {
                throw new Exception("Attribute: " + this.getClass().getName() + "." + attr + " is missing but expected!");
            }
        } else {
            value = ReflectUtil.convert(pa.getPropertyValue(this), targetClass);
        }
        if (nullable && value == null) {
    		throw new Exception("Attribute: " + this.getClass().getName() + "." + attr + " is null but expected!");
        }
        return value;
    }

    public int size(String attr) {
        if (attr == null || attr.trim().isEmpty()) {
            throw new IllegalArgumentException("attribute name cannot be null or empty!");
        }
        int size = 0;
        ExtPropertyAccessor pa = getPropertyAccessorByName(attr);
        if (Collection.class.isAssignableFrom(pa.getPropertyType())) {
            Object currentValue = pa.getPropertyValue(this);
            size = ((Collection<?>) currentValue).size();
        }
        return size;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public boolean addOrSet(String attr, Object value) {
        // TODO check value can be null or not?
        if (attr == null || attr.trim().isEmpty()) {
            throw new IllegalArgumentException("attribute name cannot be null or empty!");
        }
    	//attr = ReflectUtil.camelizeName(attr);
        ExtPropertyAccessor pa = getPropertyAccessorByName(attr);
        if (pa == null) {
            if (attr.indexOf("/") > 0) {
                try {
                    return childAddOrSet(attr, value);
                } catch (Exception ex) {
                    Logger.getLogger(TXMLObject.class.getName()).log(Level.SEVERE, null, ex);
                    return false;
                }
            } else {
                return false;
            }
        }
        Object currentValue = pa.getPropertyValue(this);
        if (pa.getPropertyType().isArray()) {
            int len = Array.getLength(currentValue);
            Object array = Array.newInstance(pa.getPropertyType().getComponentType(), len + 1);
            System.arraycopy(currentValue, 0, array, 0, len);
            Array.set(array, len + 1, value);
            return true;
        }
        if (List.class.isAssignableFrom(pa.getPropertyType())) {
            ((List) currentValue).add(value);
            return true;
        }
        return set(attr, value);
    }

    public void afterUnmarshal(Unmarshaller um, Object parent) {
        if (parent == null) {
            return;
        }
        try {
            this.set_XmlParent((TXMLObject) parent);
        } catch (Throwable t) {
        	// can only be class cast exception and then there is nothing to do
        } 
    }

    public Set<String> getNames() {
        return CACHE.get(this.getClass()).keySet();
    }

    public String findFirstPropertyByType(Class<? extends TXMLObject> clazz) {
        for (ExtPropertyAccessor pa : CACHE.get(this.getClass()).values()) {
            TXMLTypeHelper typeHelper=pa.findAnnotation(TXMLTypeHelper.class);
            if(typeHelper!=null){
                for(QNameRef ref : typeHelper.refs()){
                    if(clazz.equals(ref.type()))
                        return pa.getName();
                }
            } else {
                if( pa.getPropertyType().equals(clazz) ){
                    return pa.getName();
                }
                        
            }
        }
        return null;
    }
    
    private ExtPropertyAccessor findFirstyAccessorByType(Class<? extends TXMLObject> clazz) {
        for (ExtPropertyAccessor pa : CACHE.get(this.getClass()).values()) {
            TXMLTypeHelper eth = pa.findAnnotation(TXMLTypeHelper.class);
            
            if(eth!=null){ 
                for(  QNameRef ref : eth.refs() ){
                    if(ref.type().equals(clazz)){
                        return pa;
                    }
                }
            } else {
                if (pa.getPropertyType().equals(clazz)) {
                    return pa;
                }
            }
        }
        return null;
    }
    
    private Pair<ExtPropertyAccessor, Class<?>> findFirstyAccessorByElementName(String name) {
        
        for (ExtPropertyAccessor pa : CACHE.get(this.getClass()).values()) {
            TXMLTypeHelper eth = pa.findAnnotation(TXMLTypeHelper.class);
            
            if(eth!=null){ 
                for(  QNameRef ref : eth.refs() ){
                    if(ref.name().equals(name)){
                        return new Pair<ExtPropertyAccessor, Class<?>>(pa, ref.type());
                    }
                }
            }
        }
        return null;
    }
    
/*    
    private ExtPropertyAccessor findFirstyAccessorByElementQName(QName fqname) {
        for (ExtPropertyAccessor pa : CACHE.get(this.getClass()).values()) {
            TXMLTypeHelper eth = pa.findAnnotation(TXMLTypeHelper.class);
            if(eth!=null){ // check property annotation for registered element classes
                for( QNameRef ref : eth.refs()){
                    if(ref.name().equals(fqname.getLocalPart()) && ref.uri().equals(fqname.getNamespaceURI()))
                        return pa;
                }
            }
        }
        return null;
    }
*/

    private boolean childAddOrSet(String path, Object childvalue) throws Exception{
        int pos=path.indexOf("/");
        if (pos < 0) {
            // this should be handles somewhere else
            return false;
        } 
        
        final Pair<ExtPropertyAccessor, Class<?>> pa = findFirstyAccessorByElementName( path.substring(0, pos) );
        if (pa == null) {
            throw new IllegalArgumentException("unresolveable path "+path+" for class "+this.getClass().getName() );
        }
        if (TXMLObject.class.isAssignableFrom(pa.y) == false) {
            throw new IllegalArgumentException("unresolveable path "+path+". Referring Element type does not support nested content");
        }
        TXMLObject child=(TXMLObject) pa.y.getConstructor().newInstance();
        this.addOrSet( child );
        
        child.addOrSet(path.substring(pos+1), childvalue);
        return true;
    }
    
//    @SuppressWarnings({ "rawtypes", "unchecked" })
//    public static TXMLObject shakeIt(List<TXMLObject> gll) throws Exception {
//        /**
//         * 1. create map of relations from tuple class, id to object
//         */
//        final HashMap<Pair<Class<? extends TXMLObject>, Object>, TXMLObject> parentLookupMap
//            = CollectionUtil.<TXMLObject, Pair<Class<? extends TXMLObject>, Object>>generateLookupMap(gll,
//                    new Transformer<TXMLObject, Pair<Class<? extends TXMLObject>, Object>>() {
//                @Override
//                public Pair<Class<? extends TXMLObject>, Object> transform(TXMLObject current) {
//                    return new Pair<Class<? extends TXMLObject>, Object>(current.getClass(), current._xmlID);
//                }
//            }
//        );
//
//        /**
//         * 2. find root element
//         */
//        final TXMLObject root = CollectionUtil.applyFilter(gll, new Filter<TXMLObject>() {
//            AtomicInteger count=new AtomicInteger();
//            @Override
//            public boolean matches(TXMLObject row) {
//                if( row._xmlParentBinding  == null ){
//                    if(count.incrementAndGet()>1){
//                        throw new IllegalArgumentException(Messages.format( Messages.SHAKE_MULTIPLEROOTS ));
//                    }
//                    return true;
//                }
//                return false;
//            }
//        }).remove(0);
//
//        /**
//         * 2. create list of child nodes for parent
//         */
//        Map<TXMLObject, ArrayList<TXMLObject>> relations = CollectionUtil.<TXMLObject, TXMLObject>split(gll,
//            new Transformer<TXMLObject, TXMLObject>() {
//                @Override
//                public TXMLObject transform(TXMLObject child) {
//                    return parentLookupMap.get(child._xmlParentBinding);
//                }
//            }
//        );
//
//        /**
//         * 3. assign values to parent
//         */
//        Map<Class, Map<Class<TXMLObject>, PropertyDescriptor>> cache = new HashMap<Class, Map<Class<TXMLObject>, PropertyDescriptor>>();
//
//        for (TXMLObject parent : relations.keySet()) {
//
//            if (!cache.containsKey(parent.getClass())) {
//                cache.put(parent.getClass(), introspect((Class<TXMLObject>) parent.getClass()));
//            }
//
//            Map<Class<TXMLObject>, PropertyDescriptor> parentInfo = cache.get(parent.getClass());
//
//            for (TXMLObject child : relations.get(parent)) {
//                /*
//                 *  parent has attribute of type child-type oder List<child-type>
//                 *  when list, jaxb uses getter an add, otherwise setter is used 
//                 */
//                PropertyDescriptor pd = parentInfo.get(child.getClass());
//                if (pd.getPropertyType().isAssignableFrom(List.class)) {
//                    ((List<TXMLObject>) pd.getReadMethod().invoke(parent)).add(child);
//                } else {
//                    pd.getWriteMethod().invoke(parent, child);
//                }
//
//            }
//        }
//        return root;
//    }

    @SuppressWarnings("unchecked")
    private static Map<Class<TXMLObject>, PropertyDescriptor> introspect(Class<TXMLObject> parent) throws Exception {
        Map<Class<TXMLObject>, PropertyDescriptor> bindings = new HashMap<Class<TXMLObject>, PropertyDescriptor>();

        BeanInfo bi = Introspector.getBeanInfo(parent);

        for (PropertyDescriptor pd : bi.getPropertyDescriptors()) {
            if (pd.getPropertyType().isAssignableFrom(TXMLObject.class)) {
                bindings.put((Class<TXMLObject>) pd.getPropertyType(), pd);
            } else if (pd.getPropertyType().isAssignableFrom(List.class) && pd.getPropertyType().getComponentType().isAssignableFrom(TXMLObject.class)) {
                bindings.put((Class<TXMLObject>) pd.getPropertyType().getComponentType(), pd);

            }
        }
        return bindings;
    }

    public String toXML() throws JAXBException {
        return toXML(false, false);
    }
    
    public String toXML(boolean formatted) throws JAXBException {
        return toXML(formatted, false);
    }
    
    public String toXML(boolean formatted, boolean fragment) throws JAXBException {
        final Marshaller marshaller = Util.createJAXBContext().createMarshaller();
        if (formatted) {
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        }
        if(fragment){
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        }
        StringWriter sw = new StringWriter();       
        marshaller.marshal(this, sw);
        return sw.toString();
    } 
    
}
