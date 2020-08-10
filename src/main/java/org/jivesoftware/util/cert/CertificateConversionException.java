package org.jivesoftware.util.cert;

public class CertificateConversionException extends RuntimeException 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -160957431943861209L;

	/**
	 * Empty constructor
	 */
    public CertificateConversionException() 
    {
    }

    /**
     * {@inheritDoc}
     */
    public CertificateConversionException(String msg) 
    {
        super(msg);
    }

    /**
     * {@inheritDoc}
     */
    public CertificateConversionException(String msg, Throwable t) 
    {
        super(msg, t);
    }

    /**
     * {@inheritDoc}
     */
    public CertificateConversionException(Throwable t) 
    {
        super(t);
    }
}
