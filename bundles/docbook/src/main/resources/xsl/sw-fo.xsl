<!--
  Generates single FO document from DocBook XML source using DocBook XSL
  stylesheets.

  See xsl-stylesheets/fo/param.xsl for all parameters.

  NOTE: The URL reference to the current DocBook XSL stylesheets is
  rewritten to point to the copy on the local disk drive by the XML catalog
  rewrite directives so it doesn't need to go out to the Internet for the
  stylesheets. This means you don't need to edit the <xsl:import> elements on
  a machine by machine basis.
-->
<xsl:stylesheet version="1.0"
                xmlns:d="http://docbook.org/ns/docbook"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format">
<xsl:import href="docbook/fo/docbook.xsl"/>

<xsl:param name="body.font.family" select="'serif'"/>
<xsl:param name="body.font.master">8</xsl:param>
<xsl:param name="body.font.size">
 <xsl:value-of select="$body.font.master"/><xsl:text>pt</xsl:text>
</xsl:param>

<xsl:template match="d:para[starts-with(@role , 'hf_')]">
  <fo:block xsl:use-attribute-sets="normal.para.spacing">
    <xsl:attribute name="id">
      <xsl:value-of select="@role" />
    </xsl:attribute>
    <xsl:apply-templates />
  </fo:block>
</xsl:template>


<xsl:template match="d:para[@role = 'pagenum']">
  <fo:block xsl:use-attribute-sets="normal.para.spacing">
    <xsl:apply-templates/>
  	<fo:page-number/>
  </fo:block>
</xsl:template>

<xsl:template match="d:para[@role = 'small']">
  <fo:block font-size="75%" xsl:use-attribute-sets="normal.para.spacing">
    <xsl:call-template name="anchor"/>
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>

<xsl:template match="d:para[@role = 'big']">
  <fo:block font-size="125%" xsl:use-attribute-sets="normal.para.spacing">
    <xsl:call-template name="anchor"/>
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>

<xsl:template match="d:para[@role = 'big2']">
  <fo:block font-size="140%" xsl:use-attribute-sets="normal.para.spacing">
    <xsl:call-template name="anchor"/>
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>

<xsl:template match="d:para[@role = 'big3']">
  <fo:block font-size="165%" xsl:use-attribute-sets="normal.para.spacing">
    <xsl:call-template name="anchor"/>
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>

<xsl:template match="d:para[@role = 'bold']">
  <fo:block font-weight="bold" xsl:use-attribute-sets="normal.para.spacing">
    <xsl:call-template name="anchor"/>
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>

<xsl:template match="d:para[@role = 'smallbold']">
  <fo:block font-size="75%" font-weight="bold" xsl:use-attribute-sets="normal.para.spacing">
    <xsl:call-template name="anchor"/>
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>

<xsl:template match="d:para[@role = 'bigbold']">
  <fo:block font-size="125%" font-weight="bold" xsl:use-attribute-sets="normal.para.spacing">
    <xsl:call-template name="anchor"/>
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>

<xsl:template match="d:para[@role = 'big2bold']">
  <fo:block font-size="140%" font-weight="bold" xsl:use-attribute-sets="normal.para.spacing">
    <xsl:call-template name="anchor"/>
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>

<xsl:template match="d:para[@role = 'big3bold']">
  <fo:block font-size="165%" font-weight="bold" xsl:use-attribute-sets="normal.para.spacing">
    <xsl:call-template name="anchor"/>
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>

<xsl:template match="d:para[@role = 'italic']">
  <fo:block font-style="italic" xsl:use-attribute-sets="normal.para.spacing">
    <xsl:call-template name="anchor"/>
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>

<xsl:template match="d:para[@role = 'smallitalic']">
  <fo:block font-size="75%" font-style="italic" xsl:use-attribute-sets="normal.para.spacing">
    <xsl:call-template name="anchor"/>
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>

<xsl:template match="d:para[@role = 'bigitalic']">
  <fo:block font-size="125%" font-style="italic" xsl:use-attribute-sets="normal.para.spacing">
    <xsl:call-template name="anchor"/>
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>

<xsl:template match="d:para[@role = 'big2italic']">
  <fo:block font-size="140%" font-style="italic" xsl:use-attribute-sets="normal.para.spacing">
    <xsl:call-template name="anchor"/>
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>

<xsl:template match="d:para[@role = 'big3italic']">
  <fo:block font-size="165%" font-style="italic" xsl:use-attribute-sets="normal.para.spacing">
    <xsl:call-template name="anchor"/>
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>

<xsl:attribute-set name="section.title.level1.properties">
  <xsl:attribute name="font-size">
    <xsl:value-of select="$body.font.master * 1.3"/>
    <xsl:text>pt</xsl:text>
  </xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="section.title.level2.properties">
  <xsl:attribute name="font-size">
    <xsl:value-of select="$body.font.master * 1.15"/>
    <xsl:text>pt</xsl:text>
  </xsl:attribute>
</xsl:attribute-set>

<xsl:param name="body.start.indent">0pt</xsl:param>


<xsl:param name="header.left.content"/>
<xsl:param name="header.right.content"/>
<xsl:template name="header.content">  
  <xsl:param name="pageclass" select="''"/>
  <xsl:param name="sequence" select="''"/>
  <xsl:param name="position" select="''"/>
  <xsl:param name="gentext-key" select="''"/>

  <fo:block font-size="120%" font-weight="bold">  
    <xsl:choose>
      <xsl:when test="$position='left'">
     		<xsl:copy-of select="$header.left.content"/>
      </xsl:when>
      <xsl:when test="$position='right'">
     		<xsl:copy-of select="$header.right.content"/>
      </xsl:when>
    </xsl:choose>
  </fo:block>
</xsl:template>



<xsl:param name="paper.type">A4</xsl:param>


<xsl:param name="table.cell.border.style" select="'dotted'"/>
<xsl:param name="table.cell.border.thickness" select="'0.4pt'"/>
<xsl:param name="table.cell.border.color" select="'#777777'"/>

<xsl:param name="table.frame.border.color" select="'#777777'"/>
<xsl:param name="table.frame.border.style" select="'solid'"/>
<xsl:param name="table.frame.border.thickness" select="'0.6pt'"/>
<xsl:param name="tablecolumns.extension" select="'1'"/>

<xsl:attribute-set name="table.cell.padding">
  <xsl:attribute name="padding-start">1.5pt</xsl:attribute>
  <xsl:attribute name="padding-end">1pt</xsl:attribute>
  <xsl:attribute name="padding-top">1pt</xsl:attribute>
  <xsl:attribute name="padding-bottom">1pt</xsl:attribute>
</xsl:attribute-set>

<!--xsl:attribute-set name="informal.object.properties">
  <xsl:attribute name="space-before.minimum">0em</xsl:attribute>
  <xsl:attribute name="space-before.optimum">1em</xsl:attribute>
  <xsl:attribute name="space-before.maximum">1em</xsl:attribute>
  <xsl:attribute name="space-after.minimum">0em</xsl:attribute>
  <xsl:attribute name="space-after.optimum">1em</xsl:attribute>
  <xsl:attribute name="space-after.maximum">1em</xsl:attribute>
</xsl:attribute-set-->

<xsl:template name="table.cell.block.properties">
  <!-- highlight this entry? -->
  <xsl:choose>
    <xsl:when test="ancestor::d:thead or ancestor::d:tfoot">
      <xsl:attribute name="font-weight">bold</xsl:attribute>
      <xsl:attribute name="background-color">#DDDDDD</xsl:attribute>
    </xsl:when>
    <!-- Make row headers bold too -->
    <xsl:when test="ancestor::d:tbody and 
                    (ancestor::d:table[@rowheader = 'firstcol'] or
                    ancestor::d:informaltable[@rowheader = 'firstcol']) and
                    ancestor-or-self::d:entry[1][count(preceding-sibling::d:entry) = 0]">
      <xsl:attribute name="font-weight">bold</xsl:attribute>
    </xsl:when>
  </xsl:choose>
</xsl:template>

<xsl:attribute-set name="table.properties">
  <xsl:attribute name="keep-together.within-column">always</xsl:attribute>
</xsl:attribute-set>



<!--Pagemaster with different dimensions @@@MS-->
<xsl:param name="region.before.extent">0pt</xsl:param>
<xsl:param name="region.after.extent">0pt</xsl:param>
<xsl:param name="page.margin.top">0.10in</xsl:param>
<xsl:param name="page.margin.bottom">0.10in</xsl:param>
<xsl:param name="body.margin.top">0.25in</xsl:param>
<xsl:param name="body.margin.bottom">0.25in</xsl:param>

<xsl:param name="region.before.extent.odd">0pt</xsl:param>
<xsl:param name="region.after.extent.odd">0pt</xsl:param>
<xsl:param name="page.margin.top.odd">0.10in</xsl:param>
<xsl:param name="page.margin.bottom.odd">0.10in</xsl:param>
<xsl:param name="body.margin.top.odd">0.25in</xsl:param>
<xsl:param name="body.margin.bottom.odd">0.25in</xsl:param>

<xsl:param name="region.before.extent.even">0pt</xsl:param>
<xsl:param name="region.after.extent.even">0pt</xsl:param>
<xsl:param name="page.margin.top.even">0.10in</xsl:param>
<xsl:param name="page.margin.bottom.even">0.10in</xsl:param>
<xsl:param name="body.margin.top.even">0.25in</xsl:param>
<xsl:param name="body.margin.bottom.even">0.25in</xsl:param>

<xsl:template name="select.user.pagemaster">
  <xsl:param name="element" />
  <xsl:param name="pageclass" />
  <xsl:param name="default-pagemaster" />
  <xsl:value-of select="'body-ms'" />
</xsl:template>

<xsl:template name="user.pagemasters">
	<fo:page-sequence-master master-name="body-ms">
		<fo:repeatable-page-master-alternatives>
			<fo:conditional-page-master-reference master-reference="blank" blank-or-not-blank="blank" />
			<fo:conditional-page-master-reference master-reference="body-first-ms" page-position="first" />
			<fo:conditional-page-master-reference master-reference="body-odd-ms" odd-or-even="odd" />
			<fo:conditional-page-master-reference odd-or-even="even">
				<xsl:attribute name="master-reference">
					<xsl:choose>
						<xsl:when test="$double.sided != 0">body-even-ms</xsl:when>
						<xsl:otherwise>body-odd-ms</xsl:otherwise>
					</xsl:choose>
				</xsl:attribute>
			</fo:conditional-page-master-reference>
		</fo:repeatable-page-master-alternatives>
	</fo:page-sequence-master>

  <!-- body pages -->
  <fo:simple-page-master master-name="body-first-ms" page-width="{$page.width}" page-height="{$page.height}" margin-top="{$page.margin.top}" margin-bottom="{$page.margin.bottom}">
    <xsl:attribute name="margin-{$direction.align.start}">
      <xsl:value-of select="$page.margin.inner" />
    </xsl:attribute>
    <xsl:attribute name="margin-{$direction.align.end}">
      <xsl:value-of select="$page.margin.outer" />
    </xsl:attribute>
    <fo:region-body margin-bottom="{$body.margin.bottom}" margin-top="{$body.margin.top}" column-gap="{$column.gap.body}" column-count="{$column.count.body}">
      <xsl:attribute name="margin-{$direction.align.start}">
        <xsl:value-of select="$body.margin.inner" />
      </xsl:attribute>
      <xsl:attribute name="margin-{$direction.align.end}">
        <xsl:value-of select="$body.margin.outer" />
      </xsl:attribute>
    </fo:region-body>
    <fo:region-before region-name="xsl-region-before-first" extent="{$region.before.extent}" precedence="{$region.before.precedence}" display-align="before" />
    <fo:region-after region-name="xsl-region-after-first" extent="{$region.after.extent}" precedence="{$region.after.precedence}" display-align="after" />
    <xsl:call-template name="region.inner">
      <xsl:with-param name="sequence">first</xsl:with-param>
      <xsl:with-param name="pageclass">body</xsl:with-param>
    </xsl:call-template>
    <xsl:call-template name="region.outer">
      <xsl:with-param name="sequence">first</xsl:with-param>
      <xsl:with-param name="pageclass">body</xsl:with-param>
    </xsl:call-template>
  </fo:simple-page-master>

  <fo:simple-page-master master-name="body-odd-ms" page-width="{$page.width}" page-height="{$page.height}" margin-top="{$page.margin.top.odd}" margin-bottom="{$page.margin.bottom.odd}">
    <xsl:attribute name="margin-{$direction.align.start}">
      <xsl:value-of select="$page.margin.inner" />
    </xsl:attribute>
    <xsl:attribute name="margin-{$direction.align.end}">
      <xsl:value-of select="$page.margin.outer" />
    </xsl:attribute>
    <fo:region-body margin-bottom="{$body.margin.bottom.odd}" margin-top="{$body.margin.top.odd}" column-gap="{$column.gap.body}" column-count="{$column.count.body}">
      <xsl:attribute name="margin-{$direction.align.start}">
        <xsl:value-of select="$body.margin.inner" />
      </xsl:attribute>
      <xsl:attribute name="margin-{$direction.align.end}">
        <xsl:value-of select="$body.margin.outer" />
      </xsl:attribute>
    </fo:region-body>
    <fo:region-before region-name="xsl-region-before-odd" extent="{$region.before.extent.odd}" precedence="{$region.before.precedence}" display-align="before" />
    <fo:region-after region-name="xsl-region-after-odd" extent="{$region.after.extent.odd}" precedence="{$region.after.precedence}" display-align="after" />
    <xsl:call-template name="region.inner">
      <xsl:with-param name="pageclass">body</xsl:with-param>
      <xsl:with-param name="sequence">odd</xsl:with-param>
    </xsl:call-template>
    <xsl:call-template name="region.outer">
      <xsl:with-param name="pageclass">body</xsl:with-param>
      <xsl:with-param name="sequence">odd</xsl:with-param>
    </xsl:call-template>
  </fo:simple-page-master>

  <fo:simple-page-master master-name="body-even-ms" page-width="{$page.width}" page-height="{$page.height}" margin-top="{$page.margin.top.even}" margin-bottom="{$page.margin.bottom.even}">
    <xsl:attribute name="margin-{$direction.align.start}">
      <xsl:value-of select="$page.margin.outer" />
    </xsl:attribute>
    <xsl:attribute name="margin-{$direction.align.end}">
      <xsl:value-of select="$page.margin.inner" />
    </xsl:attribute>
    <fo:region-body margin-bottom="{$body.margin.bottom.even}" margin-top="{$body.margin.top.even}" column-gap="{$column.gap.body}" column-count="{$column.count.body}">
      <xsl:attribute name="margin-{$direction.align.start}">
        <xsl:value-of select="$body.margin.outer" />
      </xsl:attribute>
      <xsl:attribute name="margin-{$direction.align.end}">
        <xsl:value-of select="$body.margin.inner" />
      </xsl:attribute>
    </fo:region-body>
    <fo:region-before region-name="xsl-region-before-even" extent="{$region.before.extent.even}" precedence="{$region.before.precedence}" display-align="before" />
    <fo:region-after region-name="xsl-region-after-even" extent="{$region.after.extent.even}" precedence="{$region.after.precedence}" display-align="after" />
    <xsl:call-template name="region.outer">
      <xsl:with-param name="pageclass">body</xsl:with-param>
      <xsl:with-param name="sequence">even</xsl:with-param>
    </xsl:call-template>
    <xsl:call-template name="region.inner">
      <xsl:with-param name="pageclass">body</xsl:with-param>
      <xsl:with-param name="sequence">even</xsl:with-param>
    </xsl:call-template>
  </fo:simple-page-master>
</xsl:template>
</xsl:stylesheet>
