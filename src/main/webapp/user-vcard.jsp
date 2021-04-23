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

<%@ page import="org.jivesoftware.openfire.PresenceManager,
                 org.jivesoftware.openfire.admin.AdminManager,
                 org.jivesoftware.openfire.group.Group,
                 org.jivesoftware.openfire.user.User,
                 org.jivesoftware.openfire.user.UserManager,
                 org.jivesoftware.openfire.user.UserNotFoundException,
                 org.jivesoftware.smackx.vcardtemp.packet.VCard"
    errorPage="error.jsp"
%>
<%@ page import="org.jivesoftware.util.JiveGlobals"%>
<%@ page import="org.jivesoftware.util.LocaleUtils"%>
<%@ page import="org.jivesoftware.util.ParamUtils"%>
<%@ page import="org.jivesoftware.util.StringUtils"%>
<%@ page import="org.xmpp.packet.JID"%><%@ page import="org.xmpp.packet.Presence"%>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.jivesoftware.util.StringUtils" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager" />

<%  // Get parameters //
    boolean cancel = request.getParameter("cancel") != null;
    boolean delete = request.getParameter("delete") != null;
    boolean password = request.getParameter("password") != null;
    String username = ParamUtils.getParameter(request,"username");

    // Handle a cancel
    if (cancel) {
        response.sendRedirect("user-summary.jsp");
        return;
    }

    // Handle a delete
    if (delete) {
        response.sendRedirect("user-delete.jsp?username=" + URLEncoder.encode(username, "UTF-8"));
        return;
    }

    // Handle password change
    if (password) {
        response.sendRedirect("user-password.jsp?username=" + URLEncoder.encode(username, "UTF-8"));
        return;
    }

    // Load the vcard object
    VCard vCard = null;

    vCard = webManager.getVCardManager().getVCardAsVCard(username);

%>

<html>
    <head>
        <title><fmt:message key="user.vcard.title"/></title>
        <meta name="subPageID" content="user-vcard"/>
        <meta name="extraParams" content="<%= "username="+URLEncoder.encode(username, "UTF-8") %>"/>
        <meta name="helpPage" content="user_vcard.html"/>
    </head>
    <body>

<p>
<fmt:message key="user.properties.info" />
</p>

<%  if (request.getParameter("success") != null) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt=""></td>
        <td class="jive-icon-label">
        <fmt:message key="user.properties.created" />
        </td></tr>
    </tbody>
    </table>
    </div><br>

<%  }  else if (request.getParameter("editsuccess") != null) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt=""></td>
        <td class="jive-icon-label">
        <fmt:message key="user.vcard.update" />
        </td></tr>
    </tbody>
    </table>
    </div><br>

<% } else if (vCard == null) { %>
    <div class="warning">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr>
        <td class="jive-icon-label">
            <fmt:message key="error.specific_user_not_found_or_no_vcard">
                <fmt:param value="<%= StringUtils.escapeHTMLTags(username)%>" />
            </fmt:message>
        </td></tr>
    </tbody>
    </table>
    </div><br>
<%  } %>

<div class="jive-table">
<table cellpadding="0" cellspacing="0" border="0" width="100%">
<thead>
    <tr>
        <th colspan="2">
            <fmt:message key="user.vcard.title" />
        </th>
    </tr>
</thead>
<tbody>
    <% if (vCard == null) { %>
    <tr>
        <td colspan="2" align="center">
            <fmt:message key="error.requested_vcard_not_found" />
        </td>
    </tr>
    <% } else { %>
    <tr>
        <td class="c1">
            <fmt:message key="user.vcard.username" />:
        </td>
        <td>
            <%= StringUtils.escapeHTMLTags(JID.unescapeNode(username)) %>
        </td>
    </tr>    
    <tr>
        <td class="c1">
            <fmt:message key="user.vcard.firstname" />:
        </td>
        <td>
            <%= (org.apache.commons.lang3.StringUtils.isEmpty(vCard.getFirstName())) ? "" : vCard.getFirstName() %>
        </td>
    </tr>
    <tr>
        <td class="c1">
            <fmt:message key="user.vcard.lastname" />:
        </td>
        <td>
            <%= (org.apache.commons.lang3.StringUtils.isEmpty(vCard.getLastName())) ? "" : vCard.getLastName() %>
        </td>
    </tr>  
    <tr>
        <td class="c1">
            <fmt:message key="user.vcard.middlename" />:
        </td>
        <td>
            <%= (org.apache.commons.lang3.StringUtils.isEmpty(vCard.getMiddleName())) ? "" : vCard.getMiddleName() %>
        </td>
    </tr> 
    <tr>
        <td class="c1">
            <fmt:message key="user.vcard.prefix" />:
        </td>
        <td>
            <%= (org.apache.commons.lang3.StringUtils.isEmpty(vCard.getPrefix())) ? "" : vCard.getPrefix() %>
        </td>
    </tr>     
    <tr>
        <td class="c1">
            <fmt:message key="user.vcard.suffix" />:
        </td>
        <td>
            <%= (org.apache.commons.lang3.StringUtils.isEmpty(vCard.getSuffix())) ? "" : vCard.getSuffix() %>
        </td>
    </tr>
    <tr>
        <td class="c1">
            <fmt:message key="user.vcard.org" />:
        </td>
        <td>
            <%= (org.apache.commons.lang3.StringUtils.isEmpty(vCard.getOrganization())) ? "" : vCard.getOrganization() %>
        </td>
    </tr>   
    <tr>
        <td class="c1">
            <fmt:message key="user.vcard.orgunit" />:
        </td>
        <td>
            <%= (org.apache.commons.lang3.StringUtils.isEmpty(vCard.getOrganizationUnit())) ? "" : vCard.getOrganizationUnit() %>
        </td>
    </tr> 
    <tr>
        <td class="c1">
            <fmt:message key="user.vcard.title" />:
        </td>
        <td>
            <%= (org.apache.commons.lang3.StringUtils.isEmpty(vCard.getField("TITLE"))) ? "" : vCard.getField("TITLE") %>
        </td>
    </tr>  
    <tr>
        <td class="c1">
            <fmt:message key="user.vcard.role" />:
        </td>
        <td>
            <%= (org.apache.commons.lang3.StringUtils.isEmpty(vCard.getField("ROLE"))) ? "" : vCard.getField("ROLE") %>
        </td>
    </tr>             
    <tr>
        <td class="c1">
            <fmt:message key="user.vcard.workstreet1" />:
        </td>
        <td>
            <%= (org.apache.commons.lang3.StringUtils.isEmpty(vCard.getAddressFieldWork("STREET"))) ? "" : vCard.getAddressFieldWork("STREET") %>
        </td>
    </tr>     
    <tr>
        <td class="c1">
            <fmt:message key="user.vcard.workstreet2" />:
        </td>
        <td>
            <%= (org.apache.commons.lang3.StringUtils.isEmpty(vCard.getAddressFieldWork("EXTADR"))) ? "" : vCard.getAddressFieldWork("EXTADR") %>
        </td>
    </tr>  
    <tr>
        <td class="c1">
            <fmt:message key="user.vcard.workcity" />:
        </td>
        <td>
            <%= (org.apache.commons.lang3.StringUtils.isEmpty(vCard.getAddressFieldWork("LOCALITY"))) ? "" : vCard.getAddressFieldWork("LOCALITY") %>
        </td>
    </tr> 
    <tr>
        <td class="c1">
            <fmt:message key="user.vcard.workstate" />:
        </td>
        <td>
            <%= (org.apache.commons.lang3.StringUtils.isEmpty(vCard.getAddressFieldWork("REGION"))) ? "" : vCard.getAddressFieldWork("REGION") %>
        </td>
    </tr> 
    <tr>
        <td class="c1">
            <fmt:message key="user.vcard.workcountry" />:
        </td>
        <td>
            <%= (org.apache.commons.lang3.StringUtils.isEmpty(vCard.getAddressFieldWork("CTRY"))) ? "" : vCard.getAddressFieldWork("CTRY") %>
        </td>
    </tr> 
    <tr>
        <td class="c1">
            <fmt:message key="user.vcard.workzipcode" />:
        </td>
        <td>
            <%= (org.apache.commons.lang3.StringUtils.isEmpty(vCard.getAddressFieldWork("PCODE"))) ? "" : vCard.getAddressFieldWork("PCODE") %>
        </td>
    </tr>   
    <tr>
        <td class="c1">
            <fmt:message key="user.vcard.workphone" />:
        </td>
        <td>
            <%= (org.apache.commons.lang3.StringUtils.isEmpty(vCard.getPhoneWork("VOICE"))) ? "" : vCard.getPhoneWork("VOICE") %>
        </td>
    </tr>    
    <tr>
        <td class="c1">
            <fmt:message key="user.vcard.workcell" />:
        </td>
        <td>
            <%= (org.apache.commons.lang3.StringUtils.isEmpty(vCard.getPhoneWork("CELL"))) ? "" : vCard.getPhoneWork("CELL") %>
        </td>
    </tr> 
    <tr>
        <td class="c1">
            <fmt:message key="user.vcard.workemail" />:
        </td>
        <td>
            <%= (org.apache.commons.lang3.StringUtils.isEmpty(vCard.getEmailWork())) ? "" : vCard.getEmailWork() %>
        </td>
    </tr>                                                
     <% } %>
</tbody>
</table>
</div>

<br><br>

<form action="user-edit-vcard.jsp">
<input type="hidden" name="username" value="<%= StringUtils.escapeForXML(username) %>">
<input type="submit" value="<fmt:message key="user.vcard.editvcard" />">
</form>

</body>
</html>
