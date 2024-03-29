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
       
        <map xmlns="http://www.w3.org/2005/xpath-functions">
        	<string key="groupId">foo</string>
        </map>
    -->

    <xsl:template match="/">
        <xsl:apply-templates select="$input-xml/*"/>
    </xsl:template>

    <xsl:template match="map[not(exists(@key))]">
        <xsl:text>{"topic":"LatestDatum","params":{"sourceIds":"</xsl:text>
        <xsl:value-of select="string[@key='groupId']"/>
        <xsl:text>"}}</xsl:text>
    </xsl:template>

</xsl:stylesheet>
