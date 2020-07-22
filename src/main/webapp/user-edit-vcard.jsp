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

<%@ page import="org.jivesoftware.util.ParamUtils,
                 org.jivesoftware.util.StringUtils,
                 org.jivesoftware.util.CookieUtils,
                 org.jivesoftware.util.DomainResolver,
                 org.jivesoftware.openfire.user.*,
                 java.net.URLEncoder,
                 org.jivesoftware.smackx.vcardtemp.packet.VCard"
    errorPage="error.jsp"
%><%@ page import="org.xmpp.packet.JID"%>
<%@ page import="org.jivesoftware.openfire.security.SecurityAuditManager" %>
<%@ page import="org.jivesoftware.util.StringUtils" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="org.jivesoftware.openfire.admin.AdminManager" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager" />
<% webManager.init(request, response, session, application, out ); %>

<%  // Get parameters
    boolean save = ParamUtils.getBooleanParameter(request,"save");
    boolean success = ParamUtils.getBooleanParameter(request,"success");
    String username = ParamUtils.getParameter(request,"username");

    String firstname = ParamUtils.getStringParameter(request,"firstname", "");
    String lastname = ParamUtils.getStringParameter(request,"lastname", "");
    String middlename = ParamUtils.getStringParameter(request,"middlename", "");
    String prefix = ParamUtils.getStringParameter(request,"prefix", "");
    String suffix = ParamUtils.getStringParameter(request,"suffix", "");
    String org = ParamUtils.getStringParameter(request,"org", "");
    String orgunit = ParamUtils.getStringParameter(request,"orgunit", "");
    String title = ParamUtils.getStringParameter(request,"title", "");
    String role = ParamUtils.getStringParameter(request,"role", "");
    String workstreet1 = ParamUtils.getStringParameter(request,"workstreet1", "");
    String workstreet2 = ParamUtils.getStringParameter(request,"workstreet2", "");
    String workcity = ParamUtils.getStringParameter(request,"workcity", "");
    String workstate = ParamUtils.getStringParameter(request,"workstate", "");
    String workcountry = ParamUtils.getStringParameter(request,"workcountry", "");
    String workzipcode = ParamUtils.getStringParameter(request,"workzipcode", "");
    String workphone = ParamUtils.getStringParameter(request,"workphone", "");
    String workcell = ParamUtils.getStringParameter(request,"workcell", "");
    String workemail = ParamUtils.getStringParameter(request,"workemail", "" );
    
    
    
    
    boolean isAdmin = ParamUtils.getBooleanParameter(request,"isadmin");
    Map<String, String> errors = new HashMap<String, String>();
    Cookie csrfCookie = CookieUtils.getCookie(request, "csrf");
    String csrfParam = ParamUtils.getParameter(request, "csrf");
    
    /*
    if (save) {
        if (csrfCookie == null || csrfParam == null || !csrfCookie.getValue().equals(csrfParam)) {
            save = false;
            errors.put("csrf", "CSRF Failure");
        }
    }
    */
    csrfParam = StringUtils.randomString(15);
    CookieUtils.setCookie(request, response, "csrf", csrfParam, -1);
    pageContext.setAttribute("csrf", csrfParam);

    // Handle a cancel
    if (request.getParameter("cancel") != null) {
        response.sendRedirect("user-vcard.jsp?username=" + URLEncoder.encode(username, "UTF-8"));
        return;
    }

    // Load the vcard object
    VCard vCard = null;

    vCard = webManager.getVCardManager().getVCardAsVCard(username);

    // Handle a save
    if (save) 
    {

		if (vCard == null)
			vCard = new VCard();
		
        if (errors.size() == 0) 
        {
        	vCard.setFirstName(firstname);
        	vCard.setLastName(lastname);
        	vCard.setMiddleName(middlename);
        	vCard.setPrefix(prefix);
        	vCard.setSuffix(suffix);
        	vCard.setOrganization(org);
        	vCard.setOrganizationUnit(orgunit);
        	vCard.setField("TITLE", title);
        	vCard.setField("ROLE", role);
        	vCard.setAddressFieldWork("STREET", workstreet1);
        	vCard.setAddressFieldWork("EXTADR", workstreet2);
        	vCard.setAddressFieldWork("LOCALITY", workcity);
        	vCard.setAddressFieldWork("REGION", workstate);
        	vCard.setAddressFieldWork("CTRY", workcountry);
        	vCard.setAddressFieldWork("PCODE", workzipcode);
        	vCard.setPhoneWork("VOICE", workphone);
        	vCard.setPhoneWork("CELL", workcell);
        	vCard.setEmailWork(workemail);
        	
        	final String xml = vCard.toXML(VCard.NAMESPACE).toString();
        	
        	webManager.getVCardManager().setVCardAsXML(username, xml);
        	
            if (!SecurityAuditManager.getSecurityAuditProvider().blockUserEvents()) {
                // Log the event
                webManager.logEvent("edited vcard "+username, "");
            }

            // Changes good, so redirect
            response.sendRedirect("user-vcard.jsp?editsuccess=true&username=" + URLEncoder.encode(username, "UTF-8"));
            return;
        }
    }
%>

<html>
    <head>
        <title><fmt:message key="vcard.edit.form.title"/></title>
        <meta name="subPageID" content="user-vcard"/>
        <meta name="extraParams" content="<%= "username="+URLEncoder.encode(username, "UTF-8") %>"/>
    </head>
    <body>
<%  if (!errors.isEmpty()) { %>

    <div class="jive-error">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr>
            <td class="jive-icon"><img src="images/error-16x16.gif" width="16" height="16" border="0" alt=""/></td>
            <td class="jive-icon-label">

            <% if (errors.get("name") != null) { %>
                <fmt:message key="user.create.invalid_name" />
            <% } else if (errors.get("email") != null) { %>
                <fmt:message key="user.create.invalid_email" />
            <% } else if (errors.get("csrf") != null) { %>
                CSRF Failure!
            <% } %>
            </td>
        </tr>
    </tbody>
    </table>
    </div>
    <br>

<%  } else if (success) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt=""></td>
        <td class="jive-icon-label">
        <fmt:message key="vcard.edit.form.update" />
        </td></tr>
    </tbody>
    </table>
    </div><br>

<%  } %>

<p>
<fmt:message key="vcard.edit.form.info" />
</p>

<form action="user-edit-vcard.jsp">

<input type="hidden" name="csrf" value="">
<input type="hidden" name="username" value="<%= StringUtils.escapeForXML(username) %>">
<input type="hidden" name="save" value="true">

<fieldset>
    <legend><fmt:message key="vcard.edit.form.property" /></legend>
    <div>
    <table cellpadding="3" cellspacing="0" border="0" width="100%">
    <tbody>
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
                <input type="text" size="30" maxlength="150" name="firstname"
                 value="<%= StringUtils.isEmpty(vCard.getFirstName()) ? "" : vCard.getFirstName() %>">
            </td>
        </tr>
        <tr>
            <td class="c1">
                <fmt:message key="user.vcard.lastname" />:
            </td>
            <td>
                <input type="text" size="30" maxlength="150" name="lastname"
                 value="<%= (StringUtils.isEmpty(vCard.getLastName())) ? "" : vCard.getLastName() %>">
            </td>
        </tr>        
        <tr>
            <td class="c1">
                <fmt:message key="user.vcard.middlename" />:
            </td>
            <td>
                <input type="text" size="30" maxlength="150" name="middlename"
                 value="<%= (StringUtils.isEmpty(vCard.getMiddleName())) ? "" : vCard.getMiddleName() %>">
            </td>
        </tr>            
        <tr>
            <td class="c1">
                <fmt:message key="user.vcard.prefix" />:
            </td>
            <td>
                <input type="text" size="30" maxlength="150" name="prefix"
                 value="<%= (StringUtils.isEmpty(vCard.getPrefix())) ? "" : vCard.getPrefix() %>">
            </td>
        </tr>            
        <tr>
            <td class="c1">
                <fmt:message key="user.vcard.suffix" />:
            </td>
            <td>
                <input type="text" size="30" maxlength="150" name="suffix"
                 value="<%= (StringUtils.isEmpty(vCard.getSuffix())) ? "" : vCard.getSuffix() %>">
            </td>
        </tr>           
        <tr>
            <td class="c1">
                <fmt:message key="user.vcard.org" />:
            </td>
            <td>
                <input type="text" size="30" maxlength="150" name="org"
                 value="<%= (StringUtils.isEmpty(vCard.getOrganization())) ? "" : vCard.getOrganization() %>">
            </td>
        </tr>         
        <tr>
            <td class="c1">
                <fmt:message key="user.vcard.orgunit" />:
            </td>
            <td>
                <input type="text" size="30" maxlength="150" name="orgunit"
                 value="<%= (StringUtils.isEmpty(vCard.getOrganizationUnit())) ? "" : vCard.getOrganizationUnit() %>">
            </td>
        </tr>         
        <tr>
            <td class="c1">
                <fmt:message key="user.vcard.title" />:
            </td>
            <td>
                <input type="text" size="30" maxlength="150" name="title"
                 value="<%= (StringUtils.isEmpty(vCard.getField("TITLE"))) ? "" : vCard.getField("TITLE") %>">
            </td>
        </tr>       
        <tr>
            <td class="c1">
                <fmt:message key="user.vcard.role" />:
            </td>
            <td>
                <input type="text" size="30" maxlength="150" name="role"
                 value="<%= (StringUtils.isEmpty(vCard.getField("ROLE"))) ? "" : vCard.getField("ROLE") %>">
            </td>
        </tr>           
        <tr>
            <td class="c1">
                <fmt:message key="user.vcard.workstreet1" />:
            </td>
            <td>
                <input type="text" size="30" maxlength="150" name="workstreet1"
                 value="<%= (StringUtils.isEmpty(vCard.getAddressFieldWork("STREET"))) ? "" : vCard.getAddressFieldWork("STREET") %>">
            </td>
        </tr>          
        <tr>
            <td class="c1">
                <fmt:message key="user.vcard.workstreet2" />:
            </td>
            <td>
                <input type="text" size="30" maxlength="150" name="workstreet2"
                 value="<%= (StringUtils.isEmpty(vCard.getAddressFieldWork("EXTADR"))) ? "" : vCard.getAddressFieldWork("EXTADR") %>">
            </td>
        </tr>        
        <tr>
            <td class="c1">
                <fmt:message key="user.vcard.workcity" />:
            </td>
            <td>
                <input type="text" size="30" maxlength="150" name="workcity"
                 value="<%= (StringUtils.isEmpty(vCard.getAddressFieldWork("LOCALITY"))) ? "" : vCard.getAddressFieldWork("LOCALITY") %>">
            </td>
        </tr>      
        <tr>
            <td class="c1">
                <fmt:message key="user.vcard.workstate" />:
            </td>
            <td>
                <input type="text" size="30" maxlength="150" name="workstate"
                 value="<%= (StringUtils.isEmpty(vCard.getAddressFieldWork("REGION"))) ? "" : vCard.getAddressFieldWork("REGION") %>">
            </td>
        </tr>   
        <tr>
            <td class="c1">
                <fmt:message key="user.vcard.workcountry" />:
            </td>
            <td>
                <input type="text" size="30" maxlength="150" name="workcountry"
                 value="<%= (StringUtils.isEmpty(vCard.getAddressFieldWork("CTRY"))) ? "" : vCard.getAddressFieldWork("CTRY") %>">
            </td>
        </tr>          
        <tr>
            <td class="c1">
                <fmt:message key="user.vcard.workzipcode" />:
            </td>
            <td>
                <input type="text" size="30" maxlength="150" name="workzipcode"
                 value="<%= (StringUtils.isEmpty(vCard.getAddressFieldWork("PCODE"))) ? "" : vCard.getAddressFieldWork("PCODE") %>">
            </td>
        </tr>              
        <tr>
            <td class="c1">
                <fmt:message key="user.vcard.workphone" />:
            </td>
            <td>
                <input type="text" size="30" maxlength="150" name="workphone"
                 value="<%= (StringUtils.isEmpty(vCard.getPhoneWork("VOICE"))) ? "" : vCard.getPhoneWork("VOICE") %>">
            </td>
        </tr>      
        <tr>
            <td class="c1">
                <fmt:message key="user.vcard.workcell" />:
            </td>
            <td>
                <input type="text" size="30" maxlength="150" name="workcell"
                 value="<%= (StringUtils.isEmpty(vCard.getPhoneWork("CELL"))) ? "" : vCard.getPhoneWork("CELL") %>">
            </td>
        </tr>     
         <tr>
            <td class="c1">
                <fmt:message key="user.vcard.workemail" />:
            </td>
            <td>
                <input type="text" size="30" maxlength="150" name="workemail"
                 value="<%= (StringUtils.isEmpty(vCard.getEmailWork())) ? "" : vCard.getEmailWork() %>">
            </td>
        </tr>            
    </tbody>
    </table>
    </div>

</fieldset>

<br><br>

<input type="submit" value="<fmt:message key="global.save_properties" />">
<input type="submit" name="cancel" value="<fmt:message key="global.cancel" />">

</form>

<br/>

    </body>
</html>
