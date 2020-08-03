package de.cimt.talendcomp.xmldynamic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

public class UtilOSGi
{
	private final Bundle bundle;

	private static final Logger LOG = Logger.getLogger(UtilOSGi.class.getName());

	private UtilOSGi(Bundle bundle)
	{
		this.bundle = bundle;
	}

	public Iterator<TXMLBinding> load() throws IOException, ClassNotFoundException, ClassCastException, InstantiationException, IllegalAccessException
	{
		LOG.info("Loading bindings");
		List<TXMLBinding> bindings = new ArrayList<TXMLBinding>();
		URL url = bundle.getEntry("META-INF/services/de.cimt.talendcomp.xmldynamic.TXMLBinding");
		LOG.info("Found resource: " + url.toString());
		String[] classNames = getClassNames(url);
		for(String className : classNames)
		{
			LOG.info("Resolved class name: " + className);
			Class<? extends TXMLBinding> clazz = getClass(className);
			LOG.info("Loaded class: " + clazz.getCanonicalName());
			TXMLBinding binding = clazz.newInstance();
			bindings.add(binding);
		}
		return bindings.iterator();
	}

    public Class<?> loadClass(final String name) throws ClassNotFoundException, NoClassDefFoundError
    {
    	ClassLoader classLoader = bundle.getClass().getClassLoader();
    	return classLoader.loadClass(name);

    }
   	private Class<? extends TXMLBinding> getClass(String className) throws ClassNotFoundException, ClassCastException
	{
		return (Class<? extends TXMLBinding>) bundle.loadClass(className);
	}

	private String[] getClassNames(URL url) throws IOException
	{
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		InputStream is = url.openStream();
		try
		{
			byte[] buf = new byte[1024];
			for (int read = is.read(buf); read != -1; read = is.read(buf))
			{
				os.write(buf, 0, read);
			}
			String content = new String(os.toByteArray(), "UTF-8").trim();
			String[] names = content.split("\n");
			for(int i=0; i<names.length; i++)
			{
				names[i] = names[i].trim();
			}
			return names;
		}
		finally
		{
			is.close();
		}
	}

	public static UtilOSGi getUtilOSGi()
	{
		try
		{
			Bundle bundle = ((BundleReference) Util.class.getClassLoader()).getBundle();
			return new UtilOSGi(bundle);
		}
		catch (Throwable e)
		{
			return null;
		}

	}
}
