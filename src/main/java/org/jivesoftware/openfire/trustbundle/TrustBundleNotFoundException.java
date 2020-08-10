package org.jivesoftware.openfire.trustbundle;

public class TrustBundleNotFoundException extends TrustBundleException
{

	private static final long serialVersionUID = -6634011703899600700L;

	public TrustBundleNotFoundException() 
    {
        super();
    }

    public TrustBundleNotFoundException(String msg) 
    {
        super(msg);
    }

    public TrustBundleNotFoundException(Throwable nestedThrowable) 
    {
        this.nestedThrowable = nestedThrowable;
    }

    public TrustBundleNotFoundException(String msg, Throwable nestedThrowable) 
    {
        super(msg);
        this.nestedThrowable = nestedThrowable;
    }
}