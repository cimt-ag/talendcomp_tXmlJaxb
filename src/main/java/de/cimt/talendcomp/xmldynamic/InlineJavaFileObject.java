package de.cimt.talendcomp.xmldynamic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import org.apache.log4j.Logger;

/**
 *
 * @author dkoch
 */
public final class InlineJavaFileObject extends SimpleJavaFileObject implements JavaFileObject{
    private static final Logger LOG = Logger.getLogger("de.cimt.talendcomp.xmldynamic");
    public static final URI location;
    public static final int length;
    public static final boolean packed;
    
    public final String element;
    
    static{
         location=URI.create( InlineJavaFileObject.class.getProtectionDomain().getCodeSource().getLocation().toString() );
         length=location.toString().length();
         packed = new File(location).isFile();
        
    }
    public InlineJavaFileObject(String element) {
        super( packed ? location : location.resolve(element) , Kind.CLASS);
        this.element = element;
    }

    @Override
    public String getName() {
        return element.replaceAll("(^(!|\\.||/))|(\\.class$)*", "").replace('/', '.');
    }


    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        byte[] b=new byte[4096];
        StringBuffer buf=new StringBuffer();
        int pos;
        InputStream in=null;
        try{
            in=openInputStream();
            while( (pos=in.read(b))>0){
                buf.append( new String(b,0,pos) );
            }
        }finally{
            if(in!=null)
                in.close();
        }
        return b.toString();
    }

    @Override
    public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
        return new InputStreamReader(openInputStream());
    }

    @Override
    public InputStream openInputStream() throws IOException {
        if( packed ){
            ZipFile f=new ZipFile(new File( location ));
            return  f.getInputStream( new ZipEntry( element ) );
        }else{
            File folder=new File(location);
            return new FileInputStream( new File( location.resolve( element  )));
        }
    }

    @Override
    public URI toUri() {
        return packed ? URI.create(location.toString() + "!/" + element) :
                super.toUri();
    }
   
    
}
