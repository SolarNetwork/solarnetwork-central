<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:math="http://www.w3.org/2005/xpath-functions/math" exclude-result-prefixes="xs math"
    version="3.0">

    <xsl:output method="text"/>
    
    <xsl:template match="/data">
        <xsl:text>{"topic":"LatestDatum","params":{"sourceIds":"</xsl:text>
        <xsl:value-of select="@groupId"/>
        <xsl:text>"}}</xsl:text>
    </xsl:template>

</xsl:stylesheet>
