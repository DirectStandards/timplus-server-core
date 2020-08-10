package org.jivesoftware.util.crypto;

public class CryptoException extends Exception
{
	private static final long serialVersionUID = -7989697702415348185L;

	/**
	 * {@inheritDoc}
	 */
    public CryptoException() 
    {
    }

	/**
	 * {@inheritDoc}
	 */
    public CryptoException(String msg) 
    {
        super(msg);
    }

	/**
	 * {@inheritDoc}
	 */
    public CryptoException(String msg, Throwable t) 
    {
        super(msg, t);
    }

	/**
	 * {@inheritDoc}
	 */
    public CryptoException(Throwable t) 
    {
        super(t);
    }
}