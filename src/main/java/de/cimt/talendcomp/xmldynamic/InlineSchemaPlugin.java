package de.cimt.talendcomp.xmldynamic;

import de.cimt.talendcomp.xmldynamic.annotations.TXMLTypeHelper;
import de.cimt.talendcomp.xmldynamic.annotations.QNameRef;
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
import com.sun.codemodel.fmt.JTextFile;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.model.Aspect;
import com.sun.tools.xjc.model.CClassInfo;
import com.sun.tools.xjc.model.CPropertyInfo;
import com.sun.tools.xjc.model.CTypeInfo;
import com.sun.tools.xjc.model.Model;
import com.sun.tools.xjc.model.nav.NClass;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XSComponent;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSTerm;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 *
 * @author Daniel Koch <daniel.koch@cimt-ag.de>
 */
public class InlineSchemaPlugin extends Plugin {

    public static final QName PNS = new QName("http://xsd.cimt.de/plugins/inline", "_cisp", "_cisp");
    private static final Logger LOG = Logger.getLogger(InlineSchemaPlugin.class);
    
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

            final JClass refClass = model.codeModel.ref(Class.class);
            final JClass refObject = model.codeModel.ref(TXMLObject.class);
            final JClass refString = model.codeModel.ref(String.class);

            model.rootClass = refObject;
            final String ctx = "GenXS" + UUID.randomUUID().toString().replaceAll("[:\\.-]+", "");

            JDefinedClass clazz = model.codeModel._class("de.cimt.talendcomp.xmldynamic." + ctx);
            clazz._implements(model.codeModel.ref(TXMLBinding.class));
            clazz._extends(model.codeModel.ref(InternalTXMLBindingHelper.class));

            // override getTimespamp
            JMethod meth = clazz.method(JMod.PUBLIC, model.codeModel.LONG, "getTimestamp");
            meth.annotate(java.lang.Override.class);
            meth.body()._return(JExpr.lit(System.currentTimeMillis()));

            JArray e = JExpr.newArray(refClass.narrow(refObject));
            JArray t = JExpr.newArray(refClass.narrow(refObject));
            JArray n = JExpr.newArray(refString);

            Set<String> namespaces = new HashSet<String>();
            for (Map.Entry<NClass, CClassInfo> beanset : model.beans().entrySet()) {
                CClassInfo bean = beanset.getValue();
                if (bean.getElementName() != null) {
                    e.add(JExpr.dotclass(model.codeModel.ref(bean.fullName())));
                    final String ns = bean.getElementName().getNamespaceURI();
                    if (!namespaces.contains(ns)) {
                        namespaces.add(bean.getElementName().getNamespaceURI());
                        n.add(JExpr.lit(ns));
                    }
                } else {
                    t.add(JExpr.dotclass(model.codeModel.ref(bean.fullName())));
                }
            }

            meth = clazz.method(JMod.PUBLIC, refClass.narrow(refObject).array(), "getElements");
            meth.annotate(java.lang.Override.class);
            meth.body()._return(JExpr.cast(refClass.narrow(refObject).array(), e));

            meth = clazz.method(JMod.PUBLIC, refClass.narrow(refObject).array(), "getTypes");
            meth.annotate(java.lang.Override.class);
            meth.body()._return(JExpr.cast(refClass.narrow(refObject).array(), t));

            meth = clazz.method(JMod.PUBLIC, refString.array(), "getNamespaces");
            meth.annotate(java.lang.Override.class);
            meth.body()._return(n);

            JTextFile jrf = (JTextFile) model.codeModel._package("META-INF.services").addResourceFile(new JTextFile("de.cimt.talendcomp.xmldynamic.TXMLBinding"));
            jrf.setContents(clazz.fullName());

        } catch (JClassAlreadyExistsException ex) {
        	LOG.error(ex);
        }

    }

    @Override
    public List<String> getCustomizationURIs() {
        return Arrays.asList(PNS.getNamespaceURI());
    }

    private void annotateType(XSComponent component, JAnnotationArrayMember parent) {
        if ( XSAttributeUse.class.isAssignableFrom(component.getClass()) ) {
            JAnnotationUse annotate = parent.annotate(QNameRef.class);
            annotate.param("name", ((XSAttributeUse) component).getDecl().getName());
            annotate.param("uri", ((XSAttributeUse) component).getDecl().getTargetNamespace());
            annotate.param("attribute", true);
            return;
        }
        if ( XSElementDecl.class.isAssignableFrom(component.getClass()) ) {
            JAnnotationUse annotate = parent.annotate(QNameRef.class);
            annotate.param("name", ((XSElementDecl) component).getName());
            annotate.param("uri", ((XSElementDecl) component).getTargetNamespace());
        }
        if ( XSParticle.class.isAssignableFrom(component.getClass()) ) {
            XSTerm term = ((XSParticle) component).getTerm();
            if ( term.isElementDecl() ) {
                annotateType(term.asElementDecl(), parent);
                return; 
            }
            
            if (term.isModelGroupDecl()) {
                term=term.asModelGroupDecl(); //.getModelGroup()
            }
            if ( term.isModelGroup() ) {
                for (XSParticle child : term.asModelGroup().getChildren()) {
                    annotateType(child, parent);
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
                    final JAnnotationArrayMember paramArray = annotate.paramArray("componentClasses");
                    for (CTypeInfo ti : property.ref()) {
                        paramArray.param(ti.toType(otln, Aspect.EXPOSED));
                        annotateType(property.getSchemaComponent(), annotate.paramArray("references"));
                    }
                } else {
                    annotateType(property.getSchemaComponent(), annotate.paramArray("references"));
                }
            }
        }
        return true;
    }

}