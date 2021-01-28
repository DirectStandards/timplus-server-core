package org.jivesoftware.openfire.certificate;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.io.IOUtils;
import org.apache.jcs.JCS;
import org.apache.jcs.access.exception.CacheException;
import org.directtruststandards.timplus.common.cert.CertStoreUtils;
import org.directtruststandards.timplus.common.cert.CertStoreUtils.CertContainer;
import org.directtruststandards.timplus.common.cert.CertUtils;
import org.directtruststandards.timplus.common.crypto.KeyStoreProtectionManager;
import org.directtruststandards.timplus.common.crypto.WrappableKeyProtectionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.PrivateKeyType;
import org.jivesoftware.util.SystemProperty;
import org.jivesoftware.util.SystemProperty.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CertificateManager
{
	public static final String XMPP_CERT_MANAGER_CACHE_MAX_ITEMS = "xmpp.certmanager.cache.maxitems";
	
	public static final String XMPP_CERT_MANAGER_CACHE_TTL = "xmpp.certmanager.cache.ttl";
	
	private static final String DOMAIN_CACHE_NAME = "CERTIFICATE_MANAGER_DOMAIN_CERT_CACHE";

	private static final String TP_CACHE_NAME = "CERTIFICATE_MANAGER_TP_CERT_CACHE";
	
	protected static final String DEFAULT_DNS_MAX_CAHCE_ITEMS = "1000";
	
	protected static final String DEFAULT_DNS_TTL = "3600"; // 1 hour
	
	private static final int DNSName_TYPE = 2; // name type constant for Subject Alternative name domain name	
	
	private static final Logger Log = LoggerFactory.getLogger(CertificateManager.class);	
	@SuppressWarnings("rawtypes")
	public static final SystemProperty<Class> CERTIFICATE_PROVIDER;
	
	private static CertificateProvider provider;
	
	protected JCS domainCertCache;
	
	protected JCS tpCertCache;
	
	static
	{
		CERTIFICATE_PROVIDER = Builder.ofType(Class.class)
				.setKey("provider.certificate.className").setBaseClass(CertificateProvider.class)
				.setDefaultValue(DefaultCertificateProvider.class).addListener(CertificateManager::initProvider).setDynamic(true)
				.build();
	
	}
	
    private static class CertificateContainer 
    {
        private static CertificateManager instance = new CertificateManager();
    }
	
    private static void initProvider(final Class<?> clazz) 
    {
        if (provider == null || !clazz.equals(provider.getClass())) 
        {
            try 
            {
                provider = (CertificateProvider) clazz.newInstance();
            }
            catch (final Exception e) 
            {
                Log.error("Error loading certificate provider: " + clazz.getName(), e);
                provider = new DefaultCertificateProvider();
            }
        }
    }
    
    public static CertificateManager getInstance() 
    {
        return CertificateContainer.instance;
    }
    
    private CertificateManager() 
    {
    	initProvider(CERTIFICATE_PROVIDER.getValue());
    	
		createCaches();
    }
    
	private void createCaches()
	{
		try
		{
			// create instances
			domainCertCache = CertCacheFactory.getInstance().getCertCache(DOMAIN_CACHE_NAME, getDefaultCertCachePolicy());	
			tpCertCache = CertCacheFactory.getInstance().getCertCache(TP_CACHE_NAME, getDefaultCertCachePolicy());	
		}
		///CLOVER:OFF
		catch (CacheException e)
		{
			Log.warn("CertificateManager - Could not create certificate caches", e);
		}
		///CLOVER:ON
	}    
    
	private synchronized JCS getDomainCache()
	{
		if (domainCertCache == null)
			createCaches();
		
		return domainCertCache;
	}
	
	private synchronized JCS getTPCache()
	{
		if (tpCertCache == null)
			createCaches();
		
		return tpCertCache;
	}
	
    public Collection<Certificate> getCertificates() throws CertificateException
    {
    	return provider.getCertificates();
    }
    
    @SuppressWarnings("unchecked")
	public Collection<Certificate> getCertificatesByDomain(String domain) throws CertificateException
    {
    	Collection<Certificate> retVal = null;
    	
    	final JCS cache = getDomainCache();
    	if (cache != null)
    	{
    		retVal = (Collection<Certificate>)cache.get(domain.toUpperCase());
    	}
    	
    	if (cache == null || (retVal == null || retVal.size() == 0))
    	{
    		// cache miss
    		retVal = provider.getCertificatesByDomain(domain);
    		
    		if (retVal != null && retVal.size() != 0)
    		{
    			try
    			{
    				cache.put(domain.toUpperCase(), retVal);
    			}
    			catch (Exception e)
    			{
    				Log.warn("Failed to insert certificates into domain cache.", e);
    			}
    		}
    	}
    	
    	return retVal;
    }
    
    @SuppressWarnings("unchecked")
	public Certificate getCertificateByThumbprint(String thumbprint) throws CertificateException
    {
    	Certificate retVal = null;
    	
    	final JCS cache = getTPCache();
    	if (cache != null)
    	{
    		Collection<Certificate> cacheCert = (Collection<Certificate>)cache.get(thumbprint.toUpperCase());
    		if (cacheCert != null && cacheCert.size() > 0)
    			retVal = cacheCert.iterator().next();
    	}
    	
    	if (cache == null || retVal == null)
    	{
    		// cache miss
    		retVal = provider.getCertificateByThumbprint(thumbprint);
    		
    		if (retVal != null)
    		{
    			try
    			{
    				cache.put(thumbprint.toUpperCase(), Collections.singleton(retVal));
    			}
    			catch (Exception e)
    			{
    				Log.warn("Failed to insert certificate into thumbprint cache.", e);
    			}
    		}
    	}
    	
    	return retVal;    	
    }
    
    public Certificate addCertificate(Certificate cert) throws CertificateException
    {
    	return provider.addCertificate(cert);
    }
    
    public void deleteCertificate(String thumbprint) throws CertificateException
    {
    	provider.deleteCertificate(thumbprint);
    }
    
    public Certificate certFromUpdloadRequest(PrivateKeyType privKeyType, String passphrase, InputStream certFileInstream, InputStream privKeyStream) throws CertificateException
    {
    	byte[] certOrP12Bytes = null;
    	try
    	{
    		certOrP12Bytes = IOUtils.toByteArray(certFileInstream);
    	}
    	catch (Exception e)
    	{
    		throw new CertificateException("Could not extract cert data from input stream", e);
    	}
    	
    	byte[] privateKeyBytes = null;
		// need to determine if there is a private key or not

		if (privKeyType == PrivateKeyType.PKCS_12_PASSPHRASE || privKeyType == PrivateKeyType.PKCS_12_UNPROTECTED)
		{
				certOrP12Bytes = CertUtils.pkcs12ToStrippedPkcs12(certOrP12Bytes, passphrase);
		}
		else if (privKeyType != PrivateKeyType.NONE)
		{
			// there is a private key file associated with this request
			try
			{
				privateKeyBytes = IOUtils.toByteArray(privKeyStream);
			} 
			catch (Exception e)
			{
				throw new CertificateException("Could not extract private key data from input stream", e);
			}
			
			// get the private key... it may be different formats, so be on the watch
			if (privKeyType == PrivateKeyType.PKCS8_PASSPHRASE)
			{
				// this is a pass phrase protected private key... normalized it to an unprotected
				// key
				try
				{
					final EncryptedPrivateKeyInfo encryptPKInfo = new EncryptedPrivateKeyInfo(privateKeyBytes);
					final Cipher cipher = Cipher.getInstance(encryptPKInfo.getAlgName());
					final PBEKeySpec pbeKeySpec = new PBEKeySpec(passphrase.toCharArray());
					final SecretKeyFactory secFac = SecretKeyFactory.getInstance(encryptPKInfo.getAlgName());
					final Key pbeKey = secFac.generateSecret(pbeKeySpec);
					final AlgorithmParameters algParams = encryptPKInfo.getAlgParameters();
					cipher.init(Cipher.DECRYPT_MODE, pbeKey, algParams);
					final KeySpec pkcs8KeySpec = encryptPKInfo.getKeySpec(cipher);
					final KeyFactory kf = KeyFactory.getInstance("RSA");
					privateKeyBytes = kf.generatePrivate(pkcs8KeySpec).getEncoded();
				}
				catch (Exception e)
				{
					throw new CertificateException("Could not normalize the private key.", e);
				}
			}
		}
	                        	
        final Certificate cert = new Certificate();
       
        // convert the cert and key to the proper storage format
        cert.setCertData(toCertDataFormat(certOrP12Bytes, privateKeyBytes, privKeyType));


		final String domain = getDomainFromCert(cert.asX509Certificate());
        
		cert.setDomain(domain);
        cert.setStatus(CertificateStatus.GOOD);

        
        return cert;
	
    }
    
	private byte[] toCertDataFormat(byte[] certOrP12Bytes, byte[] privateKeyBytes, PrivateKeyType privKeyType) throws CertificateException
	{
		try
		{
			// if there is no private key, then just return the encoded certificate
			if (privKeyType == PrivateKeyType.NONE)
				return certOrP12Bytes;
			
			final CertContainer cont =  CertStoreUtils.toCertContainer(certOrP12Bytes);
			
			WrappableKeyProtectionManager keyManager = null;
			if (XMPPServer.getInstance().getKeyStoreProtectionManager() != null && XMPPServer.getInstance().getKeyStoreProtectionManager() instanceof WrappableKeyProtectionManager)
				keyManager = WrappableKeyProtectionManager.class.cast(XMPPServer.getInstance().getKeyStoreProtectionManager());
			
			// if this is a PKCS12 format, then either return the bytes as is, or if there is keystore manager, wrap the private keys
			if (privKeyType == PrivateKeyType.PKCS_12_PASSPHRASE | privKeyType == PrivateKeyType.PKCS_12_UNPROTECTED)
			{
				// at this point, any PKCS12 byte stream should be normalized meaning that the private key is unencrypted
				
				// if there is no keystore manager, we can't wrap the keys, so we'll just send them over the wire
				// as PKCS12 file
				if (keyManager == null)
				{
					return certOrP12Bytes;
				}
				else
				{

					// now wrap the private key
					final byte[] wrappedKey = keyManager.wrapWithSecretKey((SecretKey)((KeyStoreProtectionManager)keyManager).getPrivateKeyProtectionKey(), 
							cont.getKey());
					
					// return the wrapped key format
					return CertStoreUtils.certAndWrappedKeyToRawByteFormat(wrappedKey, cont.getCert());
				}
			}
			
			// when there is private key file, then either turn into a PKCS12 file (if there is no key manager), or wrap the key.
			else
			{
				
				// first thing, is the key is already wrapped, then do nothing to the key and return a bytes stream using the 
				// cert and wrapped key format
				if (privKeyType == PrivateKeyType.PKCS8_WRAPPED)
				{
					return CertStoreUtils.certAndWrappedKeyToRawByteFormat(privateKeyBytes, cont.getCert());
				}
				
				// get a private key object, the private key is normalized at this point into an unencrypted format
				final KeyFactory kf = KeyFactory.getInstance("RSA", "BC");
				final PKCS8EncodedKeySpec keysp = new PKCS8EncodedKeySpec (privateKeyBytes);
				final Key privKey = kf.generatePrivate (keysp);
				
	
				if (keyManager == null)
				{
					
					// if there is no keystore manager, we can't wrap the keys, so we'll just send them over the wire
					// as PKCS12 file.  need to turn this into a PKCS12 format
					final KeyStore localKeyStore = KeyStore.getInstance("PKCS12", "BC");
					localKeyStore.load(null, null);
					
					localKeyStore.setKeyEntry("privCert", privKey, "".toCharArray(),  new java.security.cert.Certificate[] {cont.getCert()});
						
					try (final ByteArrayOutputStream outStr = new ByteArrayOutputStream())
					{
						
						localKeyStore.store(outStr, "".toCharArray());	
						return outStr.toByteArray();
					}
				}		
				else
				{
					// wrap the key and turn the stream in the wrapped key format
					final byte[] wrappedKey = keyManager.wrapWithSecretKey((SecretKey)((KeyStoreProtectionManager)keyManager).getPrivateKeyProtectionKey(), 
							privKey);
					return CertStoreUtils.certAndWrappedKeyToRawByteFormat(wrappedKey, cont.getCert());
				}
			}
		}
		catch (Exception e)
		{
			throw new CertificateException("Failed to conver certificate and key to cert data format: " + e.getMessage(), e);
		}
	}   
	
	protected String getDomainFromCert(X509Certificate cert) throws CertificateException
	{
		final Collection<String> domains = new ArrayList<>();
		
	   	Collection<List<?>> altNames = null;
    	try
    	{
    		altNames = cert.getSubjectAlternativeNames();
    	}
    	catch (CertificateParsingException ex)
    	{
    		throw new CertificateException("Could not get certificate subject alt names.");
    	}	
		
    	if (altNames != null)
		{
    		for (List<?> entries : altNames)
    		{
    			if (entries.size() >= 2) // should always be the case according the altNames spec, but checking to be defensive
    			{
    				
    				final Integer nameType = (Integer)entries.get(0);
    				if (nameType == DNSName_TYPE)
    				{
    					final String name = (String)entries.get(1);
    					
    					domains.add(name.toLowerCase(Locale.getDefault()));
    				}
    				
    			}
    		}
		}
    	
    	return String.join(",", domains);
	}
	
	private CertStoreCachePolicy getDefaultCertCachePolicy()
	{
		return new DefaultCertManagerCachePolicy();
	}
	
	public static class DefaultCertManagerCachePolicy implements CertStoreCachePolicy
	{
		protected final int maxItems;
		protected final int subjectTTL;
		
		public DefaultCertManagerCachePolicy()
		{
			final String maxItemsString = JiveGlobals.getProperty(XMPP_CERT_MANAGER_CACHE_MAX_ITEMS, DEFAULT_DNS_MAX_CAHCE_ITEMS);
			maxItems =  Integer.parseInt(maxItemsString);
			
			final String maxTTLString = JiveGlobals.getProperty(XMPP_CERT_MANAGER_CACHE_TTL, DEFAULT_DNS_TTL);
			subjectTTL =  Integer.parseInt(maxTTLString);
		}
		
		public int getMaxItems() 
		{
			return maxItems;
		}

		public int getSubjectTTL() 
		{
			return subjectTTL;
		}
		
	}	
}
