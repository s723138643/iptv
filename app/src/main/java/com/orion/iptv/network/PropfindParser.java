package com.orion.iptv.network;

import android.net.Uri;

import com.orion.iptv.ui.shares.FileNode;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class PropfindParser extends DefaultHandler {
    private final Stack<String> stack;
    private final FileNode parent;
    private StringBuilder builder;
    private Node node;
    List<FileNode> children;

    public PropfindParser(FileNode parent) {
        super();
        stack = new Stack<>();
        this.parent = parent;
    }

    public List<FileNode> parse(String data) {
       children  = new ArrayList<>();
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)), this);
        } catch (ParserConfigurationException | SAXException | IOException ignored) {
        }
        return stack.empty() ? children : null;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);
        stack.push(qName);
        switch (qName) {
            case "D:response":
                assert node == null;
                node = new Node();
                break;
            case "D:href":
            case "D:displayname":
            case "D:getlastmodified":
            case "D:getcontentlength":
                assert builder == null;
                builder = new StringBuilder();
                break;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(uri, localName, qName);
        String start = stack.pop();
        if (start == null || !start.equals(qName)) {
            throw new SAXException("unclosed tag: "+ qName);
        }
        switch (qName) {
            case "D:response":
                children.add(new FileNode(node.name, node.path, node.isFile, parent));
                node = null;
                break;
            case "D:href":
                String path = builder.toString().trim();
                node.path = Uri.decode(path);
                builder = null;
                break;
            case "D:displayname":
                node.name = builder.toString().trim();
                builder = null;
                break;
            case "D:getlastmodified":
                node.lastModified = builder.toString().trim();
                builder = null;
                break;
            case "D:getcontentlength":
                node.length = builder.toString().trim();
                builder = null;
                break;
            case "D:collection":
                node.isFile = false;
                break;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        super.characters(ch, start, length);
        if (builder == null) {
            return;
        }
        builder.append(new String(ch, start, length));
    }

    @Override
    public void endDocument() throws SAXException {
        super.endDocument();
        if (!stack.empty()) {
            throw new SAXException("bad document");
        }
    }

    private static class Node {
        private String name;
        private String path;
        private String lastModified;
        private String length;
        private boolean isFile = true;
    }
}
