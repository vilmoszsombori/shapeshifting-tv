<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="eng" uri="http://nsl.shapeshifting.tv/taglibs/engine"%>

<?xml version="1.0" encoding="ISO-8859-1" ?>

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<link rel="shortcut icon" type="image/x-icon" href="rs/img/favicon.ico" />

<title>Parameters debug</title>
</head>
<body>
	<h1>Parameters debug</h1>
	<c:out value="${eng:version()}" />
	<br />
	<hr size="1" />

	<code></code>

	<p>Browsing all the JSTL implicit objects:</p>
	
	<p>"pageContext":</p>
	<c:out value="${pageContext}" /><br />

	<p>"pageScope":</p>
	<c:forEach items="${pageScope}" var="entry">
		<c:out value="${entry.key}" /> = <c:out value="${entry.value}" />
		<br />
	</c:forEach>

	<p>"requestScope":</p>
	<c:forEach items="${requestScope}" var="entry">
		<c:out value="${entry.key}" /> = <c:out value="${entry.value}" />
		<br />
	</c:forEach>

	<p>"sessionScope":</p>
	<c:forEach items="${sessionScope}" var="entry">
		<c:out value="${entry.key}" /> = <c:out value="${entry.value}" />
		<br />
	</c:forEach>

	<p>"applicationScope":</p>
	<c:forEach items="${applicationScope}" var="entry">
		<c:out value="${entry.key}" /> = <c:out value="${entry.value}" />
		<br />
	</c:forEach>

	<p>"param":</p>
	<c:forEach items="${param}" var="entry">
		<c:out value="${entry.key}" /> = <c:out value="${entry.value}" />
		<br />
	</c:forEach>

	<p>"paramValues":</p>
	<c:forEach items="${paramValues}" var="entry">
		<c:out value="${entry}" />
		<br />
	</c:forEach>

	<p>"header":</p>
	<c:forEach items="${header}" var="entry">
		<c:out value="${entry.key}" /> = <c:out value="${entry.value}" />
		<br />
	</c:forEach>

	<p>"headerValues":</p>
	<c:forEach items="${headerValues}" var="entry">
		<c:out value="${entry.key}" /> = <c:out value="${entry.value}" />
		<br />
	</c:forEach>

	<p>"cookie":</p>
	<c:forEach items="${cookie}" var="entry">
		<c:out value="${entry.key}" /> = <c:out value="${entry.value}" />
		<br />
	</c:forEach>

	<p>"initParam":</p>
	<c:forEach items="${initParam}" var="entry">
		<c:out value="${entry.key}" /> = <c:out value="${entry.value}" />
		<br />
	</c:forEach>

	<p>Class path list:</p>
	<c:forTokens
		items="${applicationScope['org.apache.catalina.jsp_classpath']}"
		delims=";" var="entry">
		<c:out value="${entry}" />
		<br />
	</c:forTokens>

</body>
</html>