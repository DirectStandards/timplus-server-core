package org.jivesoftware.openfire.spi;


import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.Socket;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.KeyStore.Builder;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;

import org.apache.commons.lang3.StringUtils;
import org.jivesoftware.util.ReferenceIDUtil;

/**
 * Custom TLS connection key manager that select certificates based on 
 * TIM+ specific requirements.
 * @author gm2552
 *
 */
public class ServerConnectionKeyManager extends X509ExtendedKeyManager implements X509KeyManager
{
	private static final int DNSName_TYPE = 2; // name type constant for Subject Alternative name domain name	
	
    private final List<Builder> builders;
	
    private final Map<String,Reference<PrivateKeyEntry>> entryCacheMap;

    public ServerConnectionKeyManager(Builder builder) 
    {
        this(Collections.singletonList(builder));
    }

    public ServerConnectionKeyManager(List<Builder> builders) {
        this.builders = builders;

        entryCacheMap = Collections.synchronizedMap
                        (new SizedMap<String,Reference<PrivateKeyEntry>>());
    }

    public String chooseEngineClientAlias(String[] keyType,
            Principal[] issuers, SSLEngine engine) 
    {
    	for (String type : keyType)
    	{
    		String retVal = chooseEngineServerAlias(type, issuers, engine);
    		if (!StringUtils.isEmpty(retVal))
    			return retVal;
    	}
    	
    	return null;
    }
    
    public String chooseEngineServerAlias(String keyType,
            Principal[] issuers, SSLEngine engine) 
    {
    	// Make sure we have a connected domain associated with the thread.
    	final String referenceId = ReferenceIDUtil.getSessionReferenceId(engine.getSession());
    	if (StringUtils.isEmpty(referenceId))
    		return null;
    	
    	for (int i = 0, n = builders.size(); i < n; i++)
    	{
    		final Builder builder = builders.get(i);
    		try
    		{
    			final KeyStore ks = builder.getKeyStore();

	    		if (ks == null)
	    			continue;
	    		
		    	// find the certificates that matches the connection domain
		    	for (Enumeration<String> e = ks.aliases(); e.hasMoreElements(); )
		    	{
		    		final String alias = e.nextElement();
		            if (ks.isKeyEntry(alias) == false) 
		            {
		                continue;
		            }
		            
		            final Certificate[] chain = ks.getCertificateChain(alias);
		            if ((chain == null) || (chain.length == 0)) 
		            {
		                // must be secret key entry, ignore
		                continue;
		            }
		            
		            boolean incompatible = false;
		            for (Certificate cert : chain) 
		            {
		                if (cert instanceof X509Certificate == false) 
		                {
		                    // not an X509Certificate, ignore this alias
		                    incompatible = true;
		                    break;
		                }
		            }
		            if (incompatible) 
		            {
		                continue;
		            }
		            
		            final X509Certificate cert = (X509Certificate)chain[0];
		            
		            if (!cert.getPublicKey().getAlgorithm().equals(keyType))
		            	continue;
		            
		            // Check the binding between connection domain name and the SAN extensions
		            final Collection<List<?>> subjAltNames = cert.getSubjectAlternativeNames();
		            if (subjAltNames != null) 
		            {
		                for ( List<?> next : subjAltNames) 
		                {
		                    if (((Integer)next.get(0)).intValue() == DNSName_TYPE) 
		                    {
		                        String dnsName = (String)next.get(1);
		                        if (referenceId.toLowerCase().equals(dnsName.toLowerCase())) 
		                        {
		                            return alias;
		                        }
		                    }
		                }
		            }
		    	}
    		}
    		catch (Exception e)
    		{
    			
    		}
    	}
    	
    	return null;
    }
    
    private static class SizedMap<K,V> extends LinkedHashMap<K,V> 
    {
		private static final long serialVersionUID = 397198051333345918L;

		@Override protected boolean removeEldestEntry(Map.Entry<K,V> eldest) 
        {
            return size() > 10;
        }
    }

	@Override
	public String[] getClientAliases(String keyType, Principal[] issuers)
	{
        if (keyType == null) 
        	return null;

        final List<String> results = new ArrayList<>();

        for (int i = 0, n = builders.size(); i < n; i++) 
        {
            try 
            {
            	final KeyStore ks = builders.get(i).getKeyStore();
		    	// find the certificates that matches the connection domain
		    	for (Enumeration<String> e = ks.aliases(); e.hasMoreElements(); )
		    	{
		    		final String alias = e.nextElement();
		            if (ks.isKeyEntry(alias) == false) 
		            {
		                continue;
		            }
		            
		            final Certificate[] chain = ks.getCertificateChain(alias);
		            if ((chain == null) || (chain.length == 0)) 
		            {
		                // must be secret key entry, ignore
		                continue;
		            }
		            
		            boolean incompatible = false;
		            for (Certificate cert : chain) 
		            {
		                if (cert instanceof X509Certificate == false) 
		                {
		                    // not an X509Certificate, ignore this alias
		                    incompatible = true;
		                    break;
		                }
		            }
		            if (incompatible) 
		            {
		                continue;
		            }
		            
		            final X509Certificate cert = (X509Certificate)chain[0];
		            
		            if (!cert.getPublicKey().getAlgorithm().equals(keyType))
		            	continue;            	
            	

		            results.add(alias);
                }
            } 
            catch (Exception e) 
            {
                // ignore
            }
        }

        return results.toArray(new String[results.size()]);
	}

	@Override
	public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getServerAliases(String keyType, Principal[] issuers)
	{
		return getClientAliases(keyType, issuers);
	}

	@Override
	public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public X509Certificate[] getCertificateChain(String alias)
	{
        PrivateKeyEntry entry = getEntry(alias);
        return entry == null ? null :
                (X509Certificate[])entry.getCertificateChain();
	}

	@Override
	public PrivateKey getPrivateKey(String alias)
	{
        PrivateKeyEntry entry = getEntry(alias);
        return entry == null ? null : entry.getPrivateKey();
	}

    private PrivateKeyEntry getEntry(String alias) {
        // if the alias is null, return immediately
        if (alias == null) {
            return null;
        }

        // try to get the entry from cache
        Reference<PrivateKeyEntry> ref = entryCacheMap.get(alias);
        PrivateKeyEntry entry = (ref != null) ? ref.get() : null;
        if (entry != null) {
            return entry;
        }

        for (int i = 0, n = builders.size(); i < n; i++) 
        {
            try 
            {
            	final KeyStore ks = builders.get(i).getKeyStore();

            	final Builder builder = builders.get(i);

            	Entry newEntry = ks.getEntry
            			(alias, builder.getProtectionParameter(alias));
	            if (newEntry instanceof PrivateKeyEntry == false) 
	            {
	                // unexpected type of entry
	                continue;
	            }
	            entry = (PrivateKeyEntry)newEntry;
	            entryCacheMap.put(alias, new SoftReference<PrivateKeyEntry>(entry));
	            return entry;
            }
            catch (Exception e) 
            {
            }
        }
        
        return null;
    }
}
