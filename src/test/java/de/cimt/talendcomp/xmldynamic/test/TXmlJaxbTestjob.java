package de.cimt.talendcomp.xmldynamic.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.sun.codemodel.JCodeModel;

//import de.cimt.talend.mock.TalendJobMock; // TODO: removed until available from mvn repo
import de.cimt.talendcomp.xmldynamic.JarUtil;
import de.cimt.talendcomp.xmldynamic.ModelBuilder;
import de.cimt.talendcomp.xmldynamic.ReflectUtil;
import de.cimt.talendcomp.xmldynamic.TXMLObject;
import de.cimt.talendcomp.xmldynamic.Util;
import de.cimt.talendcomp.xmldynamic.XJCOptions;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TXmlJaxbTestjob {

	String currentComponent ;
	Map<String, Object> globalMap = new HashMap<String, Object>();
    private boolean classesLoaded = false;

	@Before
	public void testCreateModel() throws Exception {
		Logger rootLogger = LoggerFactory.getLogger("de.cimt.talendcomp.xmldynamic.test");
		if (classesLoaded == false) {
			String classRootPath = "./target/generated-sources/modelbuilder/";
			File classRootPathFile = new File(classRootPath);
			String xsdFilepath = "./src/test/resources/company.xsd";
			String jarFilepath = "./target/test/resources/company.xsd.jar";
			File xsdFile = new File(xsdFilepath);
			File jarFile = new File(jarFilepath);
			XJCOptions opts = new XJCOptions();
			opts.targetDir = new File(classRootPathFile, xsdFile.getName());
			opts.targetDir.mkdirs();
			opts.extendClasspath = true;
			opts.ignoreAnnotations = true;
			opts.forceGenerate = true;
			opts.addGrammar(new File(xsdFile.getAbsolutePath()));
			System.out.println("Generate model...");

			//testen ob das jar file älter als das xsd file
			if (xsdFile.lastModified() > jarFile.lastModified()) {
				//wenn älter muss der generate gestartet werden und das jar file neu erstellt werden
				//ModelBuilder.buildJar(opts, String jarFilePath);
				JarUtil buildJar = new JarUtil();
				buildJar.setClassFilesRootDir(classRootPath);
				buildJar.setJarFilePath(jarFilepath);
				buildJar.create();
			}
			ModelBuilder.generate(opts, new JCodeModel());
			Util.printContexts();
		}
		classesLoaded = true;
	}

	@Test
	public void testLoadCustomerClass() throws Exception {
		currentComponent = "tXmlJaxbOutput";
		String className = "de.cimt.customer.Customer";
		TXMLObject object = (TXMLObject) Class.forName(className).newInstance();
		String expected = "Lukas";
		if (object.set("name", expected) == false) {
			throw new Exception("attribute does not exists.");
		}
		globalMap.put("tXmlJaxbOutput_1", object);
		String actual = (String) object.get("name");
		assertEquals(expected, actual);
	}

	@Test
	public void testSetDate() throws Exception {
		currentComponent = "tXmlJaxbOutput";
		String className = "de.cimt.customer.Customer";
		TXMLObject object = (TXMLObject) Class.forName(className).newInstance();
		Date expected = new Date();
		if (object.set("age", expected) == false) {
			throw new Exception("attribute does not exists.");
		}
		globalMap.put("tXmlJaxbOutput_1", object);
		Date actual = (Date) object.get("age");
		assertEquals(expected, actual);
	}

	@Test
	public void testLoadWrongCustomer() throws Exception {
		currentComponent = "tXmlJaxbOutput";
		String className = "de.cimt.customer.Customer";
		TXMLObject object = (TXMLObject) Class.forName(className).newInstance();
		String expected = "Lukas";
		String unexpected = "Jan";
		if (object.set("first_name", expected) == false) {
			throw new Exception("attribute does not exists.");
		}
		globalMap.put("tXmlJaxbOutput_1", object);
		String actual = (String) object.get("first_name");
		assertNotSame(unexpected, actual);
	}

	@Test
	public void testCamelizer() {
		String test = "First_name_";
		String expected = "firstName";
		String actual = ReflectUtil.camelizeName(test);
		System.out.println(actual);
		assertEquals(expected, actual);
	}

	@Test
	public void testLoadAdressClass() throws Exception {
		testLoadCustomerClass();
		String className = "de.cimt.customer.Customer$Address";
		String parentComponent = "tXmlJaxbOutput_1";
		TXMLObject parent = (TXMLObject) globalMap.get(parentComponent);
		String parentAttributeName = "address";
		int expectedCount = 5;
		for (int i = 0; i < expectedCount; i++) {
			TXMLObject object = (TXMLObject) Class.forName(className).newInstance();
			String expected = "Berlin" + i;
			object.set("city", expected);
			globalMap.put("tXmlJaxbOutput_2", object);
			String actual = (String) object.get("city");
			assertEquals(expected, actual);
			parent.addOrSet(parentAttributeName, object);
			parent.addOrSet("title", "t" + i);
		}
		System.out.println(parent.toXML(true));
		int actualCount = parent.size("title");
		assertEquals(expectedCount, actualCount);
	}

	@Test
	public void testBuildSQLInClauseFromString() {
		String expected = "in ('a','b','c')";
		List<String> values = new ArrayList<String>();
		values.add("a");
		values.add("b");
		values.add("c");
		String actual = Util.buildSQLInClause(values);
		System.out.println(actual);
		assertTrue("SQL code wrong", expected.equals(actual.trim()));
	}

	@Test
	public void testReadCompany() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
			    + "<ns2:company xmlns:ns2=\"http://cimt.de/customer/\">\n"
			    + "    <ns2:name>cimt</ns2:name>\n"
			    + "    <ns2:customer>\n"
			    + "        <ns2:id>A</ns2:id>\n"
			    + "        <ns2:name>Heinz</ns2:name>\n"
			    + "        <ns2:address>\n"
			    + "            <ns2:id>2</ns2:id>\n"
			    + "            <ns2:city>Düsseldorf</ns2:city>\n"
			    + "        </ns2:address>\n"
			    + "        <ns2:poaddress>\n"
			    + "            <ns2:id>2</ns2:id>\n"
			    + "            <ns2:_house_number xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xsi:type=\"xs:int\">22</ns2:_house_number>\n"
			    + "            <ns2:_house_number xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xsi:type=\"xs:int\">21</ns2:_house_number>\n"
			    + "            <ns2:city>Tegel</ns2:city>\n"
			    + "        </ns2:poaddress>\n"
			    + "        <ns2:age>2016-07-14+02:00</ns2:age>\n"
			    + "    </ns2:customer>\n"
			    + "    <ns2:customer>\n"
			    + "        <ns2:id>B</ns2:id>\n"
			    + "        <ns2:name>Jan</ns2:name>\n"
			    + "        <ns2:age>2016-07-14+02:00</ns2:age>\n"
			    + "        <ns2:title>Dr.</ns2:title>\n"
			    + "        <ns2:title>Mr.</ns2:title>\n"
			    + "    </ns2:customer>\n"
			    + "</ns2:company>";
		TXMLObject root = Util.unmarshall(xml);
		System.out.println(root.getClass().getName());
		String expected = "cimt";
		String actual = (String) root.get("name");
		System.out.println("Check name attribute...");
		assertEquals(expected, actual);
		System.out.println("Check skip over array error...");
		try {
			Util.getTXMLObjects(root, "customer.address", false, false);
			assertTrue("Do not have detected overskipped array!", false);
		} catch (Exception e) {
			System.err.println(e);
		}
		System.out.println("Check customer list attribute size...");
		List<TXMLObject> results = Util.getTXMLObjects(root, "customer", false, false);
		int countExpected = 2;
		int countActual = results.size();
		assertEquals(countExpected, countActual);

	}

}
