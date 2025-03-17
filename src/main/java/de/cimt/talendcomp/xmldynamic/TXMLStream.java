package de.cimt.talendcomp.xmldynamic;

/**
 * simple implementation of a writer used to write a sequence of TXMLObject objects
 * to an underlayin stream.
 *
 * &lt;xsd:complexType name="TXMLStream"> &lt;xsd:choice maxOccurs="unbounded">
 * &lt;xsd:any namespace="##targetNamespace"/> &lt;xsd:any namespace="##other"/>
 * &lt;/xsd:choice> &lt;/xsd:complexType>
 *
 * @author dkoch
 */
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
//import jakarta.xml.bind.JAXBException;
import javax.xml.namespace.QName;

public class TXMLStream<T extends TXMLObject> {

    private final ArrayBlockingQueue<T> queue;
    private final OutputStream out;
    private final QName fqname;
    private boolean closed = false;
    private boolean formatted = false;
    private Thread worker=null;

    private class XMLStreamWriter implements Runnable {

        private boolean first = true;

        @Override
        public void run() {
            final String name= (fqname.getPrefix()!=null) ? (fqname.getPrefix() + ":" + fqname.getLocalPart()) : fqname.getPrefix();
            final String nsprefix= fqname.getPrefix()!=null ? (":" + fqname.getPrefix() ) : "";
            
            try {
                if (first) {
                    out.write(("<" + name + "xmlns"+nsprefix+"=\"" + fqname.getNamespaceURI() + "\">").getBytes());
                    first = false;
                }
                while (!closed) {
                    while (!queue.isEmpty()) {
                        try {
                            out.write(queue.poll(10, TimeUnit.SECONDS).toXML(formatted, true).getBytes());
                            out.flush();
                        } catch (Exception ex) {
                            throw new IOException(ex);
                        }
                    }
                }
                out.write(("</" + name+ ">").getBytes());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } finally {
                try {
                    out.close();
                } catch (IOException ex) {
                }

            }
        }
    }

    public TXMLStream(QName fqname, OutputStream out, int size) {
        queue = new ArrayBlockingQueue<T>(size, true);
        this.out = out;
        this.fqname = fqname;

    }

    public TXMLStream(QName fqname, OutputStream out) {
        this(fqname, out, 1024);
    }

    public boolean write(T object) throws IOException {
        if(closed){
            throw new IOException("closed stream");
        }
        if(worker==null){
            worker=new Thread(new XMLStreamWriter());
            worker.start();
        }
        return queue.add(object);
    }

    public void close() throws IOException {
        closed = true;
    }

    public boolean isFormatted() {
        return formatted;
    }

    public void setFormatted(boolean formatted) {
        this.formatted = formatted;
    }

}
