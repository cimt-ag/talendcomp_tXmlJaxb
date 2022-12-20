package de.cimt.talendcomp.xmldynamic;

import java.io.File;

import com.sun.codemodel.JCodeModel;

/**
 *
 * @author Jan Lolling, Daniel Koch
 */
public class TMLAppsolute {

    public static void main(String[] args) throws Exception {
        play_read();
    }

    public static void play_read() throws Exception {
        XJCOptions opts = new XJCOptions();
        opts.targetDir = new File("./target/generated-test/appsolute/");
        opts.targetDir.mkdirs();
        opts.ignoreAnnotations = true;
        opts.forceGenerate = true;
        opts.compileSource = true;
        opts.addGrammar(new File("./src/test/resources/aptAppsolute_1.0.xsd"));
        opts.createJar=true;
        opts.jarFilePath="./target/generated-test/aptAppsolute_1.0.jar";
        System.out.println("Generate model...");
        //ModelBuilder.generate(opts, new JCodeModel());

        ModelBuilder mb = new ModelBuilder(opts, new JCodeModel());
        mb.generate();
        mb.load();

        String context_p_operation="LinkAudit";
        
        final TXMLObject root = Util.createTXMLObject("org.talend.service." + context_p_operation  );
        
        
//        Iterator<TXMLBinding> iter=Util.load();
//        while(iter.hasNext()){
//            final TXMLBinding next = iter.next();
//
//        }
        for(int i=0;i<10;i++){
            TXMLObject n=Util.createTXMLObject("org.talend.service." + context_p_operation + "$Data")  ;
//            if(i==0){
//                ReflectUtil.introspect(n.getClass()).stream().forEach( pa -> System.err.println( pa ) );
//            }
            n.set("radiomodel", i);
//            n.set("sitea", 3);
//            n.set("radio_model", "als");
            
            root.addOrSet(n);
        }
        System.out.println( root.toXML() );
    }

}
