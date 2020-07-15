package org.jivesoftware.openfire.filetransfer.proxy.credentials;

import java.util.Date;

public class ProxyServerCredential
{
	private String subject;
    private String secret;
    private byte[] secretHash;
    private Date creationDate;
    
    public ProxyServerCredential()
    {
    	
    }
    
    public ProxyServerCredential(String subject, String secret, byte[] secretHash, Date creationDate)
    {
    	this.subject = subject;
    	this.secret = secret;
    	this.secretHash = secretHash;
    	this.creationDate = creationDate;
    }

	public String getSubject()
	{
		return subject;
	}

	public void setSubject(String subject)
	{
		this.subject = subject;
	}

	public String getSecret()
	{
		return secret;
	}

	public void setSecret(String secret)
	{
		this.secret = secret;
	}

	public byte[] getSecretHash()
	{
		return secretHash;
	}

	public void setSecretHash(byte[] secretHash)
	{
		this.secretHash = secretHash;
	}

	public Date getCreationDate()
	{
		return creationDate;
	}

	public void setCreationDate(Date creationDate)
	{
		this.creationDate = creationDate;
	}
    
    
}
