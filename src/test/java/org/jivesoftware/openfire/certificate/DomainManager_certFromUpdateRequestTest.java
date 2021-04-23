package org.jivesoftware.openfire.certificate;

import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import org.directtruststandards.timplus.common.crypto.CryptoUtils;
import org.jivesoftware.SpringDataBaseTest;
import org.jivesoftware.util.PrivateKeyType;
import org.junit.Test;

public class DomainManager_certFromUpdateRequestTest extends SpringDataBaseTest
{
	static
	{
		CryptoUtils.registerJCEProviders();
	}
	
	@Test
	public void testCertFromUpdateRequest_unproctedPKCS12_assertAdded() throws Exception
	{
		final InputStream inStream = this.getClass().getClassLoader().getResourceAsStream("certs/altNameOnly.p12");
		
		final Certificate cert = 
				CertificateManager.getInstance().certFromUpdloadRequest(PrivateKeyType.PKCS_12_UNPROTECTED, "", inStream, null);
		
		assertNotNull(cert);
		assertNotNull(cert.asX509Certificate());
	}
	
	@Test
	public void testCertFromUpdateRequest_proctedPKCS12_assertAdded() throws Exception
	{
		final InputStream inStream = this.getClass().getClassLoader().getResourceAsStream("certs/gm2552encrypted.p12");
		
		final Certificate cert = 
				CertificateManager.getInstance().certFromUpdloadRequest(PrivateKeyType.PKCS_12_PASSPHRASE, "1kingpuff", inStream, null);
		
		assertNotNull(cert);
		assertNotNull(cert.asX509Certificate());
	}
	
	@Test
	public void testCertFromUpdateRequest_unproctedPKCS8_assertAdded() throws Exception
	{
		final InputStream certStream = this.getClass().getClassLoader().getResourceAsStream("certs/altNameOnly.der");
		final InputStream keyStream = this.getClass().getClassLoader().getResourceAsStream("certs/altNameOnlyKey.der");
		
		final Certificate cert = 
				CertificateManager.getInstance().certFromUpdloadRequest(PrivateKeyType.PKCS8_UNPROTECT, "", certStream, keyStream);
		
		assertNotNull(cert);
		assertNotNull(cert.asX509Certificate());
	}
	
	@Test
	public void testCertFromUpdateRequest_proctedPKCS8_assertAdded() throws Exception
	{
		final InputStream certStream = this.getClass().getClassLoader().getResourceAsStream("certs/riunittest.com.der");
		final InputStream keyStream = this.getClass().getClassLoader().getResourceAsStream("certs/riunittest.comKey.der");
		
		final Certificate cert = 
				CertificateManager.getInstance().certFromUpdloadRequest(PrivateKeyType.PKCS8_PASSPHRASE, "1kingpuff", certStream, keyStream);
		
		assertNotNull(cert);
		assertNotNull(cert.asX509Certificate());
	}
	
	@Test
	public void testCertFromUpdateRequest_proctedPKCSWRapped_assertAdded() throws Exception
	{
		final InputStream certStream = this.getClass().getClassLoader().getResourceAsStream("certs/hsmtest.der");
		final InputStream keyStream = this.getClass().getClassLoader().getResourceAsStream("certs/hsmtestKey.der");
		
		final Certificate cert = 
				CertificateManager.getInstance().certFromUpdloadRequest(PrivateKeyType.PKCS8_WRAPPED, "", certStream, keyStream);
		
		assertNotNull(cert);
		assertNotNull(cert.asX509Certificate());
	}
}
