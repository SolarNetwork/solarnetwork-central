<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:math="http://www.w3.org/2005/xpath-functions/math" exclude-result-prefixes="xs math"
    version="3.0">

    <xsl:output method="text"/>
    
    <!--
        Support both a root-level <data/> element or a nested list like <array><data/><data/></array>
    -->
    
    <xsl:template match="/">
        <xsl:if test="count(//data) gt 1">[</xsl:if>
        <xsl:apply-templates select="//data"/>
        <xsl:if test="count(//data) gt 1">]</xsl:if>
    </xsl:template>

    <xsl:template match="data">
        <xsl:if test="position() gt 1">,</xsl:if>
        <!--
            Always generate a "created" property so it is easier to generate all subsequent properties
            with a leading comma character.
        -->
        <xsl:text>{"created":"</xsl:text>
        <xsl:value-of select="if (exists(@ts)) then @ts else current-dateTime()"/>
        <xsl:text>"</xsl:text>
        <!--
            Support both node and location datum.
        -->
        <xsl:choose>
            <xsl:when test="number(@node) eq number(@node)">
                <xsl:text>,"nodeId":</xsl:text>
                <xsl:value-of select="@node"/>
            </xsl:when>
            <xsl:when test="number(@location) eq number(@location)">
                <xsl:text>,"locationId":</xsl:text>
                <xsl:value-of select="@location"/>
            </xsl:when>
        </xsl:choose>      
        <xsl:if test="exists(@source)">
            <xsl:text>,"sourceId":"</xsl:text>
            <xsl:value-of select="@source"/>
            <xsl:text>"</xsl:text>
        </xsl:if>
        <!--
            Convert all <prop> values that are numbers into instantaneous datum properties.
        -->
        <xsl:variable name="instantaneous" select="prop[number() eq number()]"/>
        <xsl:if test="count($instantaneous) gt 0">
            <xsl:text>,"i":{</xsl:text>
            <xsl:apply-templates select="$instantaneous"/>
            <xsl:text>}</xsl:text>
        </xsl:if>
        <!--
            Convert all <prop> values that are NOT numbers into status datum properties.
        -->
        <xsl:variable name="status" select="./prop[number() ne number()]"/>
        <xsl:if test="count($status) gt 0">
            <xsl:text>,"s":{</xsl:text>
            <xsl:apply-templates select="$status"/>
            <xsl:text>}</xsl:text>
        </xsl:if>
        <xsl:text>}</xsl:text>
    </xsl:template>

    <xsl:template match="prop">
        <xsl:if test="position() gt 1">
            <xsl:text>,</xsl:text>
        </xsl:if>
        <xsl:text>"</xsl:text>
        <xsl:value-of select="@name"/>
        <xsl:text>":</xsl:text>
        <xsl:variable name="isNumber" select="number() eq number()"/>
        <xsl:if test="not($isNumber)">
            <xsl:text>"</xsl:text>
        </xsl:if>
        <xsl:value-of select="."/>
        <xsl:if test="not($isNumber)">
            <xsl:text>"</xsl:text>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
