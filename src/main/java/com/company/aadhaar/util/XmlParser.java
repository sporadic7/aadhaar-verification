package com.company.aadhaar.util;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

public class XmlParser {

    /**
     * Parsed subset for compliance: Name, DOB, Address, Aadhaar last4.
     */
    public record ParsedAadhaarDetails(String fullName, String dob, String address, String aadhaarLast4) {
    }

    public static ParsedAadhaarDetails parseAadhaarXml(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new com.company.aadhaar.exception.DigiLockerException("XmlParser: XML is blank");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            String name = firstByAnyTag(doc, "Name");
            String dob = firstByAnyTag(doc, "Dob");
            String aadhaarNumber = firstByAnyTag(doc, "AadhaarNumber");

            String last4 = null;
            if (aadhaarNumber != null && aadhaarNumber.replaceAll("\\D", "").length() >= 4) {
                String digits = aadhaarNumber.replaceAll("\\D", "");
                last4 = digits.substring(digits.length() - 4);
            }

            String house = firstByAnyTag(doc, "House");
            String street = firstByAnyTag(doc, "Street");
            String locality = firstByAnyTag(doc, "Locality");
            String district = firstByAnyTag(doc, "District");
            String state = firstByAnyTag(doc, "State");
            String country = firstByAnyTag(doc, "Country");
            String pin = firstByAnyTag(doc, "Pincode");

            StringBuilder address = new StringBuilder();
            appendIfPresent(address, house);
            appendIfPresent(address, street);
            appendIfPresent(address, locality);
            appendIfPresent(address, district);
            appendIfPresent(address, state);
            appendIfPresent(address, pin);
            appendIfPresent(address, country);

            ParsedAadhaarDetails parsed = new ParsedAadhaarDetails(
                    clean(name),
                    clean(dob),
                    address.length() == 0 ? null : address.toString(),
                    clean(last4)
            );

            validateMandatoryFields(parsed);
            return parsed;
        } catch (com.company.aadhaar.exception.DigiLockerException e) {
            throw e;
        } catch (Exception e) {
            throw new com.company.aadhaar.exception.DigiLockerException("XmlParser: failed to parse Aadhaar XML", e);
        }
    }

    private static void validateMandatoryFields(ParsedAadhaarDetails parsed) {
        if (parsed.fullName() == null || parsed.fullName().isBlank()) {
            throw new com.company.aadhaar.exception.DigiLockerException("XmlParser: missing mandatory field fullName");
        }
        if (parsed.dob() == null || parsed.dob().isBlank()) {
            throw new com.company.aadhaar.exception.DigiLockerException("XmlParser: missing mandatory field dob");
        }
        if (parsed.aadhaarLast4() == null || parsed.aadhaarLast4().isBlank()) {
            throw new com.company.aadhaar.exception.DigiLockerException("XmlParser: missing mandatory field aadhaarLast4");
        }
    }


    private static String firstByAnyTag(Document doc, String tag) {
        NodeList list = doc.getElementsByTagName(tag);
        if (list == null || list.getLength() == 0) return null;
        String txt = list.item(0).getTextContent();
        return clean(txt);
    }

    private static void appendIfPresent(StringBuilder sb, String v) {
        if (v == null || v.isBlank()) return;
        if (sb.length() > 0) sb.append(", ");
        sb.append(v.trim());
    }

    private static String clean(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}


