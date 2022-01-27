/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
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

import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.spdx.annotations.*;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.*;
import org.eclipse.sw360.datahandler.thrift.spdx.otherlicensinginformationdetected.*;
import org.eclipse.sw360.datahandler.thrift.spdx.relationshipsbetweenspdxelements.*;
import org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.*;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.*;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.*;
import org.spdx.library.model.enumerations.RelationshipType;
import org.spdx.library.model.license.ExtractedLicenseInfo;
import org.spdx.library.model.pointer.ByteOffsetPointer;
import org.spdx.library.model.pointer.LineCharPointer;
import org.spdx.library.model.pointer.SinglePointer;
import org.spdx.library.model.pointer.StartEndPointer;
import org.spdx.library.model.*;
import org.spdx.library.model.SpdxModelFactory;

import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.tools.InvalidFileNameException;
import org.spdx.tools.SpdxToolsHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.sw360.datahandler.common.CommonUtils.isNotNullEmptyOrWhitespace;

public class SpdxBOMImporter {
    private static final Logger log = LogManager.getLogger(SpdxBOMImporter.class);
    private final SpdxBOMImporterSink sink;

    public SpdxBOMImporter(SpdxBOMImporterSink sink) {
        this.sink = sink;
    }

    public ImportBomRequestPreparation prepareImportSpdxBOMAsRelease(File targetFile) {
        final ImportBomRequestPreparation requestPreparation = new ImportBomRequestPreparation();
        final SpdxDocument spdxDocument = openAsSpdx(targetFile);
        if (spdxDocument == null) {
            requestPreparation.setRequestStatus(RequestStatus.FAILURE);
            requestPreparation.setMessage("error-read-file");
            return requestPreparation;
        }

        try {
            final List<SpdxElement> describedPackages = spdxDocument.getDocumentDescribes().stream().collect(Collectors.toList());
            final List<SpdxElement> packages =  describedPackages.stream()
            .filter(item -> item instanceof SpdxPackage)
            .collect(Collectors.toList());

            if (packages.isEmpty()) {
                requestPreparation.setMessage("The provided BOM did not contain any top level packages.");
                requestPreparation.setRequestStatus(RequestStatus.FAILURE);
                return requestPreparation;
            } else if (packages.size() > 1) {
                requestPreparation.setMessage("The provided BOM file contained multiple described top level packages. This is not allowed here.");
                requestPreparation.setRequestStatus(RequestStatus.FAILURE);
                return requestPreparation;
            }

            final SpdxElement spdxElement = packages.get(0);
            if (spdxElement instanceof SpdxPackage) {
                final SpdxPackage spdxPackage = (SpdxPackage) spdxElement;
                requestPreparation.setName(getValue(spdxPackage.getName()));
                requestPreparation.setVersion(getValue(spdxPackage.getVersionInfo()));
                requestPreparation.setRequestStatus(RequestStatus.SUCCESS);
            } else {
                requestPreparation.setMessage("Failed to get spdx package from the provided BOM file.");
                requestPreparation.setRequestStatus(RequestStatus.FAILURE);
            }
        } catch (InvalidSPDXAnalysisException e) {
            requestPreparation.setRequestStatus(RequestStatus.FAILURE);
            e.printStackTrace();
        }

        return requestPreparation;
    }

    public RequestSummary importSpdxBOMAsRelease(File file, AttachmentContent attachmentContent, String newReleaseVersion, String releaseId)
            throws SW360Exception {
        return importSpdxBOM(file, attachmentContent, SW360Constants.TYPE_RELEASE, newReleaseVersion, releaseId);
    }

    private SpdxDocument openAsSpdx(File file){
        try {
            log.info("Read file: " + file.getName());
            return SpdxToolsHelper.deserializeDocument(file);
        } catch (InvalidSPDXAnalysisException | IOException | InvalidFileNameException e) {
            log.error("Error read file " + file.getName() + " to SpdxDocument");
            e.printStackTrace();
            return null;
        }
    }

    public RequestSummary importSpdxBOMAsProject(File file, AttachmentContent attachmentContent)
            throws SW360Exception {
        return importSpdxBOM(file, attachmentContent, SW360Constants.TYPE_PROJECT);
    }

    private RequestSummary importSpdxBOM(File file, AttachmentContent attachmentContent, String type)
            throws SW360Exception {
        return importSpdxBOM(file, attachmentContent, type, null, null);
    }

    private RequestSummary importSpdxBOM(File file, AttachmentContent attachmentContent, String type, String newReleaseVersion, String releaseId)
            throws SW360Exception {
        final RequestSummary requestSummary = new RequestSummary();
        List<SpdxElement> describedPackages = new ArrayList<>();
        try {
            SpdxDocument spdxDocument = openAsSpdx(file);
            describedPackages = spdxDocument.getDocumentDescribes().stream().collect(Collectors.toList());
            List<SpdxElement> packages = describedPackages.stream()
            .filter(item -> item instanceof SpdxPackage)
            .collect(Collectors.toList());

            if (packages.size() == 0) {
                requestSummary.setTotalAffectedElements(0);
                requestSummary.setTotalElements(0);
                requestSummary.setMessage("The provided BOM did not contain any top level packages.");
                requestSummary.setRequestStatus(RequestStatus.FAILURE);
                return requestSummary;
            } else if (packages.size() > 1) {
                requestSummary.setTotalAffectedElements(0);
                requestSummary.setTotalElements(0);
                requestSummary.setMessage("The provided BOM file contained multiple described top level packages. This is not allowed here.");
                requestSummary.setRequestStatus(RequestStatus.FAILURE);
                return requestSummary;
            }

            final SpdxPackage spdxElement = (SpdxPackage) packages.get(0);
            final Optional<SpdxBOMImporterSink.Response> response;
            if (SW360Constants.TYPE_PROJECT.equals(type)) {
                response = importAsProject(spdxElement, attachmentContent);
            } else if (SW360Constants.TYPE_RELEASE.equals(type)) {
                response = importAsRelease(spdxElement, attachmentContent, spdxDocument, newReleaseVersion, releaseId);
            } else {
                throw new SW360Exception("Unsupported type=[" + type + "], can not import BOM");
            }

            if (response.isPresent()) {
                requestSummary.setRequestStatus(RequestStatus.SUCCESS);
                requestSummary.setTotalAffectedElements(response.get().countAffected());
                requestSummary.setTotalElements(response.get().count());
                requestSummary.setMessage(response.get().getId());
            } else {
                requestSummary.setRequestStatus(RequestStatus.FAILURE);
                requestSummary.setTotalAffectedElements(-1);
                requestSummary.setTotalElements(-1);
                requestSummary.setMessage("Failed to import the BOM as type=[" + type + "].");
            }
        } catch (InvalidSPDXAnalysisException | NullPointerException e) {
            log.error("Can not open file to SpdxDocument " +e);
        }

        return requestSummary;
    }

    private Component createComponentFromSpdxPackage(SpdxPackage spdxPackage) throws InvalidSPDXAnalysisException {
        final Component component = new Component();
        String name = "";
        name = getValue(spdxPackage.getName());
        component.setName(name);
        return component;
    }

    private SpdxBOMImporterSink.Response importAsComponent(SpdxPackage spdxPackage) throws SW360Exception, InvalidSPDXAnalysisException {
        final Component component = createComponentFromSpdxPackage(spdxPackage);
        return sink.addComponent(component);
    }

    private Release createReleaseFromSpdxPackage(SpdxPackage spdxPackage) throws InvalidSPDXAnalysisException {
        final Release release = new Release();
        final String name = getValue(spdxPackage.getName());
        final String version = getValue(spdxPackage.getVersionInfo());
        release.setName(name);
        release.setVersion(version);
        return release;
    }

    private SPDXDocument createSPDXDocumentFromSpdxDocument(String releaseId, SpdxDocument spdxDocument) throws SW360Exception, MalformedURLException {
        final SPDXDocument doc = getSpdxDocumentFromRelease(releaseId);
        doc.setReleaseId(releaseId);
        try {
			Stream<SpdxSnippet> snippetStream = (Stream<SpdxSnippet>)SpdxModelFactory.getElements(spdxDocument.getModelStore(), spdxDocument.getDocumentUri(), null, SpdxSnippet.class);
            List<SpdxSnippet> spdxSnippets = snippetStream.collect(Collectors.toList());
            final List<Relationship> spdxRelationships = spdxDocument.getRelationships().stream().collect(Collectors.toList());
            final List<Annotation> spdxAnnotations = List.copyOf(spdxDocument.getAnnotations());
            final List<ExtractedLicenseInfo> extractedLicenseInfos = List.copyOf(spdxDocument.getExtractedLicenseInfos());

            final Set<SnippetInformation> snippetInfos = createSnippetsFromSpdxSnippets(spdxSnippets);
            final Set<RelationshipsBetweenSPDXElements> relationships = createRelationshipsFromSpdxRelationships(spdxRelationships, spdxDocument.getId());
            final Set<Annotations> annotations = createAnnotationsFromSpdxAnnotations(spdxAnnotations);
            final Set<OtherLicensingInformationDetected> otherLicenses = createOtherLicensesFromSpdxExtractedLicenses(extractedLicenseInfos);

            doc.setSnippets(snippetInfos)
                .setRelationships(relationships)
                .setAnnotations(annotations)
                .setOtherLicensingInformationDetecteds(otherLicenses);
        } catch (InvalidSPDXAnalysisException e) {
            log.error(e);
        }

        return doc;
    }

    private Set<Annotations> createAnnotationsFromSpdxAnnotations(List<Annotation> spdxAnnotations) throws InvalidSPDXAnalysisException {
        Set<Annotations> annotations = new HashSet<>();
        int index = 0;

        for(Annotation spdxAnn : spdxAnnotations) {
            String annotator = spdxAnn.getAnnotator();
            String date = spdxAnn.getAnnotationDate();
            String type = spdxAnn.getAnnotationType().name();
            String comment = spdxAnn.getComment();

            Annotations ann = new Annotations();
            ann.setAnnotator(verifyOrSetDefault(annotator))
                .setAnnotationDate(verifyOrSetDefault(date))
                .setAnnotationType(verifyOrSetDefault(type))
                .setAnnotationComment(verifyOrSetDefault(comment))
                .setIndex(index);

            annotations.add(ann);
            index++;
        }

        return annotations;
    }

    private Set<SnippetInformation> createSnippetsFromSpdxSnippets(List<SpdxSnippet> spdxSnippets) {
        Set<SnippetInformation> snippets = new HashSet<>();
        int index = 0;

        try {
            for (SpdxSnippet spdxSnippet : spdxSnippets) {
                String id = spdxSnippet.getId();
                String snippetFromFile = spdxSnippet.getSnippetFromFile().getId();
                Set<SnippetRange> ranges = createSnippetRangesFromSpdxSnippet(spdxSnippet);
                String licenseConcluded = spdxSnippet.getLicenseConcluded().toString();
                Set<String> licenseInfoInFile = spdxSnippet.getLicenseInfoFromFiles().stream()
                                            .map(license -> verifyOrSetDefault(license.getId()))
                                            .collect(Collectors.toSet());
                String licenseComment = getValue(spdxSnippet.getLicenseComments());
                String copyrightText = spdxSnippet.getCopyrightText();
                String comment = getValue(spdxSnippet.getComment());
                String name = getValue(spdxSnippet.getName());
                String attributionText = String.join("|", spdxSnippet.getAttributionText());

                SnippetInformation snippet = new SnippetInformation();
                snippet.setSPDXID(verifyOrSetDefault(id))
                        .setSnippetFromFile(verifyOrSetDefault(snippetFromFile))
                        .setSnippetRanges(ranges)
                        .setLicenseConcluded(verifyOrSetDefault(licenseConcluded))
                        .setLicenseInfoInSnippets(licenseInfoInFile)
                        .setLicenseComments(verifyOrSetDefault(licenseComment))
                        .setCopyrightText(verifyOrSetDefault(copyrightText))
                        .setComment(verifyOrSetDefault(comment))
                        .setName(verifyOrSetDefault(name))
                        .setSnippetAttributionText(verifyOrSetDefault(attributionText))
                        .setIndex(index);

                snippets.add(snippet);
                index++;
            }
        } catch (InvalidSPDXAnalysisException e) {
            log.error(e);
        }

        return snippets;
    }

    private Set<SnippetRange> createSnippetRangesFromSpdxSnippet(SpdxSnippet spdxSnippet) throws InvalidSPDXAnalysisException {
        StartEndPointer spdxByteRange = spdxSnippet.getByteRange();
        String[] byteRanges = rangeToStrs(spdxByteRange);
        SnippetRange snippetByteRange = new SnippetRange();
        snippetByteRange.setRangeType("BYTE")
                .setStartPointer(byteRanges[0])
                .setEndPointer(byteRanges[1])
                .setReference(spdxByteRange.getStartPointer().getReference().getId())
                .setIndex(0);

        StartEndPointer spdxLineRange = spdxSnippet.getLineRange().get();
        String[] lineRanges = rangeToStrs(spdxLineRange);
        SnippetRange snippetLineRange = new SnippetRange();
        snippetLineRange.setRangeType("LINE")
                .setStartPointer(lineRanges[0])
                .setEndPointer(lineRanges[1])
                .setReference(spdxLineRange.getStartPointer().getReference().getId())
                .setIndex(1);

        return new HashSet<>(Arrays.asList(snippetByteRange, snippetLineRange));
    }

    // // refer to rangeToStr function of spdx-tools
    private String[] rangeToStrs(StartEndPointer rangePointer) throws InvalidSPDXAnalysisException {
        SinglePointer startPointer = rangePointer.getStartPointer();
        if (startPointer == null) {
            throw new InvalidSPDXAnalysisException("Missing start pointer");
        }
        SinglePointer endPointer = rangePointer.getEndPointer();
        if (endPointer == null) {
            throw new InvalidSPDXAnalysisException("Missing end pointer");
        }
        String start = null;
        if (startPointer instanceof ByteOffsetPointer) {
            start = String.valueOf(((ByteOffsetPointer)startPointer).getOffset());
        } else if (startPointer instanceof LineCharPointer) {
            start = String.valueOf(((LineCharPointer)startPointer).getLineNumber());
        } else {
            log.error("Unknown pointer type for start pointer "+startPointer.toString());
            throw new InvalidSPDXAnalysisException("Unknown pointer type for start pointer");
        }
        String end = null;
        if (endPointer instanceof ByteOffsetPointer) {
            end = String.valueOf(((ByteOffsetPointer)endPointer).getOffset());
        } else if (endPointer instanceof LineCharPointer) {
            end = String.valueOf(((LineCharPointer)endPointer).getLineNumber());
        } else {
            log.error("Unknown pointer type for start pointer "+startPointer.toString());
            throw new InvalidSPDXAnalysisException("Unknown pointer type for start pointer");
        }
        return new String[] { start, end };
    }

    private Set<RelationshipsBetweenSPDXElements> createRelationshipsFromSpdxRelationships(List<Relationship> spdxRelationships, String spdxElementId) throws InvalidSPDXAnalysisException {
        Set<RelationshipsBetweenSPDXElements> relationships = new HashSet<>();
        int index = 0;

        for (Relationship spdxRelationship : spdxRelationships) {
            if (!(spdxRelationship.getRelatedSpdxElement().get() instanceof SpdxFile)) {
                String type = spdxRelationship.getRelationshipType().name();
                String relatedSpdxElement = spdxRelationship.getRelatedSpdxElement().get().getId();
                String comment = getValue(spdxRelationship.getComment());

                RelationshipsBetweenSPDXElements relationship = new RelationshipsBetweenSPDXElements();
                relationship.setSpdxElementId(verifyOrSetDefault(spdxElementId))
                            .setRelationshipType(verifyOrSetDefault(type))
                            .setRelatedSpdxElement(verifyOrSetDefault(relatedSpdxElement))
                            .setRelationshipComment(verifyOrSetDefault(comment))
                            .setIndex(index);

                relationships.add(relationship);
                index++;
            }
        }

        return relationships;
    }

    private Set<OtherLicensingInformationDetected> createOtherLicensesFromSpdxExtractedLicenses(List<ExtractedLicenseInfo> spdxExtractedLicenses) throws InvalidSPDXAnalysisException {
        Set<OtherLicensingInformationDetected> otherLicenses = new HashSet<>();
        int index = 0;

        for (ExtractedLicenseInfo spdxExtractedLicense : spdxExtractedLicenses) {
            String licenseId = spdxExtractedLicense.getLicenseId();
            String extractedText = spdxExtractedLicense.getExtractedText();
            String name = spdxExtractedLicense.getName();
            Set<String> crossRef = new HashSet<>(Arrays.asList(verifyOrSetDefault(spdxExtractedLicense.getCrossRef().toArray(new String[spdxExtractedLicense.getCrossRef().size()]))));
            String comment = spdxExtractedLicense.getComment();

            OtherLicensingInformationDetected otherLicense = new OtherLicensingInformationDetected();
            otherLicense.setLicenseId(verifyOrSetDefault(licenseId))
                        .setExtractedText(verifyOrSetDefault(extractedText))
                        .setLicenseName(verifyOrSetDefault(name))
                        .setLicenseCrossRefs(crossRef)
                        .setLicenseComment(verifyOrSetDefault(comment))
                        .setIndex(index);

            otherLicenses.add(otherLicense);
            index++;
        }

        return otherLicenses;
    }

    private DocumentCreationInformation createDocumentCreationInfoFromSpdxDocument(String spdxDocId, SpdxDocument spdxDocument) throws SW360Exception, MalformedURLException {
        final DocumentCreationInformation info = getDocCreationInfoFromSpdxDocument(spdxDocId);
        info.setSpdxDocumentId(spdxDocId);

        try {
            final String spdxVersion = spdxDocument.getSpecVersion();
            final String dataLicense = spdxDocument.getDataLicense().toString();
            final String spdxId = spdxDocument.getId();
            final String name = getValue(spdxDocument.getName());
            final String documentNamespace = spdxDocument.getDocumentUri();
            final Set<ExternalDocumentReferences> refs = createExternalDocumentRefsFromSpdxDocument(spdxDocument);
            final String licenseListVersion = getValue(spdxDocument.getCreationInfo().getLicenseListVersion());
            final Set<Creator> creators = createCreatorFromSpdxDocument(spdxDocument);
            final String createdDate = spdxDocument.getCreationInfo().getCreated();
            final String creatorComment = getValue(spdxDocument.getCreationInfo().getComment());
            final String documentComment = getValue(spdxDocument.getComment());

            info.setSpdxVersion(verifyOrSetDefault(spdxVersion))
                .setDataLicense(verifyOrSetDefault(dataLicense))
                .setSPDXID(verifyOrSetDefault(spdxId))
                .setName(verifyOrSetDefault(name))
                .setDocumentNamespace(verifyOrSetDefault(documentNamespace))
                .setExternalDocumentRefs(refs)
                .setLicenseListVersion(verifyOrSetDefault(licenseListVersion))
                .setCreator(creators)
                .setCreated(verifyOrSetDefault(createdDate))
                .setCreatorComment(verifyOrSetDefault(creatorComment))
                .setDocumentComment(verifyOrSetDefault(documentComment));
        } catch (InvalidSPDXAnalysisException e) {
            log.error(e);
        }

        return info;
    }

    private Set<ExternalDocumentReferences> createExternalDocumentRefsFromSpdxDocument(SpdxDocument spdxDocument) {
        Set<ExternalDocumentReferences> refs = new HashSet<>();
        int index = 0;

        try {
            List<ExternalDocumentRef> externalDocumentRefs = List.copyOf(spdxDocument.getExternalDocumentRefs());

            for (ExternalDocumentRef externalDocumentRef : externalDocumentRefs) {
                Checksum spdxChecksum = externalDocumentRef.getChecksum().get();

                String externalDocumentId = externalDocumentRef.getId();
                String spdxDocumentNamespace = externalDocumentRef.getSpdxDocumentNamespace();
                CheckSum checksum = new CheckSum();
                checksum.setAlgorithm(spdxChecksum.getAlgorithm().name())
                        .setChecksumValue(spdxChecksum.getValue());

                ExternalDocumentReferences ref = new ExternalDocumentReferences();
                ref.setExternalDocumentId(verifyOrSetDefault(externalDocumentId))
                    .setChecksum(checksum)
                    .setSpdxDocument(verifyOrSetDefault(spdxDocumentNamespace))
                    .setIndex(index);

                refs.add(ref);
                index++;
            }
        } catch (InvalidSPDXAnalysisException e) {
            log.error(e);
        }

        return refs;
    }

    private Set<Creator> createCreatorFromSpdxDocument(SpdxDocument spdxDocument) {
        Set<Creator> creators = new HashSet<>();
        int index = 0;

        try {
            List<String> spdxCreators = List.copyOf(spdxDocument.getCreationInfo().getCreators());

            for (String spdxCreator : spdxCreators) {
                String[] data = spdxCreator.split(":");
                if (data.length < 2) {
                    log.error("Failed to get SPDX creator from " + spdxCreator + "!");
                    continue;
                }
                String type = data[0].trim();
                String value = spdxCreator.substring(data[0].length()+1).trim();

                Creator creator = new Creator();
                creator.setType(verifyOrSetDefault(type));
                creator.setValue(verifyOrSetDefault(value));
                creator.setIndex(index);

                creators.add(creator);
                index++;
            }
        } catch (InvalidSPDXAnalysisException e) {
            log.error(e);
        }

        return creators;
    }

    private PackageInformation createPackageInfoFromSpdxPackage(String spdxDocId, SpdxPackage spdxPackage) throws SW360Exception, MalformedURLException, InvalidSPDXAnalysisException {
        PackageVerificationCode PVC = new PackageVerificationCode();
        try {
            PVC = createPVCFromSpdxPackage(spdxPackage);
        } catch (NullPointerException e) {
            PVC.setExcludedFiles(Collections.emptySet())
                .setValue(verifyOrSetDefault(""));
            log.error("Can not get PVC " + e);
        }

        Set<CheckSum> checksums = new HashSet<>();
        try {
            checksums = createCheckSumsFromSpdxChecksums(spdxPackage);
        } catch (NullPointerException e) {
            Collections.emptySet();
            log.error("Can not get checksums " + e);
        }

        String licenseDeclared = "";
        try {
            licenseDeclared = createLicenseDeclaredFromSpdxLicenseDeclared(spdxPackage);
        } catch (NullPointerException e) {
            log.error("Can not get licenseDeclared " + e);
        }

        Set<ExternalReference> externalRefs = new HashSet<>();
        try {
            externalRefs = createExternalReferenceFromSpdxPackage(spdxPackage);
        } catch (NullPointerException e) {
            log.error("Can not get externalRefs " + e);
            externalRefs = Collections.emptySet();
        }

        PackageInformation pInfo = getPackageInformationFromSpdxDocument(spdxDocId, spdxPackage.getName().get());
        pInfo.setSpdxDocumentId(spdxDocId);

        try {
            final String name = getValue(spdxPackage.getName());
            final String spdxId = spdxPackage.getId();
            final String versionInfo = getValue(spdxPackage.getVersionInfo());
            final String packageFileName = getValue(spdxPackage.getPackageFileName());
            final String supplier = getValue(spdxPackage.getSupplier());
            final String originator = getValue(spdxPackage.getOriginator());
            final String downloadLocation = getValue(spdxPackage.getDownloadLocation());
            final boolean fileAnalyzed = spdxPackage.isFilesAnalyzed();
            final String homepage = getValue(spdxPackage.getHomepage());
            final String sourceInfo = getValue(spdxPackage.getSourceInfo());
            final String licenseConcluded = spdxPackage.getLicenseConcluded().toString();
            final Set<String> licenseInfosFromFiles = spdxPackage.getLicenseInfoFromFiles().stream()
                                                        .map(license -> license.toString())
                                                        .collect(Collectors.toSet());
            final String licenseComment = getValue(spdxPackage.getLicenseComments());
            final String copyrightText = spdxPackage.getCopyrightText();
            final String summary = getValue(spdxPackage.getSummary());
            final String description = getValue(spdxPackage.getDescription());
            final String comment = getValue(spdxPackage.getComment());
            final Set<String> attributionText = new HashSet<>(Arrays.asList(verifyOrSetDefault(spdxPackage.getAttributionText().toArray(new String [spdxPackage.getAttributionText().size()]))));
            final Set<Annotations> annotations = createAnnotationsFromSpdxAnnotations(List.copyOf(spdxPackage.getAnnotations()));

            pInfo.setName(verifyOrSetDefault(name))
                .setSPDXID(verifyOrSetDefault(spdxId))
                .setVersionInfo(verifyOrSetDefault(versionInfo))
                .setPackageFileName(verifyOrSetDefault(packageFileName))
                .setSupplier(verifyOrSetDefault(supplier))
                .setOriginator(verifyOrSetDefault(originator))
                .setDownloadLocation(verifyOrSetDefault(downloadLocation))
                .setFilesAnalyzed(fileAnalyzed)
                .setPackageVerificationCode(PVC)
                .setChecksums(checksums)
                .setHomepage(verifyOrSetDefault(homepage))
                .setSourceInfo(verifyOrSetDefault(sourceInfo))
                .setLicenseConcluded(verifyOrSetDefault(licenseConcluded))
                .setLicenseInfoFromFiles(licenseInfosFromFiles)
                .setLicenseDeclared(verifyOrSetDefault(licenseDeclared))
                .setLicenseComments(verifyOrSetDefault(licenseComment))
                .setCopyrightText(verifyOrSetDefault(copyrightText))
                .setSummary(verifyOrSetDefault(summary))
                .setDescription(verifyOrSetDefault(description))
                .setPackageComment(verifyOrSetDefault(comment))
                .setExternalRefs(externalRefs)
                .setAttributionText(attributionText)
                .setAnnotations(annotations);
        } catch (InvalidSPDXAnalysisException e) {
            log.error("createPackageInfoFromSpdxPackage error " + e);
        }

        return pInfo;
    }

    private PackageVerificationCode createPVCFromSpdxPackage(SpdxPackage spdxPackage) {
        try {
            PackageVerificationCode PVC = new PackageVerificationCode();
            SpdxPackageVerificationCode spdxPVC = spdxPackage.getPackageVerificationCode().orElse(null);
            Set<String> excludedFileNames = new HashSet<>(Arrays.asList(verifyOrSetDefault(spdxPVC.getExcludedFileNames().toArray(new String [spdxPVC.getExcludedFileNames().size()]))));
            String value = spdxPVC.getValue();

            PVC.setExcludedFiles(excludedFileNames)
                .setValue(verifyOrSetDefault(value));
            return PVC;
        } catch (InvalidSPDXAnalysisException | NullPointerException e) {
            log.error("Error get PVC " + e);
            return null;
        }
    }

    private Set<ExternalReference> createExternalReferenceFromSpdxPackage(SpdxPackage spdxPackage) {
        Set<ExternalReference> refs = new HashSet<>();
        int index = 0;

        try {
            List<ExternalRef> spdxExternalRefs = List.copyOf(spdxPackage.getExternalRefs());
            for (ExternalRef spdxRef : spdxExternalRefs) {
                String category = spdxRef.getReferenceCategory().name();
                String locator = spdxRef.getReferenceLocator();
                String type = spdxRef.getReferenceType().getIndividualURI();
                String comment = getValue(spdxRef.getComment());

                ExternalReference ref = new ExternalReference();
                ref.setReferenceCategory(verifyOrSetDefault(category))
                    .setReferenceLocator(verifyOrSetDefault(locator))
                    .setReferenceType(verifyOrSetDefault(type))
                    .setComment(verifyOrSetDefault(comment))
                    .setIndex(index);

                refs.add(ref);
                index++;
            }
        } catch (InvalidSPDXAnalysisException e) {
            log.error(e);
        }

        return refs;
    }

    private Set<CheckSum> createCheckSumsFromSpdxChecksums(SpdxPackage spdxPackage) {
        Set<CheckSum> checksums = new HashSet<>();
        int index = 0;
        try {
            List<Checksum> spdxChecksums = List.copyOf(spdxPackage.getChecksums());
            for (Checksum spdxChecksum : spdxChecksums) {
                String algorithm = spdxChecksum.getAlgorithm().name();
                String value = spdxChecksum.getValue();
                CheckSum checksum = new CheckSum();
                checksum.setAlgorithm(verifyOrSetDefault(algorithm))
                        .setChecksumValue(verifyOrSetDefault(value))
                        .setIndex(index);
                checksums.add(checksum);
                index++;
            }
        } catch (InvalidSPDXAnalysisException | NullPointerException e) {
            checksums = Collections.emptySet();
        }
        return checksums;
    }

    private String createLicenseDeclaredFromSpdxLicenseDeclared(SpdxPackage spdxPackage) {
        try {
            return spdxPackage.getLicenseDeclared().toString();
        } catch (InvalidSPDXAnalysisException e) {
            log.error("Can not get licenseDeclared " + e);
        }
        return null;
    }

    private Attachment makeAttachmentFromContent(AttachmentContent attachmentContent) {
        Attachment attachment = new Attachment();
        attachment.setAttachmentContentId(attachmentContent.getId());
        attachment.setAttachmentType(AttachmentType.SBOM);
        attachment.setCreatedComment("Used for SPDX Bom import");
        attachment.setFilename(attachmentContent.getFilename());
        attachment.setCheckStatus(CheckStatus.NOTCHECKED);

        return attachment;
    }

    private Optional<SpdxBOMImporterSink.Response> importAsRelease(SpdxElement relatedSpdxElement) throws SW360Exception, InvalidSPDXAnalysisException {
        return importAsRelease(relatedSpdxElement, null, null, null, null);
    }

    private Optional<SpdxBOMImporterSink.Response> importAsRelease(SpdxElement relatedSpdxElement, AttachmentContent attachmentContent,
            SpdxDocument spdxDocument, String newReleaseVersion, String releaseId) throws SW360Exception, InvalidSPDXAnalysisException {
        if (relatedSpdxElement instanceof SpdxPackage) {
            final SpdxPackage spdxPackage = (SpdxPackage) relatedSpdxElement;

            final Release release;
            SpdxBOMImporterSink.Response component;
            if (isNotNullEmptyOrWhitespace(releaseId)) {
                release = sink.getRelease(releaseId);
                component = new SpdxBOMImporterSink.Response(release.getComponentId(), true);
            } else {
                component = importAsComponent(spdxPackage);
                final String componentId = component.getId();

                release = createReleaseFromSpdxPackage(spdxPackage);
                if (isNotNullEmptyOrWhitespace(newReleaseVersion))
                    release.setVersion(newReleaseVersion);
                release.setComponentId(componentId);
            }

            final Relationship[] relationships = spdxPackage.getRelationships().toArray(new Relationship[spdxPackage.getRelationships().size()]);
            List<SpdxBOMImporterSink.Response> releases = importAsReleases(relationships);
            Map<String, ReleaseRelationship> releaseIdToRelationship = makeReleaseIdToRelationship(releases);
            release.setReleaseIdToRelationship(releaseIdToRelationship);

            if(attachmentContent != null) {
                Attachment attachment = makeAttachmentFromContent(attachmentContent);
                release.setAttachments(Collections.singleton(attachment));
            }

            final SpdxBOMImporterSink.Response response = sink.addRelease(release);

            try {
                importSpdxDocument(response.getId(), spdxDocument, spdxPackage);
            } catch (MalformedURLException e) {
                log.error(e);
            }

            response.addChild(component);
            return Optional.of(response);
        } else {
            log.debug("Unsupported SpdxElement: " + relatedSpdxElement.getClass().getCanonicalName());
            return Optional.empty();
        }
    }

    private void importSpdxDocument(String releaseId, SpdxDocument spdxDocument, SpdxPackage spdxPackage) throws SW360Exception, MalformedURLException, InvalidSPDXAnalysisException {
        final SPDXDocument spdxDoc = createSPDXDocumentFromSpdxDocument(releaseId, spdxDocument);
        final SpdxBOMImporterSink.Response spdxDocRes = sink.addOrUpdateSpdxDocument(spdxDoc);
        final String spdxDocId = spdxDocRes.getId();

        final DocumentCreationInformation docCreationInfo = createDocumentCreationInfoFromSpdxDocument(spdxDocId, spdxDocument);
        sink.addOrUpdateDocumentCreationInformation(docCreationInfo);

        final List<SpdxPackage> allPackages = new ArrayList<>();
        try(@SuppressWarnings("unchecked")
        Stream<SpdxPackage> allPackagesStream = (Stream<SpdxPackage>) SpdxModelFactory.getElements(spdxDocument.getModelStore(), spdxDocument.getDocumentUri(),
        spdxDocument.getCopyManager(), SpdxPackage.class)) {
            allPackagesStream.forEach((SpdxPackage pkg) -> allPackages.add(pkg));
        }

        int index = 1;
        for (SpdxPackage packageElement : allPackages) {
            log.info("Import package: " + packageElement.toString());
            PackageInformation packageInfo = createPackageInfoFromSpdxPackage(spdxDocId, packageElement);
            List<Relationship> packageRelationship = List.copyOf(packageElement.getRelationships());
            if (!packageRelationship.isEmpty()) {
                Set<RelationshipsBetweenSPDXElements> packageReleaseRelationship = createRelationshipsFromSpdxRelationships(packageRelationship, packageElement.getId());
                packageInfo.setRelationships(packageReleaseRelationship);
            } else {
                packageInfo.setRelationships(Collections.emptySet());
            }

            if (packageElement.getName().equals(spdxPackage.getName())) {
                packageInfo.setIndex(0);
            } else {
                packageInfo.setIndex(index);
                index ++;
            }
            sink.addOrUpdatePackageInformation(packageInfo);
        }
    }

    private SPDXDocument getSpdxDocumentFromRelease(String releaseId) throws SW360Exception {
        SPDXDocument spdxDoc;
        final Release release = sink.getRelease(releaseId);
        if (release.isSetSpdxId()) {
            spdxDoc = sink.getSPDXDocument(release.getSpdxId());
        } else {
            spdxDoc = new SPDXDocument();
        }
        return spdxDoc;
    }

    private DocumentCreationInformation getDocCreationInfoFromSpdxDocument(String spdxDocId) throws SW360Exception {
        DocumentCreationInformation info;
        final SPDXDocument spdxDoc = sink.getSPDXDocument(spdxDocId);
        if (spdxDoc.isSetSpdxDocumentCreationInfoId()) {
            info = sink.getDocumentCreationInfo(spdxDoc.getSpdxDocumentCreationInfoId());
        } else {
            info = new DocumentCreationInformation();
        }
        return info;
    }

    private PackageInformation getPackageInformationFromSpdxDocument(String spdxDocId, String packageName) throws SW360Exception {
        PackageInformation info;
        final SPDXDocument spdxDoc = sink.getSPDXDocument(spdxDocId);
        if (spdxDoc.getSpdxPackageInfoIdsSize() > 0) {
            for (String packageId : spdxDoc.getSpdxPackageInfoIds()) {
                if (sink.getPackageInfo(packageId).getName().equals(packageName)) {
                    info = sink.getPackageInfo(packageId);
                    return info;
                }
            }
            return new PackageInformation();
        } else {
            return new PackageInformation();
        }
    }

    private Map<String, ReleaseRelationship> makeReleaseIdToRelationship(List<SpdxBOMImporterSink.Response> releases) {
        return releases.stream()
                .collect(Collectors.toMap(SpdxBOMImporterSink.Response::getId, SpdxBOMImporterSink.Response::getReleaseRelationship));
    }

    private Project creatProjectFromSpdxPackage(SpdxPackage spdxPackage) throws InvalidSPDXAnalysisException {
        Project project = new Project();
        final String name = getValue(spdxPackage.getName());
        final String version = getValue(spdxPackage.getVersionInfo());
        project.setName(name);
        project.setVersion(version);
        return project;
    }

    private List<SpdxBOMImporterSink.Response> importAsReleases(Relationship[] relationships) throws SW360Exception, InvalidSPDXAnalysisException {
        List<SpdxBOMImporterSink.Response> releases = new ArrayList<>();

        Map<RelationshipType, ReleaseRelationship> typeToSupplierMap = new HashMap<>();
        typeToSupplierMap.put(RelationshipType.CONTAINS,  ReleaseRelationship.CONTAINED);

        for (Relationship relationship : relationships) {
            final RelationshipType relationshipType = relationship.getRelationshipType();
            if(! typeToSupplierMap.keySet().contains(relationshipType)) {
                log.debug("Unsupported RelationshipType: " + relationshipType.toString());
                continue;
            }

            final SpdxElement relatedSpdxElement = relationship.getRelatedSpdxElement().get();
            final Optional<SpdxBOMImporterSink.Response> releaseId = importAsRelease(relatedSpdxElement);
            releaseId.map(response -> {
                response.setReleaseRelationship(typeToSupplierMap.get(relationshipType));
                return response;
            }).ifPresent(releases::add);
        }
        return releases;
    }

    private Map<String, ProjectReleaseRelationship> makeReleaseIdToProjectRelationship(List<SpdxBOMImporterSink.Response> releases) {
        return makeReleaseIdToRelationship(releases).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    final ProjectReleaseRelationship projectReleaseRelationship = new ProjectReleaseRelationship();
                    projectReleaseRelationship.setMainlineState(MainlineState.OPEN);
                    projectReleaseRelationship.setReleaseRelation(e.getValue());
                    return projectReleaseRelationship;
                }));
    }


    private Optional<SpdxBOMImporterSink.Response> importAsProject(SpdxElement spdxElement, AttachmentContent attachmentContent) throws SW360Exception, InvalidSPDXAnalysisException {
        if (spdxElement instanceof SpdxPackage) {
            final SpdxPackage spdxPackage = (SpdxPackage) spdxElement;

            final Project project = creatProjectFromSpdxPackage(spdxPackage);

            final Relationship[] relationships = spdxPackage.getRelationships().toArray(new Relationship[spdxPackage.getRelationships().size()]);
            List<SpdxBOMImporterSink.Response> releases = importAsReleases(relationships);
            Map<String, ProjectReleaseRelationship> releaseIdToProjectRelationship = makeReleaseIdToProjectRelationship(releases);
            project.setReleaseIdToUsage(releaseIdToProjectRelationship);

            if(attachmentContent != null) {
                Attachment attachment = makeAttachmentFromContent(attachmentContent);
                project.setAttachments(Collections.singleton(attachment));
            }

            final SpdxBOMImporterSink.Response response = sink.addProject(project);
            response.addChilds(releases);
            return Optional.of(response);
        } else {
            log.debug("Unsupported SpdxElement: " + spdxElement.getClass().getCanonicalName());
            return Optional.empty();
        }
    }


    private String verifyOrSetDefault(String value) {
        return (isNotNullEmptyOrWhitespace(value)) ? value : "";
    }

    private String[] verifyOrSetDefault(String[] values) {
        return (values != null && values.length > 0) ? values : new String[0];
    }

    private String getValue(Optional<String> value) {
        if (value.isPresent()) {
            return value.get();
        } else {
            return "";
        }
    }
}
