<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xpath-default-namespace="http://www.w3.org/2005/xpath-functions" version="3.0">

    <xsl:param name="input-json">
        {"groupId": "foo"}
    </xsl:param>

    <xsl:mode on-no-match="deep-skip"/>
    <xsl:output method="text"/>

    <xsl:variable name="input-xml" select="json-to-xml($input-json)"/>
    
    <!--
       The XML form in $input-xml looks like this:
       
        <array xmlns="http://www.w3.org/2005/xpath-functions">
            <map>
                <string key="topic">LatestDatum</string>
                <string key="instructionDate">2024-03-29 18:05:56.85217Z</string>
                <string key="statusDate">2024-03-29 18:05:57.852862Z</string>
                <string key="state">Completed</string>
                <array key="parameters">
                    <map>
                        <string key="name">bim</string>
                        <string key="value">bam</string>
                    </map>
                    <map>
                        <string key="name">foo</string>
                        <string key="value">bar</string>
                    </map>
                </array>
                <number key="nodeId">123</number>
                <map key="resultParameters">
                    <array key="datum">
                        <map>
                            <string key="sourceId">test/1</string>
                            <number key="watts">123</number>
                            <number key="voltage">240</number>
                        </map>
                        <map>
                            <string key="sourceId">test/2</string>
                            <number key="watts">234</number>
                            <number key="voltage">241</number>
                        </map>
                    </array>
                </map>
            </map>
        </array>
    -->

    <xsl:template match="/">
        <xsl:apply-templates select="$input-xml/*"/>
    </xsl:template>
    
    <xsl:template match="/array">
        <xsl:text>{"total-power":</xsl:text>
        <xsl:value-of select="sum(//array[@key='datum']/map/number[@key='watts'])"/>
        <xsl:text>}</xsl:text>
    </xsl:template>

</xsl:stylesheet>
