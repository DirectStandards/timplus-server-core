package org.jivesoftware.openfire.trustcircle;

public class TrustCircleAlreadyExistsException extends TrustCircleException
{

	private static final long serialVersionUID = -1741165699516782072L;

	public TrustCircleAlreadyExistsException() 
    {
        super();
    }

    public TrustCircleAlreadyExistsException(String msg) 
    {
        super(msg);
    }

    public TrustCircleAlreadyExistsException(Throwable nestedThrowable) 
    {
        this.nestedThrowable = nestedThrowable;
    }

    public TrustCircleAlreadyExistsException(String msg, Throwable nestedThrowable) 
    {
        super(msg);
        this.nestedThrowable = nestedThrowable;
    }
}