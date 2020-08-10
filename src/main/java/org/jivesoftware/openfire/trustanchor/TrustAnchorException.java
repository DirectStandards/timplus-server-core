package org.jivesoftware.openfire.trustanchor;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Throws if an operation on a trust anchor fails.
 * @author Greg Meyer
 * @since 1.0.0
 */
public class TrustAnchorException extends Exception 
{
	private static final long serialVersionUID = -787212368496349373L;

	protected Throwable nestedThrowable = null;

    public TrustAnchorException() 
    {
        super();
    }

    public TrustAnchorException(String msg) 
    {
        super(msg);
    }

    public TrustAnchorException(Throwable nestedThrowable) 
    {
        this.nestedThrowable = nestedThrowable;
    }

    public TrustAnchorException(String msg, Throwable nestedThrowable) 
    {
        super(msg);
        this.nestedThrowable = nestedThrowable;
    }

    @Override
    public void printStackTrace() 
    {
        super.printStackTrace();
        if (nestedThrowable != null) 
            nestedThrowable.printStackTrace();
    }

    @Override
    public void printStackTrace(PrintStream ps) 
    {
        super.printStackTrace(ps);
        if (nestedThrowable != null) 
            nestedThrowable.printStackTrace(ps);
    }

    @Override
    public void printStackTrace(PrintWriter pw) 
    {
        super.printStackTrace(pw);
        if (nestedThrowable != null)
            nestedThrowable.printStackTrace(pw);
    }
}
