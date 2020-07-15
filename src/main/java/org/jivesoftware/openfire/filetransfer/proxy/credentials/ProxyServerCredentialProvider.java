package org.jivesoftware.openfire.filetransfer.proxy.credentials;

import java.util.Date;

public interface ProxyServerCredentialProvider
{
	public ProxyServerCredential createCredential() throws ProxyCredentialException;
	
	public ProxyServerCredential getCredential(String subject) throws ProxyCredentialNotFoundException;
	
	public void deleteCredential(String subject);
	
	public void deleteExpiredCredentials(Date expirationTime);
	
	public byte[] generateSecretHash(String secret) throws ProxyCredentialException;
}
