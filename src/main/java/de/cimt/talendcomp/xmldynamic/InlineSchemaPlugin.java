package de.cimt.talendcomp.xmldynamic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.namespace.QName;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import com.sun.codemodel.JAnnotationArrayMember;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JArray;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.fmt.JTextFile;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.model.CClassInfo;
import com.sun.tools.xjc.model.CPropertyInfo;
import com.sun.tools.xjc.model.CTypeInfo;
import com.sun.tools.xjc.model.Model;
import com.sun.tools.xjc.model.nav.NClass;
import com.sun.tools.xjc.outline.Aspect;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XSComponent;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSTerm;

import de.cimt.talendcomp.xmldynamic.annotations.QNameRef;
import de.cimt.talendcomp.xmldynamic.annotations.TXMLTypeHelper;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Daniel Koch <daniel.koch@cimt-ag.de>
 */
public class InlineSchemaPlugin extends Plugin {

    private static final String CODEFRAGMENT=           
        "    @Override\n" +
        "    public java.util.List<Class<TXMLObject>> getClasses(){\n" +
        "        java.util.List<Class<TXMLObject>> classes=new java.util.ArrayList<Class<TXMLObject>>();\n" +
        "        classes.addAll( java.util.Arrays.asList(this.getElements()) );\n" +
        "        classes.addAll( java.util.Arrays.asList(this.getTypes()) );\n" +
        "        return  classes;\n" +
        "    }\n" +
        "    \n" +
        "    public boolean matchesNamespace(javax.xml.namespace.QName qn){\n" +
        "        for(String ns : getNamespaces()){\n" +
        "            if(qn.getNamespaceURI().equalsIgnoreCase(ns))\n" +
        "                return true;\n" +
        "        }\n" +
        "        \n" +
        "        return false;\n" +
        "    }\n" +
        "    \n" +
        "    @Override\n" +
        "    public Class<TXMLObject> find(javax.xml.namespace.QName qn){\n" +
        "        final String nsuri= (qn.getNamespaceURI()!=null) ? qn.getNamespaceURI() : ANYNAMESPACE;\n" +
        "        \n" +
        "        if(matchesNamespace(qn)){\n" +
        "            for(Class<TXMLObject> c : getElements()){\n" +
        "                // only perform namespacecheck when required\n" +
        "                if(!ANYNAMESPACE.equals(nsuri)){\n" +
        "                    jakarta.xml.bind.annotation.XmlSchema schema=(jakarta.xml.bind.annotation.XmlSchema) c.getPackage().getAnnotation(jakarta.xml.bind.annotation.XmlSchema.class);\n" +
        "                    if(schema==null || !schema.namespace().equals( nsuri ))\n" +
        "                        continue;\n" +
        "                }\n" +
        "                \n" +
        "                jakarta.xml.bind.annotation.XmlElement elem=c.getAnnotation(jakarta.xml.bind.annotation.XmlElement.class);\n" +
        "                if(elem!=null && qn.getLocalPart().equals(elem.name()))\n" +
        "                    return c;\n" +
        "                jakarta.xml.bind.annotation.XmlRootElement rootElem=c.getAnnotation(jakarta.xml.bind.annotation.XmlRootElement.class);\n" +
        "                if(rootElem!=null && qn.getLocalPart().equals(rootElem.name()))\n" +
        "                    return c;\n" +
        "                \n" +
        "            }\n" +
        "        }\n" +
        "        return null;\n" +
        "        \n" +
        "    }\n" +
        "    \n" +
        "    @Override\n" +
        "    public boolean isMember(javax.xml.namespace.QName qn){\n" +
        "        return find(qn)!=null;\n" +
        "    }\n" ;
    
    
    public static final QName PNS = new QName("http://xsd.cimt.de/plugins/inline", "_cisp", "_cisp");
    private static final Logger LOG = Logger.getLogger("de.cimt.talendcomp.xmldynamic");
    StringBuilder clazzes=new StringBuilder();
    
    @Override
    public String getOptionName() {
        return PNS.getLocalPart();
    }

    @Override
    public String getUsage() {
        return "handle with care. Ask Daniel ;-)";
    }

    @Override
    public void postProcessModel(Model model, ErrorHandler errorHandler) {
        try {
            super.postProcessModel(model, errorHandler);
//            model.codeModel.

            final JClass refClass = model.codeModel.ref(Class.class);
            final JClass refObject = model.codeModel.ref(TXMLObject.class);
            final JClass refString = model.codeModel.ref(String.class);

            model.rootClass = refObject;

            Map<JPackage, JArray> e = new AutoMap<JPackage, JArray>(){
                @Override
                public JArray create(JPackage p) {
                    return JExpr.newArray(refClass.narrow(refObject));
                }
            };
            Map<JPackage, JArray> t = new AutoMap<JPackage, JArray>(){
                @Override
                public JArray create(JPackage p) {
                    return JExpr.newArray(refClass.narrow(refObject));
                }
            };
            Map<JPackage, JArray> n = new AutoMap<JPackage, JArray>(){
                @Override
                public JArray create(JPackage p) {
                    return JExpr.newArray(refString);
                }
            };           
            Map<JPackage, Set<String>> namespaces = new AutoMap<JPackage, Set<String>>(){
                @Override
                public Set<String> create(JPackage p) {
                    return new HashSet<String>();
                }
            };
            Set<JPackage> packages = new HashSet<JPackage>();
            for (Map.Entry<NClass, CClassInfo> beanset : model.beans().entrySet()) {
                
                CClassInfo bean = beanset.getValue();
                final JPackage ownerPackage = bean.getOwnerPackage();
                packages.add(ownerPackage);
                if (bean.getElementName() != null) {
                    e.get(ownerPackage).add(JExpr.dotclass(model.codeModel.ref(bean.fullName())));
                    final String ns = bean.getElementName().getNamespaceURI();
                    if (!namespaces.get(ownerPackage).contains(ns)) {
                        namespaces.get(ownerPackage).add(bean.getElementName().getNamespaceURI());
                        n.get(ownerPackage).add(JExpr.lit(ns));
                    }
                } else {
                    t.get(ownerPackage).add(JExpr.dotclass(model.codeModel.ref(bean.fullName())));
                }
            }
            
            StringBuilder sbuild=new StringBuilder(); 
            for(JPackage pack : packages){
                model.rootClass = refObject;

                final String ctx = "GenXS" + UUID.randomUUID().toString().replaceAll("[:\\.-]+", "");

                JDefinedClass clazz = pack._class(ctx);
                clazz._implements(model.codeModel.ref(TXMLBinding.class));

                // override getTimespamp
                JMethod meth = clazz.method(JMod.PUBLIC, model.codeModel.LONG, "getTimestamp");
                meth.annotate(java.lang.Override.class);
                meth.body()._return(JExpr.lit(System.currentTimeMillis()));

                meth = clazz.method(JMod.PUBLIC, refClass.narrow(refObject).array(), "getElements");
                meth.annotate(java.lang.Override.class);
                meth.body()._return(JExpr.cast(refClass.narrow(refObject).array(), e.get(pack)));
                JAnnotationUse annotate = meth.annotate(java.lang.SuppressWarnings.class);
                annotate.param("value", "unchecked");

                meth = clazz.method(JMod.PUBLIC, refClass.narrow(refObject).array(), "getTypes");
                meth.annotate(java.lang.Override.class);
                annotate = meth.annotate(java.lang.SuppressWarnings.class);
                annotate.param("value", "unchecked");
                meth.body()._return(JExpr.cast(refClass.narrow(refObject).array(), t.get(pack)));

                meth = clazz.method(JMod.PUBLIC, refString.array(), "getNamespaces");
                meth.annotate(java.lang.Override.class);
                meth.body()._return(n.get(pack));
                
                clazz.direct( CODEFRAGMENT );
                sbuild.append( clazz.fullName() ).append("\n");
               
            }


            JTextFile jrf = (JTextFile) model.codeModel._package("META-INF.services").addResourceFile(new JTextFile("de.cimt.talendcomp.xmldynamic.TXMLBinding"));
            jrf.setContents( sbuild.toString() );//clazz.fullName());

        } catch (JClassAlreadyExistsException ex) {
        	LOG.log(Level.SEVERE,"JClassAlreadyExistsException", ex);
        }

    }

    @Override
    public List<String> getCustomizationURIs() {
        return Arrays.asList(PNS.getNamespaceURI());
    }

    /**
     * this method is used to gerenrate qnamerefs for each property of a class. if a collection
     * is found all possible members are added.
     * @param component current definition
     * @param parent annotation to be filled
     * @param outline current working outline
     * @param ref list of possible elements
     */
    private void annotateType(XSComponent component, JAnnotationArrayMember parent, Outline outline, List<? extends CTypeInfo> ref) {
    	if (ref.isEmpty()) {
    		return; // get value does not support type annotations
    	}
        if ( XSAttributeUse.class.isAssignableFrom(component.getClass()) ) {
            JAnnotationUse annotate = parent.annotate(QNameRef.class);
            annotate.param("uri", ((XSAttributeUse) component).getDecl().getTargetNamespace());
            annotate.param("name", ((XSAttributeUse) component).getDecl().getName());
             
            annotate.param("type", ref.get(0).toType(outline, Aspect.EXPOSED));
            annotate.param("attribute", true);
            return;
        }
        if ( XSElementDecl.class.isAssignableFrom(component.getClass()) ) {
            JAnnotationUse annotate = parent.annotate(QNameRef.class);
            annotate.param("name", ((XSElementDecl) component).getName());
            annotate.param("type", ref.get(0).toType(outline, Aspect.EXPOSED));
            annotate.param("uri", ((XSElementDecl) component).getTargetNamespace());
            return;
        }
        if ( XSParticle.class.isAssignableFrom(component.getClass()) ) {
            XSTerm term = ((XSParticle) component).getTerm();
            if ( term.isElementDecl() ) {
                annotateType(term.asElementDecl(), parent, outline, ref);
                return; 
            }
            
            if (term.isModelGroupDecl()) {
                term = term.asModelGroupDecl();
            }
            if ( term.isModelGroup() ) {
                int i = 0;
                for (XSParticle child : term.asModelGroup().getChildren()) {
                    annotateType(child, parent, outline, ref.subList(i++, ref.size()));
                }
            }
        }
    }
    
    @Override
    public boolean run(Outline otln, Options optns, ErrorHandler eh) throws SAXException {
        
        for (ClassOutline co : otln.getClasses()) {
            for (CPropertyInfo property : co.target.getProperties()) {
                JFieldVar field = co.implClass.fields().get(property.getName(false));
                if (field == null) {
                    continue;
                }
                JAnnotationUse annotate = field.annotate(TXMLTypeHelper.class);
                if (property.isCollection()) {
                    annotate.param("collection", true);
                }
                annotateType(property.getSchemaComponent(), annotate.paramArray("refs"), otln, new ArrayList<CTypeInfo>(property.ref()) );
            }
        }
        return true;
    }

    
}
