package org.jivesoftware.openfire.trustanchor;

public class TrustAnchorNotFoundException extends TrustAnchorException
{
	private static final long serialVersionUID = -3534634167316415871L;

	public TrustAnchorNotFoundException() 
    {
        super();
    }

    public TrustAnchorNotFoundException(String msg) 
    {
        super(msg);
    }

    public TrustAnchorNotFoundException(Throwable nestedThrowable) 
    {
        this.nestedThrowable = nestedThrowable;
    }

    public TrustAnchorNotFoundException(String msg, Throwable nestedThrowable) 
    {
        super(msg);
        this.nestedThrowable = nestedThrowable;
    }

}
