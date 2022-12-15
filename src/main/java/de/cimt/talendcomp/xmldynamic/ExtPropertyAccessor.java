package de.cimt.talendcomp.xmldynamic;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *
 * @author Daniel Koch <Daniel.Koch@cimt-ag.de>
 */
public class ExtPropertyAccessor {

    private final String name;
    private Method readMethod=null;
    private Method writeMethod=null;
    private Field publicField=null;

    Field relatedField=null;

    /**
     * Create a new property accessor with a given name
     *
     * @param name the property name
     */
    public ExtPropertyAccessor(String name) {
        this.name = name;
    }

    /**
     * Returns the property name
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    public Field getRelatedField() {
        return relatedField != null ? relatedField : getPublicField();
    }

    public void setRelatedField(Field relatedField) {
        this.relatedField = relatedField;
    }

    /**
     * Set the public field
     *
     * @param publicField the public field
     */
    public void setPublicField(Field publicField) {
        this.publicField = publicField;
    }

    /**
     * Return the public field for this property (or null, if there is none)
     *
     * @return the public field
     */
    public Field getPublicField() {
        return publicField;
    }

    public <T extends Annotation> T getFieldAnnotation(Class<T> annotationClass) {
        if(publicField!=null)
            return publicField.getAnnotation(annotationClass);
        return (relatedField!=null) ? relatedField.getAnnotation(annotationClass) : null;
    }

    public Annotation[] getDeclaredFieldAnnotations() {
         if(publicField!=null)
            return publicField.getDeclaredAnnotations();
        return (relatedField!=null) ? relatedField.getDeclaredAnnotations() : new Annotation[]{};
    }

    /**
     * Get the read method (or null for public fields)
     *
     * @return the property read method
     */
    public Method getReadMethod() {
        return readMethod;
    }

    /**
     * Set the read method
     *
     * @param readMethod the read method
     */
    public void setReadMethod(Method readMethod) {
        this.readMethod = readMethod;
    }

    /**
     * Get the write method (or null for public fields)
     *
     * @return the property read method
     */
    public Method getWriteMethod() {
        return writeMethod;
    }

    /**
     * Set the write method
     *
     * @param writeMethod the write method
     */
    public void setWriteMethod(Method writeMethod) {
        this.writeMethod = writeMethod;
    }

    /**
     * sucht nach einer Annotation für ein Property. Zuerst wird die Read-Method
     * geprüft, im folgenden das Feld und zuletzt die Write-Method, so das es
     * einfach möglich ist in überschriebenen Klassen das Verhalten zu ändern.
     *
     * @param paaccessor Accessor für das Property
     * @param annotationClass die zu suchende Annotation
     * @return die gefundene Annotation oder <code>null</code> wenn nicht
     * vorhanden
     */
    public <Anno extends Annotation> Anno findAnnotation(Class<Anno> annotationClass) {

        Anno anno = null;
        if (getReadMethod() != null) {
            anno = getReadMethod().getAnnotation(annotationClass);
        }

        if (anno != null) {
            return anno;
        }

        if ((anno = getFieldAnnotation(annotationClass)) != null) {
            return anno;
        }

        if (isWritable() && getWriteMethod() != null) {
            return getWriteMethod().getAnnotation(annotationClass);
        }
        return null;
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
     * add missing field to property whose method was overwritten in childclass
     * and therefore detected before
     * @param parent 
     */
    void updateMissing(ExtPropertyAccessor parent) {
        if (relatedField == null) {
            relatedField = parent.relatedField;
        }
        if (getPublicField() == null) {
            setPublicField(parent.getPublicField());
        }
        if (getReadMethod() == null) {
            setReadMethod(parent.getReadMethod());
        }
        if (getWriteMethod() == null) {
            setWriteMethod(parent.getWriteMethod());
        }

    }

    /**
     * Get the property type
     *
     * @return the property type
     */
    public Class<?> getPropertyType() {
        if (readMethod != null) {
            return readMethod.getReturnType();
        } else if(publicField!=null){
            return publicField.getType();
        }  
        return relatedField.getType();
        
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
        if (writeMethod != null) {
            try {
                writeMethod.invoke(invokee, new Object[]{ value });
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            }
        }  
        Field f = (publicField != null) ? publicField : relatedField;
        if (f == null) 
            throw new UnsupportedOperationException("Property not writable " + name);
        
        try {
            f.set(invokee, value);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    

    @Override
    public String toString() {
        return "accessor: " + this.getName()
                + "\n\tfield=" + this.getPublicField()
                + "\n\t      " + this.getRelatedField()
                + "\n\tread =" + this.getReadMethod()
                + "\n\twrite=" + this.getWriteMethod();
        
        

    }

}
