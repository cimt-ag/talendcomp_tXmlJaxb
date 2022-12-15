package de.cimt.talendcomp.xmldynamic;

import java.io.File;

import com.sun.codemodel.JCodeModel;
import java.util.Iterator;

/**
 *
 * @author Jan Lolling, Daniel Koch
 */
public class TML {

	public static void main(String[] args) throws Exception {
		
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
                opts.createJar=true;
                opts.jarFilePath="./target/generated-test/model.jar";
		System.out.println("Generate model...");
		//ModelBuilder.generate(opts, new JCodeModel());
		
		ModelBuilder mb = new ModelBuilder(opts, new JCodeModel());
		mb.generate();
                mb.load();
                
                TXMLObject c=Util.createTXMLObject("de.cimt.customer.Customer");
                
                Iterator<TXMLBinding> iter=Util.load();
                while(iter.hasNext()){
                    final TXMLBinding next = iter.next();
                    
                }
                for(int i=0;i<10;i++){
                    TXMLObject add=Util.createTXMLObject("de.cimt.customer.Customer$Address") ;
                    
//                    add.addOrSet(add);
                    c.addOrSet(add);
                }
                System.out.println( c.toXML() );
	}
	
	public static void play_input() throws Exception {
		
	}
	
}
