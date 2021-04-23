<%@ page contentType="text/html; charset=UTF-8" %>
<%--
--%>

<%@ page import="org.jivesoftware.util.*,
                 org.jivesoftware.openfire.domain.*,
                 org.jivesoftware.util.DomainResolver,
                 java.util.HashMap,
                 java.util.Map,
                 java.net.URLEncoder"
%><%@ page import="org.xmpp.packet.JID"%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%-- Define Administration Bean --%>
<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager"/>
<%   webManager.init(request, response, session, application, out ); %>
<%  
    // Get parameters
    boolean search = ParamUtils.getBooleanParameter(request,"search");
    String domainName = ParamUtils.getParameter(request,"domain");
    domainName = JID.escapeNode(domainName);

    // Handle a cancel
    if (request.getParameter("cancel") != null) {
        response.sendRedirect("domain-summary.jsp");
        return;
    }

    // Handle a search execute:
    Map<String,String> errors = new HashMap<String,String>();
    if (search) {
        Domain domain = null;
        try {
            domain = webManager.getDomainManager().getDomain(domainName);
        }
        catch (Exception e2) {
            errors.put("domain","domain");
        }
        if (domain != null) {
            // found the domain, so redirect to the dmain properties page:
            response.sendRedirect("domain-properties.jsp?domain=" +
                    URLEncoder.encode(domain.getDomainName(), "UTF-8"));
            return;
        }
    }
%>

<html>
    <head>
        <title><fmt:message key="domain.search.title"/></title>
        <meta name="pageID" content="domain-search"/>
        <meta name="helpPage" content="search_for_a_domain.html"/>
    </head>
    <body>

<%    if (errors.size() > 0) { %>
<p class="jive-error-text"><fmt:message key="domain.search.not_found" /></p>
<%    } %>
<form name="f" action="domain-search.jsp">
  <input type="hidden" name="search" value="true"/>
  <fieldset>
    <legend><fmt:message key="domain.search.search_domain" /></legend>
    <table cellpadding="3" cellspacing="1" border="0" width="600">
      <tr class="c1">
        <td width="1%" nowrap><fmt:message key="domain.create.domain" />:</td>
        <td class="c2">
          <input type="text" name="domain" value="<%= ((domainName!=null) ? StringUtils.escapeForXML(domainName) : "") %>" size="30" maxlength="75"/>
        </td>
      </tr>
     <tr><td colspan="2" nowrap><input type="submit" name="search" value="<fmt:message key="domain.search.search" />"/><input type="submit" name="cancel" value="<fmt:message key="global.cancel" />"/></td>
     </tr>
    </table>
  </fieldset>
</form>
<script language="JavaScript" type="text/javascript">
document.f.domain.focus();
</script>

    </body>
</html>
