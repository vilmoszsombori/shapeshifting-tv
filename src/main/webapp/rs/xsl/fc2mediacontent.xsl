<xsl:stylesheet version="2.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:java="http://xml.apache.org/xalan/java">

<xsl:output method="text" encoding="UTF-8"/>

<xsl:template name="generateId">
	<xsl:variable name="uid" select="java:tv.ShapeShift.nsl.functions.uuid.generate()"/>
	<xsl:value-of select="$uid"/>
</xsl:template>

<xsl:template name="filename">
   <xsl:param name="x"/>
   <xsl:choose>
     <xsl:when test="contains($x,'/')">
       <xsl:call-template name="filename">
         <xsl:with-param name="x" select="substring-after($x,'/')"/>
       </xsl:call-template>
     </xsl:when>
     <xsl:otherwise>
       <xsl:value-of select="$x"/>
     </xsl:otherwise>
   </xsl:choose>
</xsl:template>

<xsl:template match="/">
	@prefix : &lt;http://www.ist-nm2.org/ontology/production#&gt; .
	@prefix production: &lt;http://www.ist-nm2.org/ontology/production#&gt; .
	@prefix xsd: &lt;http://www.w3.org/2001/XMLSchema#&gt; .
	@prefix owl: &lt;http://www.w3.org/2002/07/owl#&gt; .
	@prefix xml: &lt;http://www.w3.org/XML/1998/namespace&gt; .
	@prefix rdf: &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt; .
	@prefix core: &lt;http://www.ist-nm2.org/ontology/core#&gt; .
	@prefix rdfs: &lt;http://www.w3.org/2000/01/rdf-schema#&gt; . 
	@base &lt;http://www.ist-nm2.org/ontology/production&gt; .
	&lt;http://www.ist-nm2.org/ontology/production&gt; rdf:type owl:Ontology .

	<!-- Media content, Video tracks -->
	<xsl:for-each select="xmeml/sequence/media/video//clipitem">
	production:<xsl:call-template name="generateId"/> rdf:type core:MediaContent , owl:NamedIndividual ;
		rdfs:label "<xsl:value-of select="name"/>" ;
		core:hasDuration "<xsl:value-of select="duration * 40"/>"^^xsd:long ;
		core:hasRelativeIn "<xsl:value-of select="start * 40"/>"^^xsd:long ;
		core:hasRelativeOut "<xsl:value-of select="end * 40"/>"^^xsd:long ;
		<!-- core:hasFile "http://vidserv.broadbandappstestbed.com/public_upload/Roly/Woodbridge2mbit/<xsl:value-of select="name"/>.mp4" ; --> <!-- <xsl:value-of select="file/pathurl"/> -->
		core:hasFile "http://vidserv.broadbandappstestbed.com/system/media_objects/<xsl:value-of select="name"/>.mp4" ; <!-- <xsl:value-of select="file/pathurl"/> -->
		core:hasType "video" .
		
	</xsl:for-each>

	<!-- Media content, Audio tracks -->
	<xsl:for-each select="xmeml/sequence/media/audio//clipitem">
		<xsl:if test="file/pathurl">
			production:<xsl:call-template name="generateId"/> rdf:type core:MediaContent , owl:NamedIndividual ;
				rdfs:label "<xsl:value-of select="name"/>" ;
				core:hasDuration "<xsl:value-of select="duration * 40"/>"^^xsd:long ;
				core:hasRelativeIn "<xsl:value-of select="start * 40"/>"^^xsd:long ;
				core:hasRelativeOut "<xsl:value-of select="end * 40"/>"^^xsd:long ;
				<!-- core:hasFile "http://vidserv.broadbandappstestbed.com/public_upload/Michael/AUDIO/MP3/<xsl:value-of select="substring-before(name, '.wav')"/>.mp3" ; --> <!-- <xsl:value-of select="file/pathurl"/> -->
				core:hasFile "http://vidserv.broadbandappstestbed.com/system/media_objects/<xsl:value-of select="substring-before(name, '.wav')"/>.mp3" ; <!-- <xsl:value-of select="file/pathurl"/> -->
				core:hasType "audio" .
		
		</xsl:if>
	</xsl:for-each>
	
	<!-- Markers -->
	<xsl:for-each select="xmeml/sequence/marker">
	production:<xsl:call-template name="generateId"/> rdf:type production:Marker , owl:NamedIndividual ;
		rdfs:label "<xsl:value-of select="name"/>" ;
		core:hasRelativeIn "<xsl:value-of select="in * 40"/>"^^xsd:long .
				
	</xsl:for-each>				
</xsl:template>
</xsl:stylesheet>