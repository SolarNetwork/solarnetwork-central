<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:math="http://www.w3.org/2005/xpath-functions/math" exclude-result-prefixes="xs math"
    version="3.0">

    <xsl:param name="previous-input">
        <xsl:text><![CDATA[<data><prop name="foo">123</prop></data>]]></xsl:text>
    </xsl:param>

    <xsl:variable name="prev-doc" select="
            doc(concat('data:text/xml,', encode-for-uri(if (string-length($previous-input) gt 0) then
                $previous-input
            else
                '&lt;data/&gt;')))/data"/>

    <xsl:output method="text"/>

    <xsl:template match="/data">
        <xsl:text>{"created":"</xsl:text>
        <xsl:value-of select="
                if (exists(@ts)) then
                    @ts
                else
                    current-dateTime()"/>
        <xsl:text>","i":{</xsl:text>
        <xsl:apply-templates select="prop"/>
        <xsl:text>}}</xsl:text>
    </xsl:template>

    <xsl:template match="prop">
        <xsl:if test="position() gt 1">
            <xsl:text>,</xsl:text>
        </xsl:if>
        <xsl:text>"</xsl:text>
        <xsl:value-of select="@name"/>
        <xsl:text>":</xsl:text>
        <xsl:variable name="prev-prop" select="$prev-doc/prop[@name eq ./@name]"/>
        <xsl:value-of select="
                xs:decimal(.) - (if (exists($prev-prop)) then
                    xs:decimal($prev-prop)
                else
                    0)"/>
    </xsl:template>

</xsl:stylesheet>
