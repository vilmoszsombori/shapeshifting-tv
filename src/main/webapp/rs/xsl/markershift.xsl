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

<xsl:variable name="head" select="'Markers shifted by 15000'"/>

<!-- Tracks -->
<xsl:template match="/">
	<xmeml version="5">
	<xsl:element name="sequence">
		<xsl:attribute name="id"><xsl:value-of select="$head"/></xsl:attribute>
		<name><xsl:value-of select="$head"/></name>
		<uuid>4C5D8FFC-D108-45FF-8400-C6DA0E387972</uuid>
		<updatebehavior>add</updatebehavior>
		<duration>0</duration>
		<rate>
			<ntsc>FALSE</ntsc>
			<timebase>25</timebase>
		</rate>
		<timecode>
			<rate>
				<ntsc>FALSE</ntsc>
				<timebase>25</timebase>
			</rate>
			<string>01:00:00:00</string>
			<frame>90000</frame>
			<source>source</source>
			<displayformat>NDF</displayformat>
		</timecode>
		<in>-1</in>
		<out>-1</out>
		<ismasterclip>FALSE</ismasterclip>
		<xsl:for-each select="xmeml/sequence/marker">
			<marker>
				<name><xsl:value-of select="name"/></name>
				<comment><xsl:value-of select="comment"/></comment>
				<color>
					<alpha><xsl:value-of select="color/alpha"/></alpha>
					<red><xsl:value-of select="color/red"/></red>
					<green><xsl:value-of select="color/green"/></green>
					<blue><xsl:value-of select="color/blue"/></blue>				
				</color>
				<in><xsl:value-of select="in + 15000"/></in>
				<out><xsl:value-of select="out"/></out>
			</marker>		
		</xsl:for-each>
	</xsl:element>
	</xmeml>
</xsl:template>
 
</xsl:stylesheet>