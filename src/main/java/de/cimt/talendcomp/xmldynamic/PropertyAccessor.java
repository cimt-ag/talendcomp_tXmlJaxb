package de.cimt.talendcomp.xmldynamic;

import de.cimt.talendcomp.xmldynamic.annotations.QNameRef;
import de.cimt.talendcomp.xmldynamic.annotations.TXMLTypeHelper;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author Daniel Koch <Daniel.Koch@cimt-ag.de>
 */
@EqualsAndHashCode
@ToString
public class PropertyAccessor {
    private final String name;
    private final Class<?> propertyType;
    private Method       readMethod=null;
    private Method       writeMethod=null;
    private Field        publicField=null;
    Field                relatedField=null;

    /**
     * Create a new property accessor with a given name
     *
     * @param name the property name
     */
    public PropertyAccessor(String name, Class<?> type) {
        
        this.name = name;
        this.propertyType = type;
    }
    
    /**
     * Create a new property accessor with a given name
     *
     * @param name the property name
     * @param field the field related to this property
     */
    public PropertyAccessor(Field field) {
        this.name = field.getName();
        this.propertyType=field.getType();
        addField(field);
    }
    
    public PropertyAccessor(PropertyDescriptor pd) {
        this.name = pd.getName();
        this.readMethod=pd.getReadMethod();
        this.writeMethod=pd.getWriteMethod();
        this.propertyType=pd.getPropertyType();
    }

    public final boolean addField(Field field) {
        if(field==null)
            return false;
        
        if( propertyType!=null && !field.getType().equals(propertyType))
            return false;
            
        if( Modifier.isPublic( field.getModifiers() ) && this.relatedField==null )
            this.publicField = field;
        else if( !Modifier.isPublic( field.getModifiers() ) && this.publicField==null )
            this.relatedField = field;
        else 
            return false;
        
        return true;
    }
    
    public Set<String> getAnnotatedAliases(){
        
        final Set<String> aliases=new HashSet<>();
        
        Arrays.stream( new AnnotatedElement[]{
            publicField, relatedField, readMethod, writeMethod 
        }).filter( n -> n!=null)
        .forEach( n  -> {
            if( n.isAnnotationPresent( TXMLTypeHelper.class ) ){
                final QNameRef[] refs = n.getAnnotation(TXMLTypeHelper.class).refs();
                for(QNameRef q : refs){
                    aliases.add(q.name());
                }
            }
            if( n.isAnnotationPresent( XmlElement.class ) ) 
                aliases.add( n.getAnnotation( XmlElement.class ).name() );
            
            if( n.isAnnotationPresent( XmlAttribute.class ) )
               aliases.add( n.getAnnotation( XmlAttribute.class ).name() );
                
        }) 
        ;
        return aliases;
    }
    
    public Set<Class<?>> getAnnotatedTypes(){
        
        final Set<Class<?>> types=new HashSet<>();
        
        Arrays.stream( new AnnotatedElement[]{
            publicField, relatedField, readMethod, writeMethod 
        }).filter( n -> n!=null)
        .forEach( n  -> {
            if( n.isAnnotationPresent( TXMLTypeHelper.class ) ){
                final QNameRef[] refs = n.getAnnotation(TXMLTypeHelper.class).refs();
                for(QNameRef q : refs){
                    types.add( q.type() );
                }
            }
            if( n.isAnnotationPresent( XmlElement.class ) ){
                types.add( n.getAnnotation( XmlElement.class ).type()  );
            }
            if( n.isAnnotationPresent( XmlAttribute.class ) ){
                types.add( String.class  );
                
            }
            
            
        }) 
        ;
        return types;
    }

    /**
     * Checks whether the property is writable
     *
     * @return true, if there is either a write method, or a public field
     * present
     */
    public boolean isWritable() {
        return writeMethod != null || publicField != null;
    }

    /**
     * Checks whether the property is writable
     *
     * @return true, if there is either a write method, or a public field
     * present
     */
    public boolean isReadable() {
        return readMethod != null || publicField != null;
    }
    
    /**
     * Checks whether the property is only based on a public field
     *
     * @return true, if there is either a write method, or a public field
     * present
     */
    public boolean isFieldbased() {
        return readMethod == null && writeMethod == null;
    }
     
    /**
     * Checks whether the property is only based on a public field
     *
     * @return true, if there is either a write method, or a public field
     * present
     */
    public boolean isAccessible() {
        return readMethod != null || writeMethod != null || publicField != null;
    }

    /**
     * Fetch the property value from a given object
     *
     * @param invokee the object to query
     * @return the property value
     */
    public Object getPropertyValue(Object invokee) {
        try {
            if (readMethod != null) {
                return readMethod.invoke(invokee, new Object[0]);
            }
            if (publicField != null) {
                return publicField.get(invokee);
            }
            return relatedField.get(invokee);
        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the property value on a given object
     *
     * @param invokee the target object
     * @param value the new value
     */
    public void setPropertyValue(Object invokee, Object value) {
        System.err.println("write method: " + writeMethod);
        if (writeMethod != null) {
            try {
                writeMethod.invoke(invokee, new Object[]{ value });
                return;
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            }
        }  
        Field f = (publicField != null) ? publicField : relatedField;
        if (f == null) 
            throw new UnsupportedOperationException("Property not writable " + name);
        
        try {
            f.set(invokee, value);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException("error setting property "+ name, e);
        }
    }

    public String getName() {
        return name;
    }

    public Class<?> getPropertyType() {
        return propertyType;
    }

    public Method getReadMethod() {
        return readMethod;
    }

    public void setReadMethod(Method readMethod) {
        this.readMethod = readMethod;
    }

    public Method getWriteMethod() {
        return writeMethod;
    }

    public void setWriteMethod(Method writeMethod) {
        this.writeMethod = writeMethod;
    }
     
}
