<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"> 
<xsl:output method="xml" indent="yes"/> 

<xsl:template name="filename">
   <xsl:param name="x"/>
   <xsl:choose>
     <xsl:when test="contains($x,'\')">
       <xsl:call-template name="filename">
         <xsl:with-param name="x" select="substring-after($x,'\')"/>
       </xsl:call-template>
     </xsl:when>
     <xsl:otherwise>
       <xsl:value-of select="$x"/>
     </xsl:otherwise>
   </xsl:choose>
</xsl:template>

<xsl:template name="timecode">
   <xsl:param name="x"/>
   <xsl:value-of select="substring($x,1,2)*3600 + substring($x,4,2)*60 + substring($x,7,2) + substring($x,10,2) div 25"/>
</xsl:template>

<xsl:variable name="trackList" select="'OllieNormanDad Michael Rodrigo StephenConquer AndrewLord AntonyAgar GoldsboroughOllie IanKegel iphone JonWills JulieHill'"/>

<!-- Tracks -->
<xsl:template match="/">
	<ids>
		<xsl:for-each select="segments/segment[0.5 > confidence]">
			<id><xsl:value-of select="id"/></id>					
		</xsl:for-each>
	</ids>			
</xsl:template>
 
</xsl:stylesheet>