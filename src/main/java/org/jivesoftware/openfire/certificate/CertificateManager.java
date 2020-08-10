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
import java.util.List;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.io.IOUtils;
import org.jivesoftware.util.PrivateKeyType;
import org.jivesoftware.util.SystemProperty;
import org.jivesoftware.util.SystemProperty.Builder;
import org.jivesoftware.util.cert.CertUtils;
import org.jivesoftware.util.cert.CertUtils.CertContainer;
import org.jivesoftware.util.crypto.KeyStoreProtectionManager;
import org.jivesoftware.util.crypto.WrappableKeyProtectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CertificateManager
{
	private static final int DNSName_TYPE = 2; // name type constant for Subject Alternative name domain name	
	
	private static final Logger Log = LoggerFactory.getLogger(CertificateManager.class);	
	@SuppressWarnings("rawtypes")
	public static final SystemProperty<Class> CERTIFICATE_PROVIDER;
	private static CertificateProvider provider;
	
	private WrappableKeyProtectionManager keyManager;
	
	static
	{
		CERTIFICATE_PROVIDER = Builder.ofType(Class.class)
				.setKey("provider.certificate.className").setBaseClass(CertificateManager.class)
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
    }
    
    public Collection<Certificate> getCertificates() throws CertificateException
    {
    	return provider.getCertificates();
    }
    
    public Collection<Certificate> getCertificatesByDomain(String domain) throws CertificateException
    {
    	return provider.getCertificatesByDomain(domain);
    }
    
    public Certificate getCertificateByThumbprint(String thumbprint) throws CertificateException
    {
    	return provider.getCertificateByThumbprint(thumbprint);
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
			
			final CertContainer cont =  CertUtils.toCertContainer(certOrP12Bytes);
			
			// if this is a PKCS12 format, then either return the bytes as is, or if there is keystore manager, wrap the private keys
			if (privKeyType == PrivateKeyType.PKCS_12_PASSPHRASE | privKeyType == PrivateKeyType.PKCS_12_UNPROTECTED)
			{
				// at this point, any PKCS12 byte stream should be normalized meaning that the private key is unencrypted
				
				// if there is no keystore manager, we can't wrap the keys, so we'll just send them over the wire
				// as PKCS12 file
				if (this.keyManager == null)
				{
					return certOrP12Bytes;
				}
				else
				{

					// now wrap the private key
					final byte[] wrappedKey = this.keyManager.wrapWithSecretKey((SecretKey)((KeyStoreProtectionManager)keyManager).getPrivateKeyProtectionKey(), 
							cont.getKey());
					
					// return the wrapped key format
					return CertUtils.certAndWrappedKeyToRawByteFormat(wrappedKey, cont.getCert());
				}
			}
			
			// when there is private key file, then either turn into a PKCS12 file (if there is no key manager), or wrap the key.
			else
			{
				
				// first thing, is the key is already wrapped, then do nothing to the key and return a bytes stream using the 
				// cert and wrapped key format
				if (privKeyType == PrivateKeyType.PKCS8_WRAPPED)
				{
					return CertUtils.certAndWrappedKeyToRawByteFormat(privateKeyBytes, cont.getCert());
				}
				
				// get a private key object, the private key is normalized at this point into an unencrypted format
				final KeyFactory kf = KeyFactory.getInstance("RSA", "BC");
				final PKCS8EncodedKeySpec keysp = new PKCS8EncodedKeySpec (privateKeyBytes);
				final Key privKey = kf.generatePrivate (keysp);
				
	
				if (this.keyManager == null)
				{
					
					// if there is no keystore manager, we can't wrap the keys, so we'll just send them over the wire
					// as PKCS12 file.  need to turn this into a PKCS12 format
					final KeyStore localKeyStore = KeyStore.getInstance("PKCS12", "BC");
					localKeyStore.load(null, null);
					
					localKeyStore.setKeyEntry("privCert", privKey, "".toCharArray(),  new java.security.cert.Certificate[] {cont.getCert()});
					final ByteArrayOutputStream outStr = new ByteArrayOutputStream();
					localKeyStore.store(outStr, "".toCharArray());		
					
					try
					{
						return outStr.toByteArray();
					}
					finally
					{
						IOUtils.closeQuietly(outStr);
					}
				}		
				else
				{
					// wrap the key and turn the stream in the wrapped key format
					final byte[] wrappedKey = this.keyManager.wrapWithSecretKey((SecretKey)((KeyStoreProtectionManager)keyManager).getPrivateKeyProtectionKey(), 
							privKey);
					return CertUtils.certAndWrappedKeyToRawByteFormat(wrappedKey, cont.getCert());
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
}
