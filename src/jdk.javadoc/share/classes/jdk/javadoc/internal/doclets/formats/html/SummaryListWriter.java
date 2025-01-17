/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.javadoc.internal.doclets.formats.html;

import java.util.SortedSet;

import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;

import jdk.javadoc.internal.doclets.formats.html.markup.ContentBuilder;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlId;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle;
import jdk.javadoc.internal.doclets.formats.html.markup.Script;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTree;
import jdk.javadoc.internal.doclets.formats.html.Navigation.PageMode;
import jdk.javadoc.internal.doclets.formats.html.markup.Text;
import jdk.javadoc.internal.doclets.toolkit.util.DocFileIOException;
import jdk.javadoc.internal.doclets.toolkit.util.DocPath;
import jdk.javadoc.internal.doclets.toolkit.util.SummaryAPIListBuilder;
import jdk.javadoc.internal.doclets.toolkit.util.SummaryAPIListBuilder.SummaryElementKind;

/**
 * Base class for generating a summary page that lists elements with a common characteristic,
 * such as deprecated elements, preview elements, and so on.
 */
public abstract class SummaryListWriter<B extends SummaryAPIListBuilder> extends SubWriterHolderWriter {

    protected String getHeadingKey(SummaryElementKind kind) {
        return switch (kind) {
            case MODULE -> "doclet.Modules";
            case PACKAGE -> "doclet.Packages";
            case INTERFACE -> "doclet.Interfaces";
            case CLASS -> "doclet.Classes";
            case ENUM -> "doclet.Enums";
            case EXCEPTION_CLASS -> "doclet.ExceptionClasses";
            case ANNOTATION_TYPE -> "doclet.Annotation_Types";
            case FIELD -> "doclet.Fields";
            case METHOD -> "doclet.Methods";
            case CONSTRUCTOR -> "doclet.Constructors";
            case ENUM_CONSTANT -> "doclet.Enum_Constants";
            case ANNOTATION_TYPE_MEMBER -> "doclet.Annotation_Type_Members";
            case RECORD_CLASS -> "doclet.RecordClasses";
        };
    }

    private String getHeaderKey(SummaryElementKind kind) {
        return switch (kind) {
            case MODULE -> "doclet.Module";
            case PACKAGE -> "doclet.Package";
            case INTERFACE -> "doclet.Interface";
            case CLASS -> "doclet.Class";
            case ENUM -> "doclet.Enum";
            case EXCEPTION_CLASS -> "doclet.ExceptionClass";
            case ANNOTATION_TYPE -> "doclet.AnnotationType";
            case FIELD -> "doclet.Field";
            case METHOD -> "doclet.Method";
            case CONSTRUCTOR -> "doclet.Constructor";
            case ENUM_CONSTANT -> "doclet.Enum_Constant";
            case ANNOTATION_TYPE_MEMBER -> "doclet.Annotation_Type_Member";
            case RECORD_CLASS -> "doclet.RecordClass";
        };
    }

    /** The summary list builder */
    protected final B builder;

    /**
     * Constructor.
     *
     * @param configuration the configuration for this doclet
     * @param filename the file to be generated
     * @param builder the summary list builder
     */
    public SummaryListWriter(HtmlConfiguration configuration, DocPath filename, B builder) {
        super(configuration, filename);
        this.builder = builder;
    }

    /**
     * Generate the API summary.
     *
     * @param pageMode page mode to use
     * @param description page description
     * @param headContent page heading content
     * @param titleKey page title resource key
     * @throws DocFileIOException if there is a problem writing the summary list
     */
    protected void generateSummaryListFile(PageMode pageMode, String description,
                                           Content headContent, String titleKey)
            throws DocFileIOException {
        HtmlTree body = getHeader(pageMode, titleKey);
        Content content = new ContentBuilder();
        var heading = HtmlTree.HEADING_TITLE(Headings.PAGE_TITLE_HEADING,
                HtmlStyle.title, headContent);
        content.add(HtmlTree.DIV(HtmlStyle.header, heading));
        addContentSelectors(content);
        content.add(HtmlTree.HEADING_TITLE(Headings.CONTENT_HEADING, contents.contentsHeading));
        content.add(getContentsList());
        addExtraSection(content);
        for (SummaryElementKind kind : SummaryElementKind.values()) {
            if (builder.hasDocumentation(kind)) {
                addSummaryAPI(builder.getSet(kind), HtmlIds.forSummaryKind(kind),
                            getHeadingKey(kind), getHeaderKey(kind), content);
            }
        }
        bodyContents.addMainContent(content);
        // The script below enables checkboxes in the page and invokes their click handler
        // to restore any previous state when the page is loaded via back/forward button.
        bodyContents.addMainContent(new Script("""
                document.addEventListener("DOMContentLoaded", function(e) {
                    document.querySelectorAll('input[type="checkbox"]').forEach(
                        function(c) {
                            c.disabled = false;
                            c.onclick();
                        });
                    });
                window.addEventListener("load", function(e) {
                    document.querySelectorAll('input[type="checkbox"]').forEach(
                        function(c) {
                            c.onclick();
                        });
                    });
                """).asContent());
        bodyContents.setFooter(getFooter());
        body.add(bodyContents);
        printHtmlDocument(null, description, body);
    }

    /**
     * Add the index link.
     *
     * @param id the id for the link
     * @param headingKey the key for the heading content
     * @param content the content to which the index link will be added
     */
    protected void addIndexLink(HtmlId id, String headingKey, Content content) {
        // The "contents-" + id value is used in JavaScript code to toggle visibility of the link.
        var li = HtmlTree.LI(links.createLink(id,
                contents.getContent(headingKey))).setId(HtmlId.of("contents-" + id.name()));
        content.add(li);
    }

    /**
     * Get the contents list.
     *
     * @return the contents list
     */
    public Content getContentsList() {
        var ul= HtmlTree.UL(HtmlStyle.contentsList);
        addExtraIndexLink(ul);
        for (SummaryElementKind kind : SummaryElementKind.values()) {
            if (builder.hasDocumentation(kind)) {
                addIndexLink(HtmlIds.forSummaryKind(kind), getHeadingKey(kind), ul);
            }
        }
        return ul;
    }

    /**
     * @param pageMode page mode to use
     * @param titleKey page title resource key
     * {@return the header for the API Summary listing}
     */
    public HtmlTree getHeader(PageMode pageMode, String titleKey) {
        String title = resources.getText(titleKey);
        HtmlTree body = getBody(getWindowTitle(title));
        bodyContents.setHeader(getHeader(pageMode));
        return body;
    }

    /**
     * Add summary information to the documentation
     *
     * @param apiList list of API summary elements
     * @param id the id attribute of the table
     * @param headingKey the caption for the summary table
     * @param headerKey table header key for the summary table
     * @param content the content to which the summary table will be added
     */
    protected void addSummaryAPI(SortedSet<Element> apiList, HtmlId id,
                                 String headingKey, String headerKey,
                                 Content content) {
        if (apiList.size() > 0) {
            TableHeader tableHeader = getTableHeader(headerKey);

            var table = new Table<Element>(HtmlStyle.summaryTable)
                    .setCaption(getTableCaption(headingKey))
                    .setHeader(tableHeader)
                    .setId(id)
                    .setColumnStyles(getColumnStyles());
            addTableTabs(table, headingKey);
            for (Element e : apiList) {
                Content link;
                switch (e.getKind()) {
                    case MODULE -> {
                        ModuleElement m = (ModuleElement) e;
                        link = getModuleLink(m, Text.of(m.getQualifiedName()));
                    }
                    case PACKAGE -> {
                        PackageElement pkg = (PackageElement) e;
                        link = getPackageLink(pkg, getLocalizedPackageName(pkg));
                    }
                    default -> link = getSummaryLink(e);
                }
                Content extraContent = getExtraContent(e);
                Content desc = new ContentBuilder();
                addComments(e, desc);
                if (extraContent != null) {
                    table.addRow(e, link, extraContent, desc);
                } else {
                    table.addRow(e, link, desc);
                }
            }
            // note: singleton list
            content.add(HtmlTree.UL(HtmlStyle.blockList, HtmlTree.LI(table)));
        }
    }

    /**
     * Add summary text for the given element.
     *
     * @param e the element for which the summary text should be added
     * @param desc the content to which the text should be added
     */
    protected void addComments(Element e, Content desc) {
    }

    protected Content getSummaryLink(Element e) {
        // TODO: notable that these do not go through the writerFactory
        //       also maybe notable that annotation type members are not handled as such
        AbstractMemberWriter writer = switch (e.getKind()) {
            case INTERFACE, CLASS, ENUM,
                 ANNOTATION_TYPE, RECORD -> new NestedClassWriter(this);
            case FIELD -> new FieldWriter(this);
            case METHOD -> new MethodWriter(this);
            case CONSTRUCTOR -> new ConstructorWriter(this);
            case ENUM_CONSTANT -> new EnumConstantWriter(this);
            case RECORD_COMPONENT ->
                throw new AssertionError("Record components are not supported by SummaryListWriter!");
            default ->
                throw new UnsupportedOperationException("Unsupported element kind: " + e.getKind());
        };
        return writer.getSummaryLink(e);
    }

    /**
     * Add an extra optional section to the content.
     *
     * @param target the content to which the section should be added
     */
    protected void addExtraSection(Content target) {
    }

    /**
     * Add an extra optional index link.
     *
     * @param target the content to which the link should be added
     */
    protected void addExtraIndexLink(Content target) {
    }

    /**
     * Subclasses allow the user to show or hide parts of the content in the page.
     * This method should be used to add the UI to select the visible page content.
     *
     * @param target the content to which the UI should be added
     */
    protected abstract void addContentSelectors(Content target);

    /**
     * Some subclasses of this class display an extra column in their element tables.
     * This methods allows them to return the content to show for {@code element}.
     *
     * @param element the element
     * @return content for extra content or null
     */
    protected abstract Content getExtraContent(Element element);

    /**
     * Gets the table header to use for a table with the first column identified by {@code headerKey}.
     *
     * @param headerKey the header key for the first table column
     * @return the table header
     */
    protected TableHeader getTableHeader(String headerKey) {
        return new TableHeader(
                contents.getContent(headerKey), contents.descriptionLabel);
    }

    /**
     * Gets the array of styles to use for table columns. The length of the returned
     * array must match the number of column headers returned by {@link #getTableHeader(String)}.
     *
     * @return the styles to use for table columns
     */
    protected HtmlStyle[] getColumnStyles() {
        return new HtmlStyle[]{ HtmlStyle.colSummaryItemName, HtmlStyle.colLast };
    }

    /**
     * Returns the caption for the table with the given {@code headingKey}.
     *
     * @param headingKey the key for the table heading
     * @return the table caption
     */
    protected Content getTableCaption(String headingKey) {
        return contents.getContent(headingKey);
    }

    /**
     * Allow subclasses to add extra tabs to the element tables.
     *
     * @param table the element table
     * @param headingKey the key for the caption (default tab)
     */
    protected abstract void addTableTabs(Table<Element> table, String headingKey);
}
