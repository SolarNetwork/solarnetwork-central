<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xpath-default-namespace="http://www.w3.org/2005/xpath-functions" version="3.0">

    <xsl:param name="input-json">
        {
            "ts": "2024-02-22T12:00:00Z",
            "node": 123,
            "source": "test/1",
            "props": {
                "foo": 123,
                "bim": 234,
                "msg": "Hello"
            }
        }
    </xsl:param>

    <xsl:mode on-no-match="deep-skip"/>
    <xsl:output method="text"/>

    <xsl:variable name="input-xml" select="json-to-xml($input-json)"/>
    
    <!--
       The XML form in $input-xml looks like this:
       
        <map xmlns="http://www.w3.org/2005/xpath-functions">
        	<string key="ts">2024-02-22T12:00:00Z</string>
        	<number key="node">123</number>
        	<string key="source">test/1</string>
        	<map key="props">
        		<number key="foo">123</number>
        		<number key="bim">234</number>
        		<string key="msg">Hello</string>
        	</map>
        </map>
    -->

    <xsl:template match="/">
        <xsl:apply-templates select="$input-xml/*"/>
    </xsl:template>

    <xsl:template match="map[not(exists(@key))]">
        <xsl:text>{"created":"</xsl:text>
        <xsl:value-of select="
                if (exists(string[@key = 'ts'])) then
                    string[@key = 'ts']
                else
                    current-dateTime()"/>
        <xsl:text>"</xsl:text>
        <xsl:if test="exists(number[@key = 'node'])">
            <xsl:text>,"nodeId":</xsl:text>
            <xsl:value-of select="number[@key = 'node']"/>
        </xsl:if>
        <xsl:if test="exists(string[@key = 'source'])">
            <xsl:text>,"sourceId":"</xsl:text>
            <xsl:value-of select="string[@key = 'source']"/>
            <xsl:text>"</xsl:text>
        </xsl:if>
        <!--
            Convert all <map key="props"><number> values into instantaneous datum properties.
        -->
        <xsl:variable name="instantaneous" select="map[@key = 'props']/number"/>
        <xsl:if test="count($instantaneous) gt 0">
            <xsl:text>,"i":{</xsl:text>
            <xsl:apply-templates select="$instantaneous"/>
            <xsl:text>}</xsl:text>
        </xsl:if>
        <!--
            Convert all <map key="props"><string> values into instantaneous datum properties.
        -->
        <xsl:variable name="status" select="map[@key = 'props']/string"/>
        <xsl:if test="count($status) gt 0">
            <xsl:text>,"s":{</xsl:text>
            <xsl:apply-templates select="$status"/>
            <xsl:text>}</xsl:text>
        </xsl:if>
        <xsl:text>}</xsl:text>
    </xsl:template>

    <xsl:template match="map[@key = 'props']/number">
        <xsl:if test="position() gt 1">,</xsl:if>
        <xsl:text>"</xsl:text>
        <xsl:value-of select="@key"/>
        <xsl:text>":</xsl:text>
        <xsl:value-of select="."/>
    </xsl:template>

    <xsl:template match="map[@key = 'props']/string">
        <xsl:if test="position() gt 1">,</xsl:if>
        <xsl:text>"</xsl:text>
        <xsl:value-of select="@key"/>
        <xsl:text>":"</xsl:text>
        <xsl:value-of select="."/>
        <xsl:text>"</xsl:text>
    </xsl:template>

</xsl:stylesheet>
