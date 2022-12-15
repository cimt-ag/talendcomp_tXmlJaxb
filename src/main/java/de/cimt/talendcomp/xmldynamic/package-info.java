@jakarta.xml.bind.annotation.XmlSchema(
    namespace="http://www.cimt.de/talend",
    elementFormDefault = jakarta.xml.bind.annotation.XmlNsForm.QUALIFIED,
    xmlns = {
        @jakarta.xml.bind.annotation.XmlNs(prefix = "_cisp", namespaceURI = "http://xsd.cimt.de/plugins/inline"),
        @jakarta.xml.bind.annotation.XmlNs(prefix = "cimt", namespaceURI = "http://www.cimt.de/"),
        @jakarta.xml.bind.annotation.XmlNs(prefix = "xs", namespaceURI = "http://www.w3.org/2001/XMLSchema"),
    }
)
package de.cimt.talendcomp.xmldynamic;
