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
                 org.jivesoftware.openfire.trustcircle.TrustCircle,
                 org.jivesoftware.openfire.trustcircle.TrustCircleManager"
    errorPage="error.jsp"
%>
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
    String circleName = ParamUtils.getParameter(request,"circleName");
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
        response.sendRedirect("trust-circle-properties.jsp?circleName=" + URLEncoder.encode(circleName, "UTF-8"));
        return;
    }

    // Load the trust bundle object
    try
    {
    	TrustCircle circle = TrustCircleManager.getInstance().getTrustCircle(circleName, false, false);
    }
    catch (Exception e)
    {
    	
    }
    // Handle a trust circle delete:
    if (delete) {
        // Delete the trust bundle
        TrustCircleManager.getInstance().deleteTrustCircle(circleName);

        if (!SecurityAuditManager.getSecurityAuditProvider().blockUserEvents()) {
            // Log the event
            webManager.logEvent("deleted trust circle "+ circleName, "");
        }
        

        

        // Done, so redirect
        response.sendRedirect("trust-circle-summary.jsp?deletesuccess=true");

        
        return;
    }
%>

<html>
    <head>
        <title><fmt:message key="trustcircle.delete.title"/></title>
        <meta name="subPageID" content="trust-circle-delete"/>
        <meta name="extraParams" content="<%= "circleName="+URLEncoder.encode(circleName, "UTF-8") %>"/>
        <meta name="helpPage" content="remove_a_trust_circle_from_the_system.html"/>
    </head>
    <body>

<p>
<fmt:message key="trustcircle.delete.info" />
<b><a href="trust-circle-properties.jsp?circleName=<%= URLEncoder.encode(circleName, "UTF-8") %>"><%= StringUtils.escapeHTMLTags(circleName) %></a></b>
<fmt:message key="trustcircle.delete.info1" />
</p>

<form action="trust-circle-delete.jsp">
    <input type="hidden" name="csrf" value="${csrf}">
<input type="hidden" name="circleName" value="<%= StringUtils.escapeForXML(circleName) %>">
<input type="submit" name="delete" value="<fmt:message key="trustcircle.delete.delete" />">
<input type="submit" name="cancel" value="<fmt:message key="global.cancel" />">
</form>


    </body>
</html>
