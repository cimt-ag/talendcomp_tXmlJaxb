package de.cimt.talendcomp.xmldynamic;

import java.io.File;


import com.sun.codemodel.JCodeModel;

/**
 *
 * @author Jan Lolling, Daniel Koch
 */
public class TML {

	public static void main(String[] args) throws Exception {
		
		org.apache.log4j.BasicConfigurator.configure();
		play_read();
	}
	
	public static void play_read() throws Exception {

		XJCOptions opts = new XJCOptions();
		opts.targetDir = new File("./target/generated-test/modelbuilder/");
		opts.targetDir.mkdirs();
		opts.ignoreAnnotations = true;
		opts.forceGenerate = true;
                opts.compileSource = true;
                
		opts.addGrammar(new File("./src/test/resources/company2.xsd"));
		System.out.println("Generate model...");
		//ModelBuilder.generate(opts, new JCodeModel());
		
		ModelBuilder mb = new ModelBuilder(opts, new JCodeModel());
		mb.generate();
		
	}
	
	public static void play_input() throws Exception {
		
	}
	
}
