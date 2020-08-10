package org.jivesoftware.openfire.trustbundle;

public class TrustBundleAlreadyExistsException extends TrustBundleException
{
	private static final long serialVersionUID = -3263478800634389941L;

	public TrustBundleAlreadyExistsException() 
    {
        super();
    }

    public TrustBundleAlreadyExistsException(String msg) 
    {
        super(msg);
    }

    public TrustBundleAlreadyExistsException(Throwable nestedThrowable) 
    {
        this.nestedThrowable = nestedThrowable;
    }

    public TrustBundleAlreadyExistsException(String msg, Throwable nestedThrowable) 
    {
        super(msg);
        this.nestedThrowable = nestedThrowable;
    }
}