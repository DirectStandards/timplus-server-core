package org.jivesoftware.openfire.trustcircle;

public class TrustCircleNotFoundException extends TrustCircleException
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 5726546728493345229L;

	public TrustCircleNotFoundException() 
    {
        super();
    }

    public TrustCircleNotFoundException(String msg) 
    {
        super(msg);
    }

    public TrustCircleNotFoundException(Throwable nestedThrowable) 
    {
        this.nestedThrowable = nestedThrowable;
    }

    public TrustCircleNotFoundException(String msg, Throwable nestedThrowable) 
    {
        super(msg);
        this.nestedThrowable = nestedThrowable;
    }
}