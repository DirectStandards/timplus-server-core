package org.jivesoftware.openfire.domain;

import java.io.PrintStream;
import java.io.PrintWriter;

public class DomainNotFoundException extends Exception 
{
	private static final long serialVersionUID = 4033902154089068992L;

	private Throwable nestedThrowable = null;

    public DomainNotFoundException() 
    {
        super();
    }

    public DomainNotFoundException(String msg) 
    {
        super(msg);
    }

    public DomainNotFoundException(Throwable nestedThrowable) 
    {
        this.nestedThrowable = nestedThrowable;
    }

    public DomainNotFoundException(String msg, Throwable nestedThrowable) 
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
