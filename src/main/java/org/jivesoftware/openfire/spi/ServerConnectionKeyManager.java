package org.jivesoftware.openfire.spi;


import java.lang.ref.Reference;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;

import org.apache.commons.lang3.StringUtils;
import org.directtruststandards.timplus.common.cert.CertStoreUtils;
import org.directtruststandards.timplus.common.cert.X509CertificateEx;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.certificate.CertificateManager;
import org.jivesoftware.util.ReferenceIDUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom TLS connection key manager that select certificates based on 
 * TIM+ specific requirements.
 * @author Greg Meyer
 *
 */
public class ServerConnectionKeyManager extends X509ExtendedKeyManager implements X509KeyManager
{
	private static final Logger Log = LoggerFactory.getLogger(ServerConnectionKeyManager.class);	
	
	private static final int DNSName_TYPE = 2; // name type constant for Subject Alternative name domain name	
	
    private final CertificateManager mgr;
	
    private final Map<String,Reference<X509CertificateEx>> entryCacheMap;

    public ServerConnectionKeyManager(CertificateManager mgr) 
    {
    	this.mgr = mgr;
    	
        entryCacheMap = Collections.synchronizedMap
                (new SizedMap<String,Reference<X509CertificateEx>>());
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
    
    /*
     * This alias returned will be the certificate thumb print
     */
    public String chooseEngineServerAlias(String keyType,
            Principal[] issuers, SSLEngine engine) 
    {
    	// Make sure we have a connected domain associated with the thread.
    	final String referenceId = ReferenceIDUtil.getSessionReferenceId(engine.getSession());
    	if (StringUtils.isEmpty(referenceId))
    		return null;
    	
    		try
    		{
    			// get the possible certificates for the reference id
    			Collection<org.jivesoftware.openfire.certificate.Certificate> certs =  mgr.getCertificatesByDomain(referenceId);

	    		if (certs == null || certs.isEmpty())
	    			return null;
	    		
		    	// find the certificates that matches the connection domain
		    	for (org.jivesoftware.openfire.certificate.Certificate checkCert :  certs)
		    	{

		            
		            final X509Certificate cert = checkCert.asX509Certificate();
		            
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
		                            return checkCert.getThumbprint();
		                        }
		                    }
		                }
		            }
		    	}
    		}
    		catch (Exception e)
    		{
    			Log.warn("Could not get a certificate for reference id " + referenceId, e);
    		}
    	
    	if (StringUtils.isEmpty(referenceId))
    		Log.warn("Can not lookup a certificate for an empty reference id");
    	else
    		Log.warn("Could not get a certificate for reference id " + referenceId + " and key type" + keyType);
    	
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


            try 
            {
            
		    	// find the certificates that matches the connection domain
		    	for (org.jivesoftware.openfire.certificate.Certificate checkCert : mgr.getCertificates())
		    	{
		            
		            final X509Certificate cert = (X509Certificate)checkCert.asX509Certificate();
		            
		            if (!cert.getPublicKey().getAlgorithm().equals(keyType))
		            	continue;            	
            	

		            results.add(checkCert.getThumbprint());
                }
            } 
            catch (Exception e) 
            {
                // ignore
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
		X509CertificateEx entry = getEntry(alias);
        return entry == null ? null : new X509Certificate[] {entry};
	}

	@Override
	public PrivateKey getPrivateKey(String alias)
	{
		X509CertificateEx entry = getEntry(alias);
        return entry == null ? null : entry.getPrivateKey();
	}

    private X509CertificateEx getEntry(String thumbprint) 
    {
        // if the alias is null, return immediately
        if (StringUtils.isEmpty(thumbprint)) 
        {
        	Log.warn("Cannot get a certificate entry for an empty thumbprint");
        	return null;
        }

        // try to get the entry from cache
        Reference<X509CertificateEx> ref = entryCacheMap.get(thumbprint);
        X509CertificateEx entry = (ref != null) ? ref.get() : null;
        if (entry != null) {
            return entry;
        }


        try 
        {
        	final org.jivesoftware.openfire.certificate.Certificate checkCert = 
        			mgr.getCertificateByThumbprint(thumbprint);

        	final X509Certificate cert = CertStoreUtils.certFromData(XMPPServer.getInstance().getKeyStoreProtectionManager(), checkCert.getCertData());
        	

            if (cert instanceof X509CertificateEx) 
            {
                return (X509CertificateEx)cert;
            }
        }
        catch (Exception e) 
        {
        	Log.warn("Error trying to retrive a certrificate for thumprint " + thumbprint, e);
        }
            
        Log.warn("Could not get a certificate entry for thumbprint " + thumbprint);
        
        return null;
    }
}
