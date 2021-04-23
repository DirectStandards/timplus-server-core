<%@ page contentType="text/html; charset=UTF-8" %>
<%--
  -
  - Copyright (C) 2004-2008 Jive Software. All rights reserved.
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
--%>

<%@ page import="org.jivesoftware.openfire.security.SecurityAuditManager,
                 org.jivesoftware.openfire.session.ClientSession,
                 org.jivesoftware.util.DomainResolver,
                 org.jivesoftware.openfire.user.User,
                 org.jivesoftware.openfire.domain.Domain"
    errorPage="error.jsp"
%>
<%@ page import="org.jivesoftware.openfire.domain.DomainManager" %>
<%@ page import="org.jivesoftware.openfire.user.UserManager" %>
<%@ page import="org.jivesoftware.util.ParamUtils" %>
<%@ page import="org.jivesoftware.util.StringUtils" %>
<%@ page import="org.jivesoftware.util.CookieUtils" %>
<%@ page import="org.xmpp.packet.JID" %>
<%@ page import="org.xmpp.packet.StreamError" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.util.Collection" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager" />
<% webManager.init(request, response, session, application, out ); %>

<%  // Get parameters //
    boolean cancel = request.getParameter("cancel") != null;
    boolean delete = request.getParameter("delete") != null;
    String domainName = ParamUtils.getParameter(request,"domain");
    Cookie csrfCookie = CookieUtils.getCookie(request, "csrf");
    String csrfParam = ParamUtils.getParameter(request, "csrf");

    if (delete) {
        if (csrfCookie == null || csrfParam == null || !csrfCookie.getValue().equals(csrfParam)) {
            delete = false;
        }
    }
    csrfParam = StringUtils.randomString(15);
    CookieUtils.setCookie(request, response, "csrf", csrfParam, -1);
    pageContext.setAttribute("csrf", csrfParam);

    // Handle a cancel
    if (cancel) {
        response.sendRedirect("domain-properties.jsp?domain=" + URLEncoder.encode(domainName, "UTF-8"));
        return;
    }

    // Load the domain object
    Domain domain = webManager.getDomainManager().getDomain(domainName);

    // Handle a domain delete:
    if (delete) {
        // Delete the domain
        webManager.getDomainManager().deleteDomain(domainName);

        if (!SecurityAuditManager.getSecurityAuditProvider().blockUserEvents()) {
            // Log the event
            webManager.logEvent("deleted domain "+ domainName, "");
        }
        
        // delete all of the users associated with the domain
        final Collection<User> users = UserManager.getInstance().getUsersByDomain(domainName);
        boolean deletedSelf = false;
        
        for (User user : users)
        {
        	UserManager.getInstance().deleteUser(user);
        	
	        // Close the user's connection
	        final StreamError error = new StreamError(StreamError.Condition.not_authorized);
	        for (ClientSession sess : webManager.getSessionManager().getSessions(user.getUsername()) )
	        {
	            sess.deliverRawText(error.toXML());
	            sess.close();
	        }
	        
	        if (user.getUsername().endsWith(webManager.getAuthToken().getUsername()))
	        	deletedSelf = true;
        }
        
        // Deleted your own user account, force login
        if (deletedSelf)
        {
            session.removeAttribute("jive.admin.authToken");
            response.sendRedirect("login.jsp");
        }
        else 
        {
            // Done, so redirect
            response.sendRedirect("domain-summary.jsp?deletesuccess=true");
        }
        
        return;
    }
%>

<html>
    <head>
        <title><fmt:message key="domain.delete.title"/></title>
        <meta name="subPageID" content="domain-delete"/>
        <meta name="extraParams" content="<%= "domain="+URLEncoder.encode(domain.getDomainName(), "UTF-8") %>"/>
        <meta name="helpPage" content="remove_a_domain_from_the_system.html"/>
    </head>
    <body>

<p>
<fmt:message key="domain.delete.info" />
<b><a href="domain-properties.jsp?domain=<%= URLEncoder.encode(domain.getDomainName(), "UTF-8") %>"><%= StringUtils.escapeHTMLTags(JID.unescapeNode(domain.getDomainName())) %></a></b>
<fmt:message key="domain.delete.info1" />
</p>

<form action="domain-delete.jsp">
    <input type="hidden" name="csrf" value="${csrf}">
<input type="hidden" name="domain" value="<%= StringUtils.escapeForXML(domainName) %>">
<input type="submit" name="delete" value="<fmt:message key="domain.delete.delete" />">
<input type="submit" name="cancel" value="<fmt:message key="global.cancel" />">
</form>


    </body>
</html>
