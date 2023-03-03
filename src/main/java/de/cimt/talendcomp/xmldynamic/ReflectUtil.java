package de.cimt.talendcomp.xmldynamic;

import de.cimt.utils.conversion.Converter;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.util.JAXBResult;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Node;

/**
 *
 * Angepasste aus Colllib um die bestehenden Funktionen zu erweitern
 * TODO: let us enhance the collib library
 */
public class ReflectUtil {

	private static boolean printDebugInfo = false;
    private static final Logger LOG = Logger.getLogger("de.cimt.talendcomp.xmldynamic");
	
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Number convertNumber(String text, Class type) throws ParseException {
        Number numb = null;
        try {
            numb = NumberFormat.getInstance().parse(text);
        } catch (ParseException e0) {
            try {
                numb = NumberFormat.getInstance(Locale.ENGLISH).parse(text);
            } catch (ParseException e1) {
                try {
                    numb = NumberFormat.getInstance(Locale.GERMAN).parse(text);
                } catch (ParseException e2) {
                    if (text.startsWith(".")) {
                        return convertNumber("0" + text, type);
                    }
                    throw e0;
                }
            }
        }

        if (type.equals(AtomicInteger.class)) {
            return new AtomicInteger(numb.intValue());
        }
        if (type.equals(AtomicLong.class)) {
            return new AtomicLong(numb.longValue());
        }
        if (type.equals(BigDecimal.class)) {
            return new BigDecimal(numb.doubleValue());
        }
        if (type.equals(BigInteger.class)) {
            return new BigInteger(numb.toString());
        }
        if (type.equals(Byte.class)) {
            return numb.byteValue();
        }
        if (type.equals(Double.class)) {
            return numb.doubleValue();
        }
        if (type.equals(Float.class)) {
            return numb.floatValue();
        }
        if (type.equals(Integer.class)) {
            return numb.intValue();
        }
        if (type.equals(Long.class)) {
            return numb.longValue();
        }
        if (type.equals(Short.class)) {
            return numb.shortValue();
        }

        throw new UnsupportedOperationException("Cannot convert type " + type + " to class " + Number.class);
    }

    @SuppressWarnings("unchecked")
    public static <T> T convertXML(Object v, Class<?> vClass, Class<T> tClass) {
        try {
            Source s = null;
            if (CharSequence.class.isAssignableFrom(vClass)) {
                s = new StreamSource( new StringReader(((CharSequence) v).toString()) );
            }
            if (Node.class.isAssignableFrom(vClass)) {
                s = new DOMSource((Node) v);
            }
            if (XMLEventReader.class.isAssignableFrom(vClass)) {
                s = new StAXSource((XMLEventReader) v);
            }
            if (XMLStreamReader.class.isAssignableFrom(vClass)) {
                s = new StAXSource((XMLStreamReader) v);
            }
            if (Reader.class.isAssignableFrom(vClass)) {
                s = new StAXSource((XMLStreamReader) v);
            }
            if (File.class.isAssignableFrom(vClass)) {
                s = new StreamSource((File) v);
            }
            if (InputStream.class.isAssignableFrom(vClass)) {
                s = new StreamSource((InputStream) v);
            }
            if (s != null) {
                s.setSystemId("~/" + UUID.randomUUID().toString());
                JAXBResult result = new JAXBResult(Util.createJAXBContext());
                TransformerFactory.newInstance().newTransformer().transform(s, result);
                return (T) result.getResult();
            }
            return Converter.convert(v, vClass, tClass);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Convert an object to type tClass This Methods also allows Numbers in
     * localized format and customizations of enum values (like XmlEnumValue).
     *
     * @param v the object to convert
     * @param tClass target class
     * @return v the converted object of type tClass
     * @exception java.lang.UnsupportedOperationException in case of any error
     * during conversion
     * @see {org.colllib.util.convert}
     */
    public static <T> T convert(Object v, Class<T> tClass) {
        if (v == null) {
            return null;
        }
        return convert(v, v.getClass(), tClass);
    }

    /**
     * Convert an object to type tClass This Methods also allows Numbers in
     * localized format and customizations of enum values (like XmlEnumValue).
     *
     * @param <T> wildcard type of targettype
     * @param v the object to convert
     * @param vClass class of value v 
     * @param tClass target class
     * @return v the converted object of type tClass
     * @exception java.lang.UnsupportedOperationException in case of any error
     * during conversion
     * @see {org.colllib.util.convert}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T convert(Object v, Class<?> vClass, Class<T> tClass) {
        if (v == null) {
            return null;
        }
        if (vClass.isAssignableFrom(java.util.Date.class) && tClass.isAssignableFrom(XMLGregorianCalendar.class)) {
            Calendar cal = GregorianCalendar.getInstance();
            Date dateValue = (Date) v;
            cal.setTime(dateValue);
            try {
                return (T) javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar((GregorianCalendar) cal);
            } catch (DatatypeConfigurationException e) {
                throw new RuntimeException(e);
            }
        } else if (XMLGregorianCalendar.class.isAssignableFrom(vClass) && Date.class.isAssignableFrom(tClass)) {
            XMLGregorianCalendar xmlCal = (XMLGregorianCalendar) v;
            return (T) xmlCal.toGregorianCalendar().getTime();
        }
        if (vClass.isAssignableFrom(TXMLObject.class)) {
            return convertXML(v, vClass, tClass);
        }
        try {
            return Converter.convert(v, vClass, tClass);
        } catch (Throwable uoe) {
            if (Number.class.isAssignableFrom(tClass)) {
                try {
                    return (T) convertNumber(v.toString(), tClass);
                } catch (Throwable ex) {
                    if (v.toString().trim().length() == 0) {
                        return null;
                    }
                    throw new RuntimeException(ex);
                }
            } else if (tClass.isEnum()) {
                return (T) findEnumConstant((Class) tClass, v.toString());
            } else if ((Boolean.class.equals(tClass) || boolean.class.equals(tClass)) && Number.class.isAssignableFrom(vClass)) {
                return convert(Boolean.toString(((Number) v).intValue() == 1), String.class, tClass);
            }
            
            LOG.warning("convert failed: convert "+vClass.getName()+" with value "+v+" to class "+tClass.getName());
            throw new RuntimeException(uoe);
        }
    }

    public static Duration handleDurations(Object v, Class<?> vClass, Class<Duration> tClass) throws Exception {
        final DatatypeFactory dtf = DatatypeFactory.newInstance();
        if (CharSequence.class.isAssignableFrom(vClass)) {
            try {
                try {
                    Method m = DatatypeFactory.class.getMethod("newDuration", vClass);
                    return (Duration) m.invoke(dtf, v);
                } catch (IllegalArgumentException ex) {
                    try {
                        Method m = DatatypeFactory.class.getMethod("newDurationDayTime", vClass);
                        return (Duration) m.invoke(dtf, v);
                    } catch (IllegalArgumentException nex) {
                        try {
                            Method m = DatatypeFactory.class.getMethod("newDurationYearMonth", vClass);
                            return (Duration) m.invoke(dtf, v);
                        } catch (IllegalArgumentException nnex) {
                        	// ignore intentionally
                        }
                    }
                }
            } catch (NoSuchMethodException ex) {
            	// ignore intentionally
            }
        }
        Calendar cal = null;
        if (Date.class.isAssignableFrom(vClass)) {
            cal = GregorianCalendar.getInstance();
            cal.setTime((Date) v);
        } else if (Calendar.class.isAssignableFrom(vClass)) {
            cal = (Calendar) v;
        }

        if (cal != null) {
            return dtf.newDuration(true, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
        }

        return null;
    }

    public static <T> T[] convert(Object[] vs, Class<T> tClass) {
        return convert(vs, vs.getClass().getComponentType(), tClass);
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] convert(Object[] vs, Class<?> vClass, Class<T> tClass) {
        List<T> res = new ArrayList<T>();
        for (Object o : vs) {
            res.add(convert(o, vClass, tClass));
        }
        return (T[]) res.toArray();
    }

    public static <T> List<T> convert(Collection<Object> vs, Class<T> tClass) {
        List<T> res = new ArrayList<T>();
        for (Object o : vs) {
            res.add(convert(o, o.getClass(), tClass));
        }
        return res;
    }

    /**
     * Finds EnumConstant by String. This method allows customized enumconstants
     * by annotations with names ending with *EnumValue (like
     * jakarta.xml.bind.annotation.XmlEnumValue)
     *
     * @param <T>
     * @param clazz
     * @param value
     * @return
     */
    public static <T extends Enum<T>> T findEnumConstant(Class<T> clazz, String value) {
        if (value == null || value.length() == 0) {
            return null;
        }
        try {
            return T.valueOf(clazz, value);
        } catch (Throwable ignore) {
        }

        for (Field o : clazz.getFields()) {
            // skip instance attributes
            if (!o.getType().equals(clazz)) {
                continue;
            }

            for (Annotation anno : o.getAnnotations()) {
                /**
                 * testen aller annnotations EnumValue (z.B. XmlEnumValue,
                 * CSVEnumValue, etc),
                 */
                if (anno.annotationType().getName().endsWith("EnumValue")) {
                    try {
                        if (((String) anno.getClass().getMethod("value", new Class<?>[]{}).invoke(anno, (Object[]) null)).equalsIgnoreCase(value)) {
                            return T.valueOf(clazz, o.getName());
                        }
                    } catch (Throwable ignore) {
                    }
                }
            }
        }
        return null;
    }

    public static List<Field> getAllFields(Class<?> c) {
        return getAllFields(c, Object.class);
    }

    public static List<Field> getAllFields(Class<?> c, Class<?> stopclass) {
        List<Field> fields = new ArrayList<Field>();
        if (c != null && !c.equals(stopclass)) {
            fields.addAll(getAllFields(c.getSuperclass()));

            for (Field f : c.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    fields.add(f);
                }
            }
        }
        return fields;
    }

    public static <T extends Annotation> T getClassAnnotation(Class<?> cl, Class<T> anno) {
        return cl.getAnnotation(anno);
    }

    public static <T extends Annotation> Method findAnnotatedMethod(Class<?> type, Class<T> anno) {
        if (type == null || anno == null) {
            return null;
        }
        for (Method m : type.getDeclaredMethods()) {
            if (!Modifier.isPublic(m.getModifiers()) || m.getAnnotation(anno) == null) {
                continue;
            }
            return m;
        }
        if (type.getSuperclass() == null) {
            return null;
        }
        return findAnnotatedMethod(type.getSuperclass(), anno);
    }

   

    private static final Pattern MPAT = Pattern.compile("(get|is|set)(.*)");

    private static final AutoMap<Class<?>, List<ExtPropertyAccessor>> CACHE = new AutoMap<>() {
        @Override
        public synchronized List<ExtPropertyAccessor> create(Class<?> key) {
            return introspectInternal(key);
        }
    };

    /**
     * Introspect a given class and find all accessible properties
     *
     * @param tClass the class
     * @return a list of {@link org.colllib.introspect.PropertyAccessor} objects
     */
    public static List<ExtPropertyAccessor> introspect(Class<?> tClass) {
        return introspect(tClass, null);
    }

    public static List<ExtPropertyAccessor> introspect(Class<?> tClass, Class<?> stopclass) {
        List<ExtPropertyAccessor> all = new ArrayList<ExtPropertyAccessor>();
        Class<?> current = tClass;

        while (current != null && !current.equals(stopclass)) {
            System.err.println("Current: " + CACHE.get(current));
            all.addAll(CACHE.get(current));
            current = current.getSuperclass();
        }

        /**
         * prüfen nach doppelter eintragen und zuweisen fehlender methoden und
         * felder aus basisklasse
         */
        Map<String, ExtPropertyAccessor> res = new HashMap<String, ExtPropertyAccessor>();
        for (ExtPropertyAccessor pa : all) {
            System.err.println("pa name: " + pa.getName());
            System.err.println("Read: " + pa.getReadMethod());
            System.err.println("Write: " + pa.getWriteMethod());
            // XK: bad idea?
            final String name = pa.getName().substring(0, 1).toLowerCase() + pa.getName().substring(1);
            if (!res.containsKey(name)) {
                res.put(name, pa);
            } else {
                res.get(name).updateMissing(pa);
            }
        }

        return Collections.unmodifiableList(new ArrayList<ExtPropertyAccessor>(res.values()));

    }

    private static List<ExtPropertyAccessor> introspectInternal(Class<?> tClass) {
        final Map<String, ExtPropertyAccessor> mcoll = new HashMap<>();

        try {
            mcoll.putAll(
                Arrays.asList(Introspector.getBeanInfo(tClass, tClass.getSuperclass()).getPropertyDescriptors())
                        .stream()
                        .map(pd -> {
                            try {
                                ExtPropertyAccessor pa = new ExtPropertyAccessor(pd.getName());
                                LOG.finest("A) set read method " +pd.getReadMethod());
                                pa.setReadMethod(pd.getReadMethod());
                                LOG.finest("A) set write method " +pd.getWriteMethod());
                                pa.setWriteMethod(pd.getWriteMethod());
                                return pa;
                            } catch (Exception ex) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(pa -> pa.getName(), pa -> pa))
            );
            
        } catch (IntrospectionException ex) {
            LOG.log(Level.SEVERE, "intorspection failed, try to go ahead without Bean introspection" , ex);
        }

        HashSet<String> beanInfoProps = new HashSet<String>(mcoll.keySet());

        // TODO: test this part and change the logic
        for (Method m : tClass.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers())) {
                Matcher matcher = MPAT.matcher(m.getName());
                if (matcher.matches()) {
                    String propName = matcher.group(2);
                    if (propName.length() == 0) {
                        continue;
                    }

                    final String prefix = matcher.group(1);
                    int methodType;

                    if (prefix.equals("set") && m.getParameterTypes().length == 1 && m.getReturnType().equals(Void.TYPE)) {
                        methodType = 1;
                    } else if (prefix.equals("get") && m.getParameterTypes().length == 0 && !m.getReturnType().equals(Void.TYPE)) {
                        methodType = 2;
                    } else if (prefix.equals("is") && m.getParameterTypes().length == 0 && m.getReturnType().equals(Boolean.TYPE)) {
                        methodType = 3;
                    } else {
                        continue;
                    }

                    if (!beanInfoProps.contains(propName)) {
                        final ExtPropertyAccessor prop = new ExtPropertyAccessor(propName);
                        if (methodType == 1) {
                            LOG.finest("b) set write method " +m);
                            prop.setWriteMethod(m);
                        } else {
                            LOG.finest("b) set read method " +m);
                            prop.setReadMethod(m);
                        }
                        mcoll.put(propName, prop);
                        continue;
                    } else {
                        final ExtPropertyAccessor prop = mcoll.get(propName);

                        if (!prop.isWritable() && methodType == 1) {
                            LOG.finest("c) set write method " +m);
                            prop.setWriteMethod(m);
                        } else if (prop.getReadMethod() == null && methodType > 1) {
                            LOG.finest("c) set read method " +m);
                            prop.setReadMethod(m);
                        }
                    }
                }
            }
        }

        for (Field f : tClass.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            int modifier = f.getModifiers();
            String name = f.getName();
            Class<?> type = f.getType();

            if (Modifier.isPublic(modifier)) {
                ExtPropertyAccessor property = mcoll.get(name);
                property.setPublicField(f);
            } else {
                ExtPropertyAccessor property = mcoll.get(name);
                Class<?> propertyType;
                try {
                    propertyType = property.getPropertyType();
                    if (propertyType.equals(type)) {
                        property.setRelatedField(f);
                    }
                } catch (NullPointerException exception) {
                    // Nur Setter vorhanden, wird unten durch applyFilter aussortiert.
                }
            }
        }
        return mcoll.values().stream()
                .filter(pa -> pa.getReadMethod() != null || pa.getPublicField() != null)
                .collect(Collectors.toUnmodifiableList());

    }


    public static Class<?> resolveGenericType(Class<?> clazz) {
        return resolveGenericType(clazz, 0, Object.class);
    }

    public static Class<?> resolveGenericType(Class<?> clazz, int idx, Class<?> fallback) {
        Type t;
        try {
            Type[] ts = ((ParameterizedType) clazz.getGenericSuperclass()).getActualTypeArguments();
            if (ts.length <= idx) {
                return fallback;
            }
            t = ts[idx];
        } catch (TypeNotPresentException tnpe) {
            return fallback;
        }

        try {
            if (t instanceof ParameterizedType) {
                return (Class<?>) ((ParameterizedType) t).getRawType();
            }
            if (t instanceof TypeVariable) {
            	if (isPrintDebugInfo()) {
                    System.err.println("T     =" + t);
                    System.err.println("NAME  =" + ((TypeVariable<?>) t).getName());
                    System.err.println("BOUNDS=" + Arrays.asList(((TypeVariable<?>) t).getBounds()));
                    GenericDeclaration gd = ((TypeVariable<?>) t).getGenericDeclaration();
                    System.err.println("GD    =" + gd);
                    for (TypeVariable<?> v : gd.getTypeParameters()) {
                        System.err.println(v.getClass());
                    }
            	}
                return ((TypeVariable<?>) t).getName().getClass();
            }
            return (Class<?>) t;
        } catch (ClassCastException cce) {
            return fallback;
        }
    }

    /**
     * checks when ever a class is a root class in the XML document
     *
     * @param clazz
     * @return true if it is a root
     */
    public static boolean isRootClass(Class<? extends TXMLObject> clazz) {
        Annotation[] annoArray = clazz.getAnnotations();
        for (Annotation ann : annoArray) {
            if (ann instanceof XmlRootElement) {
                return true;
            }
        }
        return false;
    }

    public static String camelizeName(String attrName) {
        
//        // TODO REVIEW PROPOSAL
//        if (attrName == null || attrName.contains("_") ) {
//            return attrName;
//        }
//        StringTokenizer stok=new StringTokenizer(attrName, "_", false);
//        
//        StringBuilder sbuild = new StringBuilder();
//        while (stok.hasMoreElements()){
//            final String str=stok.nextToken().toLowerCase();
//            sbuild.append( Character.toUpperCase( str.charAt(0) ) ).append( str.substring(1) );
//        }
//        return sbuild.toString();
        

        if (attrName == null ) {
            throw new IllegalArgumentException("attrName cannot be null");
        }
        if (attrName.contains("_")) {
            StringBuilder sb = new StringBuilder();
            char[] array = attrName.toCharArray();
            boolean lastWasUnderScore = false;
            boolean isFirstChar = true;
            for (char c : array) {
                if (c == '_') {
                    if (isFirstChar == false) {
                        lastWasUnderScore = true;
                    }
                } else if (lastWasUnderScore) {
                    sb.append(Character.toUpperCase(c));
                    lastWasUnderScore = false;
                } else if (isFirstChar) {
                    sb.append( Character.toLowerCase(c) );
                } else {
                    sb.append(c);
                }
                isFirstChar = false;
            }
            return sb.toString();
        }
        return attrName;
    }

    public static String getSimpleClassName(String className) {
        if (className == null) {
            throw new IllegalArgumentException("className cannot be null");
        }
        int pos = className.lastIndexOf('$');
        if (pos == -1) {
            pos = className.lastIndexOf('.');
        }
        if (pos != -1) {
            return className.substring(pos + 1);
        } else {
            return className;
        }
    }

	public static boolean isPrintDebugInfo() {
		return printDebugInfo;
	}

	public static void setPrintDebugInfo(boolean printDebugInfo) {
		ReflectUtil.printDebugInfo = printDebugInfo;
	}

}
