package org.jivesoftware.openfire.keystore.jce;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;

import org.directtruststandards.timplus.common.cert.CertStoreUtils;
import org.directtruststandards.timplus.common.cert.Thumbprint;
import org.directtruststandards.timplus.common.cert.X509CertificateEx;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.certificate.Certificate;
import org.jivesoftware.openfire.certificate.CertificateManager;

public class CertManagerKeyStore extends KeyStoreSpi
{
	
	@Override
    public Key engineGetKey(String alias, char[] password)
            throws NoSuchAlgorithmException, UnrecoverableKeyException
    {
		Certificate cert = null;
		try
		{
			cert = CertificateManager.getInstance().getCertificateByThumbprint(alias);
		}
		catch (Exception e)
		{
			return null;
		}
		
		if (cert == null)
			return null;
		
		try
		{
			final X509CertificateEx x509Cert = (X509CertificateEx)CertStoreUtils.certFromData(XMPPServer.getInstance().getKeyStoreProtectionManager(), cert.getCertData());
		
			return x509Cert.getPrivateKey();
		}
		catch (Exception e) 
		{
			throw new UnrecoverableKeyException("Failed to get private key.");
		}
    }

	@Override
	public java.security.cert.Certificate[] engineGetCertificateChain(String alias)
	{
		Certificate cert = null;
		try
		{
			cert = CertificateManager.getInstance().getCertificateByThumbprint(alias);
		}
		catch (Exception e)
		{
			return null;
		}
		
		if (cert == null)
			return null;
		
		try
		{
			return new java.security.cert.Certificate[] {CertStoreUtils.certFromData(XMPPServer.getInstance().getKeyStoreProtectionManager(), cert.getCertData())};
		}
		catch (Exception e) 
		{
			throw new IllegalStateException("Failed to get certifcate.", e);
		}
	}

	@Override
	public java.security.cert.Certificate engineGetCertificate(String alias)
	{
		java.security.cert.Certificate[] certs = engineGetCertificateChain(alias);
		
		if (certs == null)
			return null;
		
		return certs[0];
	}

	@Override
	public Date engineGetCreationDate(String alias)
	{
		return Calendar.getInstance().getTime();
	}

	@Override
	public void engineSetKeyEntry(String alias, Key key, char[] password, java.security.cert.Certificate[] chain)
			throws KeyStoreException
	{
		// don't add certs this way
		
	}

	@Override
	public void engineSetKeyEntry(String alias, byte[] key, java.security.cert.Certificate[] chain)
			throws KeyStoreException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void engineSetCertificateEntry(String alias, java.security.cert.Certificate cert) throws KeyStoreException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void engineDeleteEntry(String alias) throws KeyStoreException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public Enumeration<String> engineAliases()
	{
		final Collection<String> aliases = new ArrayList<>();
		
		try
		{
			Collection<Certificate> certs = CertificateManager.getInstance().getCertificates();
			
			
			for (Certificate cert : certs)
				aliases.add(cert.getThumbprint());
			
			return Collections.enumeration(aliases);
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Failed to get certificate aliases", e);
		}
	}

	@Override
	public boolean engineContainsAlias(String alias)
	{
		try
		{
			return CertificateManager.getInstance().getCertificateByThumbprint(alias) != null;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	@Override
	public int engineSize()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean engineIsKeyEntry(String alias)
	{
		try
		{
			return engineGetKey(alias, "".toCharArray()) != null;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	@Override
	public boolean engineIsCertificateEntry(String alias)
	{
		try
		{
			return engineContainsAlias(alias);
		}
		catch (Exception e)
		{
			return false;
		}
	}

	@Override
	public String engineGetCertificateAlias(java.security.cert.Certificate cert)
	{
		if (cert instanceof X509Certificate)
		{
			return Thumbprint.toThumbprint((X509Certificate)cert).toString();
		}
		return null;
	}

	@Override
	public void engineStore(OutputStream stream, char[] password)
			throws IOException, NoSuchAlgorithmException, CertificateException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void engineLoad(InputStream stream, char[] password)
			throws IOException, NoSuchAlgorithmException, CertificateException
	{
		// TODO Auto-generated method stub
		
	}
}
