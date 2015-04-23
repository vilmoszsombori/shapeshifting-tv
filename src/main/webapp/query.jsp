<!--
 Simple interface for testing a SPARQL queries 
-->

<!DOCTYPE html>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<html>
<head>
<link href="shapeshift.css" rel="stylesheet" type="text/css" />
<link rel="shortcut icon" type="image/x-icon" href="rs/img/favicon.ico" />
<title>Simple SPARQL query/update dispatcher</title>
</head>

<body>
	<h1>Simple SPARQL query/update dispatcher</h1>

	<hr size="1" />

	<form action="query" method="GET">
		<c:choose>
			<c:when test="${requestScope['query'] == null}">
				<textarea rows="30" cols="150" name="query" autofocus="autofocus"><c:out value="${'PREFIX nsl: <http://shapeshifting.tv/ontology/nsl#>
PREFIX production: <http://shapeshifting.tv/ontology/production#>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> 
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> 

SELECT 
WHERE { 
}'}"/></textarea>
			</c:when>
			<c:otherwise>
				<textarea rows="30" cols="150" name="query" autofocus="autofocus"><c:out value="${requestScope['query']}" /></textarea>
			</c:otherwise>
		</c:choose>		
		<br />
		<label>
			<c:choose>
				<c:when test="${param['model'] == 'Inference'}">
					<input type="radio" name="model" value="Inference" required checked/>
				</c:when>
				<c:otherwise>
					<input type="radio" name="model" value="Inference" required/>
				</c:otherwise>
			</c:choose>
			Inference
		</label>

		<label>
			<c:choose>
				<c:when test="${param['model'] == 'Ontology'}">
					<input type="radio" name="model" value="Ontology" checked/>
				</c:when>
				<c:otherwise>
					<input type="radio" name="model" value="Ontology"/>
				</c:otherwise>
			</c:choose>
			Ontology
		</label>

		<label>
			<c:choose>
				<c:when test="${param['model'] == 'Raw'}">
					<input type="radio" name="model" value="Raw" checked/>
				</c:when>
				<c:otherwise>
					<input type="radio" name="model" value="Raw"/>
				</c:otherwise>
			</c:choose>
			Raw
		</label>
		<!--
		<label><input type="radio" name="model" value="Raw" <c:if test="${param['model'] == 'Raw'}">checked="true"</c:if>/>Raw</label>
		<input type="radio" name="model" value="Ontology"
			<c:if test="${param['model'] == 'Ontology'}">checked="true"</c:if>>Ontology</input>
		<input type="radio" name="model" value="Inference"
			<c:if test="${param['model'] == 'Inference'}">checked="true"</c:if>>Inference</input>
		--> 
		<br /> 
		<input type="submit" name="command" value="Query"/> 
		<input type="submit" name="command" value="Construct"/> 
		<input type="submit" name="command" value="Update"/>
	</form>

	<hr size="1" />

	<c:if test="${requestScope['result'] != null}">
		<p class="grey16bf">Response</p>
		<pre><c:out value="${requestScope['result']}" /></pre>
	</c:if>

</body>

</html>


