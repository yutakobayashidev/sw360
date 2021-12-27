/*
 * Copyright TOSHIBA CORPORATION, 2021. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.spdx;

import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.spdx.annotations.*;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.*;
import org.eclipse.sw360.datahandler.thrift.spdx.otherlicensinginformationdetected.*;
import org.eclipse.sw360.datahandler.thrift.spdx.relationshipsbetweenspdxelements.*;
import org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.*;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.*;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.*;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SpdxDocumentContainer;
import org.spdx.rdfparser.model.*;
import org.spdx.rdfparser.model.Checksum.ChecksumAlgorithm;
import org.spdx.rdfparser.model.ExternalRef.ReferenceCategory;
import org.spdx.rdfparser.model.Relationship.RelationshipType;
import org.spdx.rdfparser.model.pointer.*;
import org.spdx.rdfparser.referencetype.ReferenceType;
import org.spdx.tools.SpdxConverter;
import org.spdx.tools.SpdxConverterException;
import org.spdx.tools.TagToRDF;
import org.spdx.tools.RdfToTag;

import org.spdx.tag.CommonCode;
import org.spdx.rdfparser.license.ExtractedLicenseInfo;
import org.spdx.rdfparser.SpdxPackageVerificationCode;
import org.spdx.rdfparser.SpdxRdfConstants;
import org.spdx.rdfparser.license.AnyLicenseInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.io.FileInputStream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.eclipse.sw360.datahandler.couchdb.DatabaseMixInForSPDXDocument.*;
public class SpdxBOMExporter {
    private static final Logger log = LogManager.getLogger(SpdxBOMExporter.class);
    private final SpdxBOMExporterSink sink;
    private Set<ExtractedLicenseInfo> licenses = new HashSet<>();

    public SpdxBOMExporter(SpdxBOMExporterSink sink) {
        this.sink = sink;
    }

    public RequestSummary exportSPDXFile(String releaseId, String outputFormat) throws SW360Exception, MalformedURLException, InvalidSPDXAnalysisException {
        RequestSummary requestSummary = new RequestSummary();
        String verifyMessage = "";
        final String targetFileName = releaseId + "." + outputFormat.toLowerCase();
        log.info("Export to file: " + targetFileName);
        
        if (creteSPDXJsonFomatFromSW360SPDX(releaseId)) {
            if (outputFormat.equals("JSON")) {
                //verifyMessage = convertJSONtoOutputFormat(targetFileName, releaseId + ".RDF");
                requestSummary.setMessage("Export to JSON format successfully !");
                return requestSummary.setRequestStatus(RequestStatus.SUCCESS);
            } else {
                verifyMessage = convertJSONtoOutputFormat(releaseId + ".json", targetFileName);
                if (verifyMessage.isEmpty()) {
                    log.info("Export to " + targetFileName + " sucessfully");
                    requestSummary.setMessage("Export to " + outputFormat + " format successfully !");
                    return requestSummary.setRequestStatus(RequestStatus.SUCCESS);
                } else {
                    log.error("Export to " + targetFileName + " error");
                    requestSummary.setMessage(verifyMessage);
                    return requestSummary.setRequestStatus(RequestStatus.FAILURE);
                }
            }
        } else {
            log.error("Export to " + targetFileName + " error !!!");
            requestSummary.setMessage("Export to " + targetFileName + " error !!!");
            return requestSummary.setRequestStatus(RequestStatus.FAILURE);
        }
    }

    private String convertJSONtoOutputFormat(String jsonFileName, String outputFileName ) {
        try {
            log.info("Convert " + jsonFileName + " to " + outputFileName);
            if (outputFileName.split("\\.")[1].equals("spdx")) {
                SpdxConverter.convert(jsonFileName, "tmp.rdf");
                RdfToTag.main(new String[] {"tmp.rdf", outputFileName});
            } else {
                SpdxConverter.convert(jsonFileName, outputFileName);
            }
        } catch (SpdxConverterException e) {
            log.error("Convert to " + outputFileName + " file error !!!");
            e.printStackTrace();
            return e.getMessage();
        }
        return "";
    }

    private boolean creteSPDXJsonFomatFromSW360SPDX(String releaseId) throws SW360Exception {
        final SPDXDocument sw360SPDXDocument = getSpdxDocumentFromRelease(releaseId);
        final Set<PackageInformation> sw360PackageInformations = getPackagesInformationFromSpdxDocument(sw360SPDXDocument.getId());
        final DocumentCreationInformation sw360CreationInfo = getDocCreationInfoFromSpdxDocument(sw360SPDXDocument.getId());

        // creating JSONObject
        JSONObject SPDXJson = new JSONObject();
        Map<String, String> m = new LinkedHashMap<>();
        JSONParser parser = new JSONParser();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.addMixInAnnotations(Annotations.class, AnnotationsMixin.class);
        objectMapper.addMixInAnnotations(CheckSum.class, CheckSumMixin.class);
        objectMapper.addMixInAnnotations(ExternalReference.class, ExternalReferenceMixin.class);
        objectMapper.addMixInAnnotations(PackageInformation.class, PackageInformationMixin.class);
        objectMapper.addMixInAnnotations(ExternalDocumentReferences.class, ExternalDocumentReferencesMixin.class);
        objectMapper.addMixInAnnotations(SnippetInformation.class, SnippetInformationMixin.class);
        objectMapper.addMixInAnnotations(SnippetRange.class, SnippetRangeMixin.class);
        objectMapper.addMixInAnnotations(RelationshipsBetweenSPDXElements.class, RelationshipsBetweenSPDXElementsMixin.class);
        objectMapper.addMixInAnnotations(OtherLicensingInformationDetected.class, OtherLicensingInformationDetectedMixin.class);
        objectMapper.addMixInAnnotations(PackageVerificationCode.class, PackageVerificationCodeMixin.class);


        try {
            // put package infomation to SPDX json
            JSONArray SPDXPackageInfo = new JSONArray();
            JSONArray SDPXRelationships = (JSONArray) parser.parse(objectMapper.writeValueAsString(sw360SPDXDocument.getRelationships()));
            for (PackageInformation sw360PackageInfo : sw360PackageInformations) {
                log.info("Export Package Infomation: " +sw360PackageInfo.getName());
                JSONObject SW360SPDXPackageInfo = (JSONObject) parser.parse(objectMapper.writeValueAsString(sw360PackageInfo));

                if (sw360PackageInfo.getPackageVerificationCode() != null) {
                    JSONObject packageVerificationCode = new JSONObject();
                    JSONObject sw360packageVerificationCode = (JSONObject) parser.parse(objectMapper.writeValueAsString(sw360PackageInfo.getPackageVerificationCode()));
                    packageVerificationCode.put("packageVerificationCodeExcludedFiles", sw360packageVerificationCode.get("excludedFiles"));
                    packageVerificationCode.put("packageVerificationCodeValue", sw360packageVerificationCode.get("value"));
                    SW360SPDXPackageInfo.remove("packageVerificationCode");
                    SW360SPDXPackageInfo.put("packageVerificationCode", packageVerificationCode);
                }

                if (!sw360PackageInfo.getRelationships().isEmpty()) {
                    for (RelationshipsBetweenSPDXElements relationship : sw360PackageInfo.getRelationships()) {
                        JSONObject packageReleationship = (JSONObject) parser.parse(objectMapper.writeValueAsString(relationship));
                        SDPXRelationships.add(packageReleationship);
                    }
                }

                SW360SPDXPackageInfo.remove("relationships");
                SPDXPackageInfo.add(SW360SPDXPackageInfo);
            }
            SPDXJson.put("packages", SPDXPackageInfo);

            // put document creation infomation to SPDX json
            JSONObject SW360SPDXCreationInfo = (JSONObject) parser.parse(objectMapper.writeValueAsString(sw360CreationInfo));
            Set<String> keys = new HashSet<>(Arrays.asList("spdxVersion", "dataLicense", "SPDXID", "name", "documentNamespace", "externalDocumentRefs",
            "documentComment"));

            for (String key : keys) {
                if (key.equals("documentNamespace")) {
                    String documentNamespace =  SW360SPDXCreationInfo.get(key).toString();
                    if (documentNamespace.substring(documentNamespace.length() - 1).equals("#")) {
                        SPDXJson.put(key, documentNamespace.substring(0, documentNamespace.length() - 1));
                    } else {
                        SPDXJson.put(key, documentNamespace);
                    }
                } else {
                    SPDXJson.put(key, SW360SPDXCreationInfo.get(key));
                }
            }

            JSONObject creationInfo = new JSONObject();
            creationInfo.put("comment", SW360SPDXCreationInfo.get("creatorComment"));
            creationInfo.put("created", SW360SPDXCreationInfo.get("created"));
            creationInfo.put("licenseListVersion", SW360SPDXCreationInfo.get("licenseListVersion"));

            JSONArray SW360SPDXCreationInfoCreator = (JSONArray) parser.parse(objectMapper.writeValueAsString(sw360CreationInfo.getCreator()));
            JSONArray creators = new JSONArray();
            SW360SPDXCreationInfoCreator.forEach(c -> {
                JSONObject obj = (JSONObject) c;
                String type = (String) obj.get("type");
                String value = (String) obj.get("value");
                creators.add(type + ": " +value);
            });
            creationInfo.put("creators", creators);
            SPDXJson.put("creationInfo", creationInfo);


            // put spdx document to SPDX json
            JSONArray files = new JSONArray();
            for (SnippetInformation snippet : sw360SPDXDocument.getSnippets()) {
                if (! snippet.getSnippetFromFile().isEmpty()) {
                    JSONObject file = new JSONObject();
                    file.put("SPDXID", snippet.getSnippetFromFile());
                    file.put("fileName", snippet.getSnippetFromFile());
                    file.put("comment", "File information is generated from snippet and only SPDXID is correct information");
                    files.add(file);
                }
            }
            SPDXJson.put("files", files);

            JSONArray snippets = (JSONArray) parser.parse(objectMapper.writeValueAsString(sw360SPDXDocument.getSnippets()));
            snippets.forEach(s -> {
                JSONObject snippet = (JSONObject) s;
                JSONArray ranges = new JSONArray();

                JSONArray snippetRanges = (JSONArray) snippet.get("snippetRanges");
                snippetRanges.forEach(r -> {
                    JSONObject rangeElement = (JSONObject) r;
                    JSONObject range = new JSONObject();
                    if (rangeElement.get("rangeType").equals("LINE")) {
                        JSONObject startPointer = new JSONObject();
                        startPointer.put("lineNumber", rangeElement.get("startPointer"));
                        startPointer.put("reference", rangeElement.get("reference"));
                        range.put("startPointer", startPointer);

                        JSONObject endPointer = new JSONObject();
                        endPointer.put("lineNumber", rangeElement.get("endPointer"));
                        endPointer.put("reference", rangeElement.get("reference"));
                        range.put("endPointer", endPointer);
                    } else {
                        JSONObject startPointer = new JSONObject();
                        startPointer.put("offset", rangeElement.get("startPointer"));
                        startPointer.put("reference", rangeElement.get("reference"));
                        range.put("startPointer", startPointer);

                        JSONObject endPointer = new JSONObject();
                        endPointer.put("offset", rangeElement.get("endPointer"));
                        endPointer.put("reference", rangeElement.get("reference"));
                        range.put("endPointer", endPointer);
                    }
                    ranges.add(range);
                });
                snippet.remove("snippetRanges");
                snippet.put("ranges", ranges);
            });
            SPDXJson.put("snippets", snippets);

            SPDXJson.put("relationships", SDPXRelationships);
            SPDXJson.put("annotations", (JSONArray) parser.parse(objectMapper.writeValueAsString(sw360SPDXDocument.getAnnotations())));
            SPDXJson.put("hasExtractedLicensingInfos", (JSONArray) parser.parse(objectMapper.writeValueAsString(sw360SPDXDocument.getOtherLicensingInformationDetecteds())));

            PrintWriter pw = new PrintWriter(releaseId + ".json");
            pw.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(SPDXJson));
            pw.flush();
            pw.close();
        } catch (ParseException | JsonProcessingException | FileNotFoundException e) {
            log.error("Can not convert SW360 SPDX Document to Json");
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private File convertSpdxDocumentToTagFile(SpdxDocument doc, String tagFileName) {
        File spdxTagFile = new File(tagFileName);
        PrintWriter out = null;
        List<String> verify = new LinkedList<String>();

        try {
            try {
                out = new PrintWriter(spdxTagFile, "UTF-8");
            } catch (IOException e1) {
                log.error("Could not write to the new SPDX Tag file "+ spdxTagFile.getPath() + "due to error " + e1.getMessage());
            }
            try {
                verify = doc.verify();
                if (!verify.isEmpty()) {
                    log.warn("This SPDX Document is not valid due to:");
                    for (int i = 0; i < verify.size(); i++) {
                        log.warn("\t" + verify.get(i));
                    }
                }

                Properties constants = CommonCode.getTextFromProperties("org/spdx/tag/SpdxTagValueConstants.properties");
                CommonCode.printDoc(doc, out, constants);
            } catch (Exception e) {
                log.error("Error transalting SPDX Document to tag-value format: " + e.getMessage());
            }
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }

        return spdxTagFile;
    }

    private void convertTagToRdf(File spdxTagFile, String targetFileName) {
        FileInputStream spdxTagStream = null;

        try {
            spdxTagStream = new FileInputStream(spdxTagFile);
        } catch (FileNotFoundException e2) {
            e2.printStackTrace();
        }

        File spdxRDFFile = new File(targetFileName);
        String outputFormat = "RDF/XML";
		FileOutputStream outStream = null;
		try {
			outStream = new FileOutputStream(spdxRDFFile);
		} catch (FileNotFoundException e1) {
			try {
				spdxTagStream.close();
			} catch (IOException e) {
                log.error("Warning: Unable to close input file on error.");
			}
			log.error("Could not write to the new SPDX RDF file "+ spdxRDFFile.getPath() + "due to error " + e1.getMessage());
        }

		List<String> warnings = new ArrayList<String>();
		try {
			TagToRDF.convertTagFileToRdf(spdxTagStream, outStream, outputFormat, warnings);
			if (!warnings.isEmpty()) {
				log.warn("The following warnings and or verification errors were found:");
				for (String warning:warnings) {
					log.warn("\t"+warning);
				}
            }
		} catch (Exception e) {
			log.error("Error creating SPDX Analysis: " + e.getMessage());
		} finally {
			if (outStream != null) {
				try {
					outStream.close();
				} catch (IOException e) {
					log.error("Error closing RDF file: " + e.getMessage());
				}
			}
			if (spdxTagStream != null) {
				try {
					spdxTagStream.close();
				} catch (IOException e) {
					log.error("Error closing Tag/Value file: " + e.getMessage());
				}
			}
		}
    }

    private SpdxDocument createSpdxDocumentFromSw360Spdx(String releaseId) throws SW360Exception, InvalidSPDXAnalysisException {
        final SPDXDocument sw360SPDXDocument = getSpdxDocumentFromRelease(releaseId);

        SpdxDocument spdxDocument = createSpdxDocumentCreationInfoFromSw360DocumentCreationInfo(sw360SPDXDocument.getId(), sw360SPDXDocument);

        final Set<SnippetInformation> snippetInfos = sw360SPDXDocument.getSnippets();
        final Set<RelationshipsBetweenSPDXElements> relationships = sw360SPDXDocument.getRelationships();
        final Set<Annotations> annotations = sw360SPDXDocument.getAnnotations();


        final SpdxSnippet[] spdxSnippets = createSpdxSnippetsFromSw360Snippets(snippetInfos);
        final Relationship[] spdxRelationships = createSpdxRelationshipsFromSw360Relationships(relationships, sw360SPDXDocument.getId());
        final Annotation[] spdxAnnotations = createSpdxAnnotationsFromSw360Annotations(annotations);

        try{

            for (SpdxSnippet spdxSnippet : spdxSnippets) {
                spdxDocument.getDocumentContainer().addElement(spdxSnippet);
            }
            spdxDocument.setAnnotations(spdxAnnotations);
            spdxDocument.setRelationships(spdxRelationships);
        } catch (Exception e) {
            log.error("Error setSPDXDocument: " +e);
        }

        return spdxDocument;
    }

    private SpdxSnippet[] createSpdxSnippetsFromSw360Snippets (Set<SnippetInformation> sw360SnippetInfos) throws InvalidSPDXAnalysisException {
        List<SpdxSnippet> spdxSnippets = new ArrayList<>();
        for (SnippetInformation sw360SnippetInfo : sw360SnippetInfos) {
            SpdxSnippet snippet = new SpdxSnippet("snippetName", null, null, null, null, null, null, null, null, null, null); // name is updated at line 270

            String spdxId = sw360SnippetInfo.getSPDXID();
            snippet.setId(spdxId);

            String spdxSnippetFromFileName = sw360SnippetInfo.getSnippetFromFile();
            SpdxFile spdxSnippetFromFile = new SpdxFile(spdxSnippetFromFileName, null, null, null, null, null, SpdxRdfConstants.NOASSERTION_VALUE,
            null, null, null, null, null, null);
            snippet.setSnippetFromFile(spdxSnippetFromFile);

            for (SnippetRange range : sw360SnippetInfo.getSnippetRanges()) {
                if (range.getRangeType().equals("BYTE")) {
                    StartEndPointer spdxByteRange;
                    Integer OFFSET1_1 = Integer.parseInt(range.getStartPointer());
                    Integer OFFSET1_2 = Integer.parseInt(range.getEndPointer());
                    ByteOffsetPointer BOP_POINTER1_1 = new ByteOffsetPointer(spdxSnippetFromFile, OFFSET1_1);
                    ByteOffsetPointer BOP_POINTER1_2 = new ByteOffsetPointer(spdxSnippetFromFile, OFFSET1_2);
                    spdxByteRange = new StartEndPointer(BOP_POINTER1_1, BOP_POINTER1_2);
                    snippet.setByteRange(spdxByteRange);
                } else {
                    StartEndPointer spdxLineRange;
                    Integer LINE1_1 = Integer.parseInt(range.getStartPointer());
                    Integer LINE1_2 = Integer.parseInt(range.getEndPointer());
                    LineCharPointer LCP_POINTER1_1 = new LineCharPointer(spdxSnippetFromFile, LINE1_1);
                    LineCharPointer LCP_POINTER1_2 = new LineCharPointer(spdxSnippetFromFile, LINE1_2);
                    spdxLineRange = new StartEndPointer(LCP_POINTER1_1, LCP_POINTER1_2);
                    snippet.setLineRange(spdxLineRange);
                }
            }

            ExtractedLicenseInfo spdxlicenseConcluded = new ExtractedLicenseInfo(sw360SnippetInfo.getLicenseConcluded(), "");
            snippet.setLicenseConcluded(existedLicense(spdxlicenseConcluded));

            List<AnyLicenseInfo> spdxLicenseInfoFromFiles = new ArrayList<>();
            for (String sw360LicenseInfoInSnippet : sw360SnippetInfo.getLicenseInfoInSnippets()) {
                ExtractedLicenseInfo license = new ExtractedLicenseInfo(sw360LicenseInfoInSnippet, "");
                spdxLicenseInfoFromFiles.add(existedLicense(license));
            }
            snippet.setLicenseInfosFromFiles(spdxLicenseInfoFromFiles.toArray(AnyLicenseInfo[]::new));

            String spdxLicenseComment = sw360SnippetInfo.getLicenseComments();
            snippet.setLicenseComment(spdxLicenseComment);

            String copyrightText = sw360SnippetInfo.getCopyrightText();
            snippet.setCopyrightText(copyrightText);

            String comment = sw360SnippetInfo.getComment();
            snippet.setComment(comment);

            String name= sw360SnippetInfo.getName();
            snippet.setName(name);

            String[] attributionText= sw360SnippetInfo.getSnippetAttributionText().split("|");
            snippet.setAttributionText(attributionText);

            spdxSnippets.add(snippet);
        }
        return spdxSnippets.toArray(SpdxSnippet[]::new);
    }


    private Relationship[] createSpdxRelationshipsFromSw360Relationships(Set<RelationshipsBetweenSPDXElements> sw360Relationships, String SPDXDocId) throws InvalidSPDXAnalysisException {
        List<Relationship> spdxRelationships = new ArrayList<>();
        List<RelationshipsBetweenSPDXElements> list = new ArrayList(sw360Relationships);
        Collections.reverse(list);
        Set<RelationshipsBetweenSPDXElements> resultSet = new LinkedHashSet(list);
        boolean checkIsPackageInfo = false;

        for (RelationshipsBetweenSPDXElements sw360Relationship : resultSet) {
            // todo: setId for relatedSpdxElement
            // relatedSpdxElement.setId(sw360Relationship.getSpdxElementId());

            RelationshipType relationshipType;
            relationshipType = RelationshipType.fromTag(sw360Relationship.getRelationshipType());
            String comment = sw360Relationship.getRelationshipComment();

            if (relationshipType == Relationship.RelationshipType.DESCRIBES && checkIsPackageInfo == false) {
                SpdxPackage relatedSpdxPackage = new SpdxPackage(sw360Relationship.getRelatedSpdxElement(), null,null, null,
                null, null, null, null, null, null,null, null, null, null, null, null, null, null, null, null, null, true, null);
                try {
                    createSpdxPackageInfoFromSw360PackageInfo(relatedSpdxPackage, SPDXDocId);
                } catch (SW360Exception | URISyntaxException e) {
                    e.printStackTrace();
                }
                Relationship relationship = new Relationship(relatedSpdxPackage, relationshipType, comment);
                spdxRelationships.add(relationship);
                checkIsPackageInfo = true;
            } else if (sw360Relationship.getSpdxElementId().equals("SPDXRef-Package")) {
                SpdxPackage relatedSpdxPackage = new SpdxPackage(sw360Relationship.getRelatedSpdxElement(), null,null, null,
                null, null, null, null, null, null,null, null, null, null, null, null, null, null, null, null, null, true, null);
                relatedSpdxPackage.setId(sw360Relationship.getSpdxElementId());
                Relationship relationship = new Relationship(relatedSpdxPackage, relationshipType, comment);
                spdxRelationships.add(relationship);
            } else if (sw360Relationship.getSpdxElementId().equals("SPDXRef-File")) {
                SpdxFile relatedSpdxElement = new SpdxFile(sw360Relationship.getRelatedSpdxElement(), null, null, null, null, null, null,
                null, null, null, null, null, null);
                relatedSpdxElement.setId(sw360Relationship.getSpdxElementId());
                Relationship relationship = new Relationship(relatedSpdxElement, relationshipType, comment);
                spdxRelationships.add(relationship);
            } else {
                SpdxElement relatedSpdxElement = new SpdxElement(sw360Relationship.getRelatedSpdxElement(), null, null, null);
                relatedSpdxElement.setId(sw360Relationship.getSpdxElementId());
                Relationship relationship = new Relationship(relatedSpdxElement, relationshipType, comment);
                spdxRelationships.add(relationship);
            }
        }

        return spdxRelationships.toArray(Relationship[]::new);
    }


    private Annotation[] createSpdxAnnotationsFromSw360Annotations(Set<Annotations> sw360Annotations) throws InvalidSPDXAnalysisException {
        List<Annotation> spdxAnnotations = new ArrayList<>();
        for (Annotations sw360Annotation : sw360Annotations) {
            Annotation annotation = new Annotation(null, null, null, null);
            annotation.setAnnotator(sw360Annotation.getAnnotator());
            annotation.setAnnotationDate(sw360Annotation.getAnnotationDate());
            annotation.setAnnotationType(Annotation.TAG_TO_ANNOTATION_TYPE.get(sw360Annotation.getAnnotationType()));
            annotation.setComment(sw360Annotation.getAnnotationComment());

            spdxAnnotations.add(annotation);
        }

        return spdxAnnotations.toArray(Annotation[]::new);
    }

    private ExtractedLicenseInfo[] createSpdxExtractedLicenseInfoFromSw360ExtractedLicenseInfo(Set<OtherLicensingInformationDetected> sw360OtherLicenses) {
        List<ExtractedLicenseInfo> spdxExtractedLicenseInfo = new ArrayList<>();
        for (OtherLicensingInformationDetected sw360OtherLicense: sw360OtherLicenses) {
            ExtractedLicenseInfo extractedLicenseInfo = new ExtractedLicenseInfo(null, null, null, null, null);
            extractedLicenseInfo.setLicenseId(sw360OtherLicense.getLicenseId());
            extractedLicenseInfo.setExtractedText(sw360OtherLicense.getExtractedText());
            extractedLicenseInfo.setName(sw360OtherLicense.getLicenseName());
            extractedLicenseInfo.setCrossRef(sw360OtherLicense.getLicenseCrossRefs().toArray(String[]::new));
            extractedLicenseInfo.setComment(sw360OtherLicense.getLicenseComment());
            spdxExtractedLicenseInfo.add(existedLicense(extractedLicenseInfo));
        }

        return spdxExtractedLicenseInfo.toArray(ExtractedLicenseInfo[]::new);
    }

    private SpdxDocument createSpdxDocumentCreationInfoFromSw360DocumentCreationInfo(String sw360SpdxDocId, SPDXDocument sw360SPDXDocument) throws SW360Exception, InvalidSPDXAnalysisException {
        DocumentCreationInformation sw360DocumentCreationInformation = getDocCreationInfoFromSpdxDocument(sw360SpdxDocId);

        SpdxDocumentContainer documentContainer = new SpdxDocumentContainer(sw360DocumentCreationInformation.getDocumentNamespace(), sw360DocumentCreationInformation.getSpdxVersion());
        SpdxDocument spdxDocument = documentContainer.getSpdxDocument();

        // set other license firstly because only in this license has text in sw360 spdx
        final Set<OtherLicensingInformationDetected> otherLicenses = sw360SPDXDocument.getOtherLicensingInformationDetecteds();
        final ExtractedLicenseInfo[] extractedLicenseInfos = createSpdxExtractedLicenseInfoFromSw360ExtractedLicenseInfo(otherLicenses);
        spdxDocument.setExtractedLicenseInfos(extractedLicenseInfos);

        spdxDocument.setSpecVersion(sw360DocumentCreationInformation.getSpdxVersion());

        ExtractedLicenseInfo dataLicense = new ExtractedLicenseInfo(sw360DocumentCreationInformation.getDataLicense(), "");
        spdxDocument.setDataLicense(existedLicense(dataLicense));

        // todo: can not set a file ID for an SPDX element already in an RDF Model. You must create a new SPDX File with this ID.
        // spdxDocument.setId(sw360DocumentCreationInformation.getSPDXID());

        spdxDocument.setName(sw360DocumentCreationInformation.getName());

        // documentNamespace set in new SpdxDocumentContainer()

        Set<ExternalDocumentReferences> sw360Refs = sw360DocumentCreationInformation.getExternalDocumentRefs();
        List<ExternalDocumentRef> externalDocumentRefs = new ArrayList<>();
        for (ExternalDocumentReferences sw360Ref : sw360Refs) { 
            ChecksumAlgorithm algorithm = org.spdx.rdfparser.model.Checksum.CHECKSUM_TAG_TO_ALGORITHM.get(sw360Ref.getChecksum().getAlgorithm() +":");
            org.spdx.rdfparser.model.Checksum checksum = new org.spdx.rdfparser.model.Checksum(algorithm, sw360Ref.getChecksum().getChecksumValue());
            ExternalDocumentRef externalDocumentRef = new ExternalDocumentRef(sw360Ref.getSpdxDocument(), checksum, sw360Ref.getExternalDocumentId());

            externalDocumentRefs.add(externalDocumentRef);
        }
        spdxDocument.getDocumentContainer().setExternalDocumentRefs(externalDocumentRefs.toArray(ExternalDocumentRef[]::new));

        spdxDocument.getCreationInfo().setLicenseListVersion(sw360DocumentCreationInformation.getLicenseListVersion());

        List<String> creators = new ArrayList<>();
        for (Creator sw360Creator : sw360DocumentCreationInformation.getCreator()) {
            String creator = sw360Creator.getType() + ": " +sw360Creator.getValue();
            creators.add(creator);
        }
        spdxDocument.getCreationInfo().setCreators(creators.toArray(String[]::new));

        spdxDocument.getCreationInfo().setCreated(sw360DocumentCreationInformation.getCreated());

        spdxDocument.getCreationInfo().setComment(sw360DocumentCreationInformation.getCreatorComment());

        spdxDocument.setComment(sw360DocumentCreationInformation.getDocumentComment());


        return spdxDocument;
    }

    private void createSpdxPackageInfoFromSw360PackageInfo(SpdxPackage spdxPackage, String sw360SpdxDocId) throws SW360Exception, InvalidSPDXAnalysisException, URISyntaxException {
        final PackageInformation sw360PackageInfo = getPackageInformationFromSpdxDocument(sw360SpdxDocId);

        spdxPackage.setId(sw360PackageInfo.getSPDXID());

        spdxPackage.setVersionInfo(sw360PackageInfo.getVersionInfo());

        spdxPackage.setPackageFileName(sw360PackageInfo.getPackageFileName());

        spdxPackage.setSupplier(sw360PackageInfo.getSupplier());

        spdxPackage.setOriginator(sw360PackageInfo.getOriginator());

        spdxPackage.setDownloadLocation(sw360PackageInfo.getDownloadLocation());

        spdxPackage.setFilesAnalyzed(sw360PackageInfo.isFilesAnalyzed());

        SpdxPackageVerificationCode packageVerificationCode = new SpdxPackageVerificationCode(sw360PackageInfo.getPackageVerificationCode().getValue(),
        sw360PackageInfo.getPackageVerificationCode().getExcludedFiles().toArray(String[]::new));
        spdxPackage.setPackageVerificationCode(packageVerificationCode);

        List<org.spdx.rdfparser.model.Checksum> checksums = new ArrayList<>();
        for (CheckSum sw360cCheckSum: sw360PackageInfo.getChecksums()) {
            ChecksumAlgorithm algorithm = org.spdx.rdfparser.model.Checksum.CHECKSUM_TAG_TO_ALGORITHM.get(sw360cCheckSum.getAlgorithm() +":");
            org.spdx.rdfparser.model.Checksum checksum = new org.spdx.rdfparser.model.Checksum(algorithm, sw360cCheckSum.getChecksumValue());
            checksums.add(checksum);
        }
        spdxPackage.setChecksums(checksums.toArray(org.spdx.rdfparser.model.Checksum[]::new));

        spdxPackage.setHomepage(sw360PackageInfo.getHomepage());

        spdxPackage.setSourceInfo(sw360PackageInfo.getSourceInfo());

        ExtractedLicenseInfo licenseConcluded = new ExtractedLicenseInfo(sw360PackageInfo.getLicenseConcluded(), "");
        spdxPackage.setLicenseConcluded(existedLicense(licenseConcluded));

        List<AnyLicenseInfo> licenseInfoFromFiles = new ArrayList<>();
        for (String sw360licenseInfoFromFile : sw360PackageInfo.getLicenseInfoFromFiles()) {
            ExtractedLicenseInfo license = new ExtractedLicenseInfo(sw360licenseInfoFromFile, "");
            licenseInfoFromFiles.add(existedLicense(license));
        }
        spdxPackage.setLicenseInfosFromFiles(licenseInfoFromFiles.toArray(AnyLicenseInfo[]::new));

        ExtractedLicenseInfo licenseDeclared = new ExtractedLicenseInfo(sw360PackageInfo.getLicenseDeclared(), "");
        spdxPackage.setLicenseDeclared(existedLicense(licenseDeclared));

        spdxPackage.setLicenseComment(sw360PackageInfo.getLicenseComments());

        spdxPackage.setCopyrightText(sw360PackageInfo.getCopyrightText());

        spdxPackage.setSummary(sw360PackageInfo.getSummary());

        spdxPackage.setDescription(sw360PackageInfo.getDescription());

        spdxPackage.setComment(sw360PackageInfo.getPackageComment());

        List<ExternalRef> externalRefs = new ArrayList<>();
        for (ExternalReference sw360Ref : sw360PackageInfo.getExternalRefs()) {
            ReferenceCategory referenceCategory = ReferenceCategory.fromTag(sw360Ref.getReferenceCategory());
            URI uri = new URI(sw360Ref.getReferenceType());
            ReferenceType referenceType = new ReferenceType(uri, null, null, null);
            ExternalRef externalRef = new ExternalRef(referenceCategory, referenceType , sw360Ref.getReferenceLocator(), sw360Ref.getComment());
            externalRefs.add(externalRef);
        }
        spdxPackage.setExternalRefs(externalRefs.toArray(ExternalRef[]::new));

        spdxPackage.setAttributionText(sw360PackageInfo.getAttributionText().toArray(String[]::new));

        List<Annotation> annotations = new ArrayList<>();
        for (Annotations sw360Annotation: sw360PackageInfo.getAnnotations()) {
            Annotation annotation = new Annotation(sw360Annotation.getAnnotator(), Annotation.TAG_TO_ANNOTATION_TYPE.get(sw360Annotation.getAnnotationType()), 
            sw360Annotation.getAnnotationDate(), sw360Annotation.getAnnotationComment());
            annotations.add(annotation);
        }

        spdxPackage.setAnnotations(annotations.toArray(Annotation[]::new));
    }

    private SPDXDocument getSpdxDocumentFromRelease(String releaseId) throws SW360Exception {
        SPDXDocument spdxDoc;
        spdxDoc = null;
        final Release release = sink.getRelease(releaseId);
        if (release.isSetSpdxId()) {
            spdxDoc = sink.getSPDXDocument(release.getSpdxId());
        }
        return spdxDoc;
    }

    private DocumentCreationInformation getDocCreationInfoFromSpdxDocument(String spdxDocId) throws SW360Exception {
        DocumentCreationInformation info = null;
        final SPDXDocument spdxDoc = sink.getSPDXDocument(spdxDocId);
        if (spdxDoc.isSetSpdxDocumentCreationInfoId()) {
            info = sink.getDocumentCreationInfo(spdxDoc.getSpdxDocumentCreationInfoId());
        }
        return info;
    }

    private PackageInformation getPackageInformationFromSpdxDocument(String spdxDocId) throws SW360Exception {
        PackageInformation info = null;
        final SPDXDocument spdxDoc = sink.getSPDXDocument(spdxDocId);
        if (spdxDoc.getSpdxPackageInfoIdsSize() > 0) {
            info = sink.getPackageInfo(spdxDoc.getSpdxPackageInfoIds().iterator().next());
        }
        return info;
    }

    private Set<PackageInformation> getPackagesInformationFromSpdxDocument(String spdxDocId) throws SW360Exception {
        Set<PackageInformation> infos = new HashSet<>();
        final SPDXDocument spdxDoc = sink.getSPDXDocument(spdxDocId);
        for (String  packageId : spdxDoc.getSpdxPackageInfoIds()) {
            PackageInformation  info = sink.getPackageInfo(packageId);
            infos.add(info);
        }
        return infos;
    }

    private ExtractedLicenseInfo existedLicense(ExtractedLicenseInfo license) {
        if (!licenses.isEmpty()) {
            for (ExtractedLicenseInfo existedLicense : licenses) {
                if (existedLicense.getLicenseId().equals(license.getLicenseId())) {
                    return existedLicense;
                }
            }
            licenses.add(license);
        } else {
            licenses.add(license);
        }
        return license;
    }
}
