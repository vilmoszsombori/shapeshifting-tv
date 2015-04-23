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
<xsl:template name="compileTrack">
	<xsl:param name="tracks"/>
	<xsl:variable name="head" select="substring-before($tracks, ' ')"/>
	<xsl:variable name="tail" select="substring-after($tracks, ' ')"/>
	<xmeml version="5">
	<xsl:element name="sequence">
		<xsl:attribute name="id"><xsl:value-of select="$head"/></xsl:attribute>
	<!--  
	<sequence id="Alignment Template">
	-->
		<!--  
		<uuid>17618DC4-3EDF-4283-BDBE-51961C5317BA</uuid>
		-->
		<updatebehavior>add</updatebehavior>
		<name><xsl:value-of select="$head"/></name>
		<duration>15880</duration>
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
		<media>
			<video>
				<format>
					<samplecharacteristics>
						<width>1280</width>
						<height>720</height>
						<anamorphic>FALSE</anamorphic>
						<pixelaspectratio>Square</pixelaspectratio>
						<fielddominance>none</fielddominance>
						<rate>
							<ntsc>FALSE</ntsc>
							<timebase>25</timebase>
						</rate>
						<colordepth>24</colordepth>
						<codec>
							<name>Apple HDV 720p25</name>
							<appspecificdata>
								<appname>Final Cut Pro</appname>
								<appmanufacturer>Apple Inc.</appmanufacturer>
								<appversion>7.0</appversion>
								<data>
									<qtcodec>
										<codecname>Apple HDV 720p25</codecname>
										<codectypename>HDV 720p25</codectypename>
										<codectypecode>hdv5</codectypecode>
										<codecvendorcode>appl</codecvendorcode>
										<spatialquality>1023</spatialquality>
										<temporalquality>0</temporalquality>
										<keyframerate>0</keyframerate>
										<datarate>0</datarate>
									</qtcodec>
								</data>
							</appspecificdata>
						</codec>
					</samplecharacteristics>
					<appspecificdata>
						<appname>Final Cut Pro</appname>
						<appmanufacturer>Apple Inc.</appmanufacturer>
						<appversion>7.0</appversion>
						<data>
							<fcpimageprocessing>
								<useyuv>TRUE</useyuv>
								<usesuperwhite>FALSE</usesuperwhite>
								<rendermode>YUV8BPP</rendermode>
							</fcpimageprocessing>
						</data>
					</appspecificdata>
				</format>
	<!-- Track -->
	<track>
		<xsl:for-each select="segments/segment[confidence >= 0.5 and starts-with(id, $head)]">					
			<xsl:variable name="start">
				<xsl:value-of select="round( start div 40 )"/>
			</xsl:variable>						
			<xsl:variable name="stop">
				<xsl:value-of select="round( stop div 40 )"/>
			</xsl:variable>
			<xsl:variable name="dur">
				<xsl:value-of select="round( (stop - start) div 40 )"/>
			</xsl:variable>
			<xsl:variable name="track">
				<xsl:value-of select="substring(id, 0, 7)"/>
			</xsl:variable>
								
			<xsl:element name="clipitem">
				<xsl:attribute name="id"><xsl:value-of select="id"/></xsl:attribute>
				<name><xsl:value-of select="id"/></name>
				<duration><xsl:value-of select="$dur"/></duration>
				<rate>
					<ntsc>FALSE</ntsc>
					<timebase>25</timebase>
				</rate>
				<in>0</in>
				<out><xsl:value-of select="$dur"/></out>
				<start><xsl:value-of select="$start"/></start>
				<end><xsl:value-of select="$stop"/></end>
				<pixelaspectratio>Square</pixelaspectratio>
				<anamorphic>FALSE</anamorphic>
				<alphatype>none</alphatype>
				<masterclipid><xsl:value-of select="id"/> 1</masterclipid>
				<logginginfo>
					<scene/>
					<shottake/>
					<lognote/>
					<good>FALSE</good>
				</logginginfo>
				<labels>
					<label2/>
				</labels>
				<comments>
					<mastercomment1/>
					<mastercomment2/>
					<mastercomment3/>
					<mastercomment4/>
				</comments>
				<xsl:element name="file">
					<xsl:attribute name="id"><xsl:value-of select="id"/> 2</xsl:attribute>
					<name><xsl:value-of select="id"/></name>
					<pathurl>file://localhost/Volumes/Media/woodbridgenov2011media/<xsl:value-of select="id"/>.mov</pathurl>
					<rate>
						<timebase>25</timebase>
					</rate>
					<duration><xsl:value-of select="$dur"/></duration>
					<!--
					<metadata>
						<storage>QuickTime</storage>
						<key>com.apple.finalcutstudio.media.uuid</key>
						<size>36</size>
						<type>UTF8</type>
						<value>931BAE98-EBF1-45BC-BAB8-572D81DFA0E0</value>
					</metadata>
					<metadata>
						<storage>QuickTime</storage>
						<key>com.apple.finalcutstudio.media.history.uuid</key>
						<size>36</size>
						<type>UTF8</type>
						<value>9742420E-C183-4EA0-BE03-427871B4608E</value>
					</metadata>
					-->
					<timecode>
						<rate>
							<timebase>25</timebase>
						</rate>
						<string>00:00:00:00</string>
						<frame>0</frame>
						<displayformat>NDF</displayformat>
						<source>source</source>
						<reel>
							<name><xsl:value-of select="$head"/></name>
						</reel>
					</timecode>
					<media>
						<video>
							<duration><xsl:value-of select="$dur"/></duration>
							<samplecharacteristics>
								<width>1280</width>
								<height>720</height>
							</samplecharacteristics>
						</video>
						<audio>
							<samplecharacteristics>
								<samplerate>48000</samplerate>
								<depth>16</depth>
							</samplecharacteristics>
							<channelcount>2</channelcount>
							<layout>stereo</layout>
							<audiochannel>
								<sourcechannel>1</sourcechannel>
								<channellabel>left</channellabel>
							</audiochannel>
							<audiochannel>
								<sourcechannel>2</sourcechannel>
								<channellabel>right</channellabel>
							</audiochannel>
						</audio>
					</media>
				</xsl:element>
				<sourcetrack>
					<mediatype>video</mediatype>
				</sourcetrack>
				<fielddominance>none</fielddominance>
				<!--
				<itemhistory>
					<uuid>878E4404-AE21-4768-92D2-156D38B058EE</uuid>
				</itemhistory>
				-->
			</xsl:element>
		</xsl:for-each>			
		<enabled>TRUE</enabled>
		<locked>FALSE</locked>
		</track>
			</video>
			<audio>
				<format>
					<samplecharacteristics>
						<depth>16</depth>
						<samplerate>48000</samplerate>
					</samplecharacteristics>
				</format>
				<outputs>
					<group>
						<index>1</index>
						<numchannels>2</numchannels>
						<downmix>0</downmix>
						<channel>
							<index>1</index>
						</channel>
						<channel>
							<index>2</index>
						</channel>
					</group>
				</outputs>
				<in>-1</in>
				<out>-1</out>
				<track>
					<clipitem id="MasterSequence.wav">
						<name>MasterSequence.wav</name>
						<duration>15880</duration>
						<rate>
							<ntsc>FALSE</ntsc>
							<timebase>25</timebase>
						</rate>
						<in>0</in>
						<out>15880</out>
						<start>0</start>
						<end>15880</end>
						<enabled>TRUE</enabled>
						<masterclipid>MasterSequence.wav1</masterclipid>
						<logginginfo>
							<scene/>
							<shottake/>
							<lognote/>
							<good>FALSE</good>
						</logginginfo>
						<labels>
							<label2/>
						</labels>
						<comments>
							<mastercomment1/>
							<mastercomment2/>
							<mastercomment3/>
							<mastercomment4/>
						</comments>
						<file id="MasterSequence">
							<name>MasterSequence.wav</name>
							<pathurl>file://localhost/Volumes/Media/woodbridgenov2011media/MasterSequence.wav</pathurl>
							<rate>
								<timebase>25</timebase>
							</rate>
							<duration>15880</duration>
							<media>
								<audio>
									<samplecharacteristics>
										<samplerate>48000</samplerate>
										<depth>16</depth>
									</samplecharacteristics>
									<channelcount>2</channelcount>
								</audio>
							</media>
						</file>
						<filter>
							<effect>
								<name>Audio Levels</name>
								<effectid>audiolevels</effectid>
								<effectcategory>audiolevels</effectcategory>
								<effecttype>audiolevels</effecttype>
								<mediatype>audio</mediatype>
								<parameter>
									<name>Level</name>
									<parameterid>level</parameterid>
									<valuemin>0</valuemin>
									<valuemax>3.98109</valuemax>
									<value>1</value>
								</parameter>
							</effect>
						</filter>
						<filter>
							<effect>
								<name>Audio Pan</name>
								<effectid>audiopan</effectid>
								<effectcategory>audiopan</effectcategory>
								<effecttype>audiopan</effecttype>
								<mediatype>audio</mediatype>
								<parameter>
									<name>Pan</name>
									<parameterid>pan</parameterid>
									<valuemin>-1</valuemin>
									<valuemax>1</valuemax>
									<value>-1</value>
								</parameter>
							</effect>
						</filter>
						<sourcetrack>
							<mediatype>audio</mediatype>
							<trackindex>1</trackindex>
						</sourcetrack>
						<link>
							<linkclipref>MasterSequence.wav</linkclipref>
							<mediatype>audio</mediatype>
							<trackindex>1</trackindex>
							<clipindex>1</clipindex>
							<groupindex>1</groupindex>
						</link>
						<link>
							<linkclipref>MasterSequence.wav2</linkclipref>
							<mediatype>audio</mediatype>
							<trackindex>2</trackindex>
							<clipindex>1</clipindex>
							<groupindex>1</groupindex>
						</link>
						<itemhistory>
							<uuid>A71C05FC-20CC-427C-8B95-C5DB9A773D5B</uuid>
						</itemhistory>
					</clipitem>
					<enabled>TRUE</enabled>
					<locked>FALSE</locked>
					<outputchannelindex>1</outputchannelindex>
				</track>
				<track>
					<clipitem id="MasterSequence.wav2">
						<name>MasterSequence.wav</name>
						<duration>15880</duration>
						<rate>
							<ntsc>FALSE</ntsc>
							<timebase>25</timebase>
						</rate>
						<in>0</in>
						<out>15880</out>
						<start>0</start>
						<end>15880</end>
						<enabled>TRUE</enabled>
						<masterclipid>MasterSequence.wav1</masterclipid>
						<logginginfo>
							<scene/>
							<shottake/>
							<lognote/>
							<good>FALSE</good>
						</logginginfo>
						<labels>
							<label2/>
						</labels>
						<comments>
							<mastercomment1/>
							<mastercomment2/>
							<mastercomment3/>
							<mastercomment4/>
						</comments>
						<file id="MasterSequence"/>
						<filter>
							<effect>
								<name>Audio Levels</name>
								<effectid>audiolevels</effectid>
								<effectcategory>audiolevels</effectcategory>
								<effecttype>audiolevels</effecttype>
								<mediatype>audio</mediatype>
								<parameter>
									<name>Level</name>
									<parameterid>level</parameterid>
									<valuemin>0</valuemin>
									<valuemax>3.98109</valuemax>
									<value>1</value>
								</parameter>
							</effect>
						</filter>
						<filter>
							<effect>
								<name>Audio Pan</name>
								<effectid>audiopan</effectid>
								<effectcategory>audiopan</effectcategory>
								<effecttype>audiopan</effecttype>
								<mediatype>audio</mediatype>
								<parameter>
									<name>Pan</name>
									<parameterid>pan</parameterid>
									<valuemin>-1</valuemin>
									<valuemax>1</valuemax>
									<value>1</value>
								</parameter>
							</effect>
						</filter>
						<sourcetrack>
							<mediatype>audio</mediatype>
							<trackindex>2</trackindex>
						</sourcetrack>
						<link>
							<linkclipref>MasterSequence.wav</linkclipref>
							<mediatype>audio</mediatype>
							<trackindex>1</trackindex>
							<clipindex>1</clipindex>
							<groupindex>1</groupindex>
						</link>
						<link>
							<linkclipref>MasterSequence.wav2</linkclipref>
							<mediatype>audio</mediatype>
							<trackindex>2</trackindex>
							<clipindex>1</clipindex>
							<groupindex>1</groupindex>
						</link>
						<itemhistory>
							<uuid>EBC5972D-A164-426F-B098-B6282701C9CD</uuid>
						</itemhistory>
					</clipitem>
					<enabled>TRUE</enabled>
					<locked>FALSE</locked>
					<outputchannelindex>2</outputchannelindex>
				</track>
				<track>
					<enabled>TRUE</enabled>
					<locked>FALSE</locked>
					<outputchannelindex>1</outputchannelindex>
				</track>
				<track>
					<enabled>TRUE</enabled>
					<locked>FALSE</locked>
					<outputchannelindex>2</outputchannelindex>
				</track>
				<filter>
					<effect>
						<name>Audio Levels</name>
						<effectid>audiolevels</effectid>
						<effectcategory>audiolevels</effectcategory>
						<effecttype>audiolevels</effecttype>
						<mediatype>audio</mediatype>
						<parameter>
							<name>Level</name>
							<parameterid>level</parameterid>
							<valuemin>0</valuemin>
							<valuemax>3.98109</valuemax>
							<value>1</value>
						</parameter>
					</effect>
				</filter>
			</audio>
		</media>
		<ismasterclip>FALSE</ismasterclip>
	</xsl:element>
	<!--  
	</sequence>
	-->
	</xmeml>
	
	<xsl:if test="$tail">
		<xsl:call-template name="compileTrack">
			<xsl:with-param name="tracks" select="$tail"/>
		</xsl:call-template>
	</xsl:if>
</xsl:template>

<!-- main iteration --> 
<xsl:template match="/">
	<!-- Compile video tracks -->
	<xsl:call-template name="compileTrack">
		<xsl:with-param name="tracks" select="$trackList"/>
	</xsl:call-template>			
</xsl:template>
 
</xsl:stylesheet>