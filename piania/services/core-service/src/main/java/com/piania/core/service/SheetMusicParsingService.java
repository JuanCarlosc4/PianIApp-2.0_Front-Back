package com.piania.core.service;

import com.piania.core.entity.SheetMusic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SheetMusicParsingService {

    public List<Integer> extractExpectedMidiNotes(SheetMusic sheetMusic) {
        List<Integer> notes = new ArrayList<>();

        if (sheetMusic.getMusicXmlUrl() == null || sheetMusic.getMusicXmlUrl().isBlank()) {
            log.warn("Sheet music {} has no musicXmlUrl", sheetMusic.getId());
            return notes;
        }

        try {
            log.info("========== MUSIC XML DEBUG ==========");
            log.info("Sheet music id: {}", sheetMusic.getId());
            log.info("Raw musicXmlUrl: {}", sheetMusic.getMusicXmlUrl());

            Path xmlPath = resolveUploadsPath(sheetMusic.getMusicXmlUrl());
            File xmlFile = xmlPath.toFile();

            log.info("Resolved relative path: {}", xmlPath);
            log.info("Resolved absolute path: {}", xmlPath.toAbsolutePath());
            log.info("Exists: {}", xmlFile.exists());
            log.info("Is file: {}", xmlFile.isFile());
            log.info("Length: {}", xmlFile.exists() ? xmlFile.length() : -1);

            if (!xmlFile.exists() || !xmlFile.isFile()) {
                log.warn("MusicXML file not found for sheet music {} at {}",
                        sheetMusic.getId(), xmlPath.toAbsolutePath());
                return notes;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setValidating(false);

            // Desactivar carga de DTD y entidades externas
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            var builder = factory.newDocumentBuilder();
            builder.setEntityResolver((publicId, systemId) -> new org.xml.sax.InputSource(new java.io.StringReader("")));

            Document document = builder.parse(xmlFile);
            document.getDocumentElement().normalize();
            document.getDocumentElement().normalize();

            log.info("Root element: {}", document.getDocumentElement().getNodeName());

            var noteNodes = document.getElementsByTagName("note");
            log.info("Found {} note nodes", noteNodes.getLength());

            for (int i = 0; i < noteNodes.getLength(); i++) {
                Node node = noteNodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) continue;

                Element noteElement = (Element) node;

                if (hasChild(noteElement, "rest")) {
                    continue;
                }

                Element pitchElement = getDirectChild(noteElement, "pitch");
                if (pitchElement == null) {
                    continue;
                }

                String step = getChildText(pitchElement, "step");
                String octaveText = getChildText(pitchElement, "octave");
                String alterText = getChildText(pitchElement, "alter");

                if (step == null || octaveText == null) {
                    continue;
                }

                int octave = Integer.parseInt(octaveText);
                int alter = alterText != null ? Integer.parseInt(alterText) : 0;

                int midi = toMidi(step, alter, octave);
                notes.add(midi);
            }

            log.info("Extracted {} MIDI notes", notes.size());
            if (!notes.isEmpty()) {
                log.info("First 10 MIDI notes: {}", notes.stream().limit(10).toList());
            }
            log.info("====================================");

            return notes;

        } catch (Exception e) {
            log.error("Error parsing MusicXML for sheet music {}: {}",
                    sheetMusic.getId(), e.getMessage(), e);
            return notes;
        }
    }

    private Path resolveUploadsPath(String fileUrl) {
        String normalized = fileUrl == null ? "" : fileUrl.trim().replace("\\", "/");

        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        if (normalized.startsWith("uploads/")) {
            normalized = normalized.substring("uploads/".length());
        }

        return Paths.get("uploads", normalized);
    }

    private boolean hasChild(Element parent, String tagName) {
        return getDirectChild(parent, tagName) != null;
    }

    private Element getDirectChild(Element parent, String tagName) {
        for (int i = 0; i < parent.getChildNodes().getLength(); i++) {
            Node child = parent.getChildNodes().item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && tagName.equals(child.getNodeName())) {
                return (Element) child;
            }
        }
        return null;
    }

    private String getChildText(Element parent, String tagName) {
        Element child = getDirectChild(parent, tagName);
        return child != null ? child.getTextContent() : null;
    }

    private int toMidi(String step, int alter, int octave) {
        int base;
        switch (step.toUpperCase()) {
            case "C": base = 0; break;
            case "D": base = 2; break;
            case "E": base = 4; break;
            case "F": base = 5; break;
            case "G": base = 7; break;
            case "A": base = 9; break;
            case "B": base = 11; break;
            default: throw new IllegalArgumentException("Unknown step: " + step);
        }

        return (octave + 1) * 12 + base + alter;
    }
}
