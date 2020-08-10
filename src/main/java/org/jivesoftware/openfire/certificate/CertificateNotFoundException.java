package org.jivesoftware.openfire.certificate;


public class CertificateNotFoundException extends CertificateException
{

	private static final long serialVersionUID = 2921200431097540452L;

	public CertificateNotFoundException() 
    {
        super();
    }

    public CertificateNotFoundException(String msg) 
    {
        super(msg);
    }

    public CertificateNotFoundException(Throwable nestedThrowable) 
    {
        this.nestedThrowable = nestedThrowable;
    }

    public CertificateNotFoundException(String msg, Throwable nestedThrowable) 
    {
        super(msg);
        this.nestedThrowable = nestedThrowable;
    }
}