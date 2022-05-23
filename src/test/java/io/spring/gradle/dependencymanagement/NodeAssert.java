/*
 * Copyright 2014-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.gradle.dependencymanagement;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.AbstractListAssert;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AssertJ {@link AssertProvider} for {@link Node} assertions.
 *
 * @author Andy Wilkinson
 */
public class NodeAssert extends AbstractAssert<NodeAssert, Node> {

    private static final DocumentBuilderFactory FACTORY = DocumentBuilderFactory.newInstance();

    private final XPathFactory xpathFactory = XPathFactory.newInstance();

    private final XPath xpath = this.xpathFactory.newXPath();

    public NodeAssert(String xmlContent) {
        this(read(xmlContent));
    }

    private NodeAssert(Node actual) {
        super(actual, NodeAssert.class);
    }

    public NodeAssert(File file) {
        this(read(file));
    }

    private static String read(File file) {
        FileReader reader = null;
        try {
            reader = new FileReader(file);
            StringWriter writer = new StringWriter();
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) > 0) {
                writer.write(buffer, 0, read);
            }
            return writer.toString();
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException ex2) {
                    // Swallow
                }
            }
        }
    }

    private static Document read(String xmlContent) {
        try {
            return FACTORY.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public NodeAssert nodeAtPath(String xpath) {
        try {
            return new NodeAssert((Node) this.xpath.evaluate(xpath, this.actual, XPathConstants.NODE));
        }
        catch (XPathExpressionException ex) {
            throw new RuntimeException(ex);
        }
    }

    public AbstractListAssert<?, ? extends List<? extends Node>, Node> nodesAtPath(String xpath) {
        try {
            NodeList nodeList = (NodeList) this.xpath.evaluate(xpath, this.actual, XPathConstants.NODESET);
            return assertThat(toList(nodeList));
        }
        catch (XPathExpressionException ex) {
            throw new RuntimeException(ex);
        }
    }

    public AbstractCharSequenceAssert<?, String> textAtPath(String xpath) {
        try {
            return assertThat((String) this.xpath.evaluate(xpath + "/text()", this.actual, XPathConstants.STRING));
        }
        catch (XPathExpressionException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static List<Node> toList(NodeList nodeList) {
        List<Node> nodes = new ArrayList<Node>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            nodes.add(nodeList.item(i));
        }
        return nodes;
    }

}
