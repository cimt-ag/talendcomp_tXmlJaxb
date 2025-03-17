package de.cimt.talendcomp.xmldynamic;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlTransient;
import java.util.Map.Entry;
import javax.xml.datatype.XMLGregorianCalendar;


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

    private PropertyAccessor getPropertyAccessorByName(String attr){
        return ReflectUtil.introspectName(this.getClass()).entrySet().stream()
                .filter( entry -> entry.getKey().equalsIgnoreCase(attr))
                .map( entry -> entry.getValue())
                .findFirst()
                .orElse( null );
    } 
    
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
        
    	PropertyAccessor attr = findFirstyAccessorByType(childObject.getClass());
    	if (attr == null) {
            LOG.finer("no property found in class "+getClass().getName()+ " matching type " +childObject.getClass().getName() );
            return false;
    	}
        if (Collection.class.isAssignableFrom(attr.getPropertyType()))  
            return internalAdd(attr, childObject.getClass(), childObject);
        return internalSet(attr, childObject.getClass(), childObject);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public boolean set(String attr, Object value) {
        if (attr == null || attr.trim().isEmpty()) {
            throw new IllegalArgumentException("attribute name cannot be null or empty!");
        }
        PropertyAccessor pa = getPropertyAccessorByName(attr);
        if(pa==null){
            return false;
        }
        LOG.log(Level.FINER,"Property Type: {0}",pa.getPropertyType());
        return internalSet(pa, pa.getPropertyType(), value);
    }
    
    private boolean internalSet(PropertyAccessor pa, Class<?> type, Object value){
        LOG.log(Level.FINER, "internalSet {0} to type {1} value={2}", new Object[]{pa.getName(), type, value});
        /**
         * jaxb never generates setter for collections, so set must be get and
         * add....
         */
        if (Collection.class.isAssignableFrom(pa.getPropertyType())) {
            ((Collection) pa.getPropertyValue(this)).clear();
            return internalAdd(pa, type, value);
        }
        LOG.log(Level.FINER, "convert {0} to type {1}", new Object[]{value, type});
        // happens here
        System.err.println("this class: " + this.getClass().getName());
        pa.setPropertyValue(this, ReflectUtil.convert(value, type));
        return true;
    }
    
    private boolean internalAdd(PropertyAccessor pa, Class<?> type, Object value){
        LOG.log(Level.FINER, "internalAdd {0} to type {1} value={2}", new Object[]{pa.getName(), type, value});
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
        return findFirstyAccessorByName(attr).getPropertyType();
    }

    public Object get(String attr) {
        if (attr == null || attr.trim().isEmpty()) {
            throw new IllegalArgumentException("attribute name cannot be null or empty!");
        }
        PropertyAccessor pa = getPropertyAccessorByName(attr);
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
    	//attr = ReflectUtil.camelizeName(attr);
        PropertyAccessor pa = getPropertyAccessorByName(attr);
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
        PropertyAccessor pa = getPropertyAccessorByName(attr);
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
        System.err.println("orginial attr: " + attr);
        PropertyAccessor pa = getPropertyAccessorByName(attr);
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
        // --> property not writeable
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
        return ReflectUtil.introspectName( this.getClass() ).keySet();
    }

    public String findFirstPropertyByType(Class<? extends TXMLObject> clazz) {
        PropertyAccessor pa=findFirstyAccessorByType(clazz);
        return pa!=null ? pa.getName() : null;
    }
    
    private PropertyAccessor findFirstyAccessorByType(Class<? extends TXMLObject> clazz) {
        for (PropertyAccessor pa : ReflectUtil.introspect(this.getClass()) ) {
            if( pa.getAnnotatedTypes().contains(clazz ) )
                return pa;
        }
        return null;

    }
    
    private PropertyAccessor findFirstyAccessorByName(String name) {
        for (Entry<String, PropertyAccessor> pa : ReflectUtil.introspectName(this.getClass()).entrySet() ) {
            if( pa.getKey().equalsIgnoreCase(name) )
                return pa.getValue();
        }
        return null;
    }

    private boolean childAddOrSet(final String path, Object childvalue) throws Exception{
        int pos=path.indexOf("/");
        if (pos < 0) {
            // this should be handles somewhere else
            return addOrSet(path, childvalue);
        } 
        
//        final Pair<PropertyAccessor, Class<?>> pa = findFirstyAccessorByElementName( path.substring(0, pos) );
        PropertyAccessor p=ReflectUtil.introspectName( getClass() ).entrySet().stream()
                .filter( es-> es.getKey().equalsIgnoreCase( path.substring(0, pos) ) )
                .map( es -> es.getValue() )
                .findFirst().orElseThrow();
        
        if ( !TXMLObject.class.isAssignableFrom( p.getPropertyType() ) ) {
            p.setPropertyValue(childvalue, childvalue);
            throw new IllegalArgumentException("unresolveable path "+path+". Referring Element type does not support nested content");
        }
        TXMLObject child=(TXMLObject)  p.getPropertyType().getConstructor().newInstance();
        addOrSet( child );
        return child.childAddOrSet(path.substring(pos+1), childvalue);
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
