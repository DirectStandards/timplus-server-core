package org.jivesoftware.openfire.certificate;

public class CertificateAlreadyExistsException extends CertificateException
{
	private static final long serialVersionUID = -3172693661096609006L;

	public CertificateAlreadyExistsException() 
    {
        super();
    }

    public CertificateAlreadyExistsException(String msg) 
    {
        super(msg);
    }

    public CertificateAlreadyExistsException(Throwable nestedThrowable) 
    {
        this.nestedThrowable = nestedThrowable;
    }

    public CertificateAlreadyExistsException(String msg, Throwable nestedThrowable) 
    {
        super(msg);
        this.nestedThrowable = nestedThrowable;
    }
}