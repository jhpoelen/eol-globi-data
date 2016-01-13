package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.util.CSVUtil;
import org.eol.globi.util.ResourceUtil;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class StudyImporterForCoetzer extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForCoetzer.class);

    private String archiveURL = "seltmann/testArchive.zip";

    public StudyImporterForCoetzer(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        DB db = DBMaker
                .newMemoryDirectDB()
                .compressionEnable()
                .transactionDisable()
                .make();
        final HTreeMap<Integer, String> taxonMap = db
                .createHashMap("taxonMap")
                .make();

        final HTreeMap<Integer, String> refMap = db
                .createHashMap("refMap")
                .make();


        try {
            InputStream inputStream = ResourceUtil.asInputStream(getArchiveURL(), StudyImporterForCoetzer.class);
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);
            ZipEntry entry;
            File taxonTempFile = null;
            File assocTempFile = null;
            File referencesTempFile = null;
            File distributionTempFile = null;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().matches("(^|(.*/))taxon.txt$")) {
                    taxonTempFile = FileUtils.saveToTmpFile(zipInputStream, entry);
                } else if (entry.getName().matches("(^|(.*/))description.txt$")) {
                    assocTempFile = FileUtils.saveToTmpFile(zipInputStream, entry);
                } else if (entry.getName().matches("(^|(.*/))references.txt$")) {
                    referencesTempFile = FileUtils.saveToTmpFile(zipInputStream, entry);
                } else if (entry.getName().matches("(^|(.*/))distribution.txt$")) {
                    distributionTempFile = FileUtils.saveToTmpFile(zipInputStream, entry);
                } else {
                    IOUtils.copy(zipInputStream, new NullOutputStream());
                }
            }
            IOUtils.closeQuietly(zipInputStream);

            if (taxonTempFile == null) {
                throw new StudyImporterException("failed to find expected [taxon.txt] resource");
            }

            if (assocTempFile == null) {
                throw new StudyImporterException("failed to find expected [description.txt] resource");
            }

            if (referencesTempFile == null) {
                throw new StudyImporterException("failed to find expected [references.txt] resource");
            }

            if (distributionTempFile == null) {
                throw new StudyImporterException("failed to find expected [distribution.txt] resource");
            }

            BufferedReader assocReader = FileUtils.getUncompressedBufferedReader(new FileInputStream(taxonTempFile), CharsetConstant.UTF8);
            LabeledCSVParser parser = CSVUtil.createLabeledCSVParser(assocReader);
            parser.changeDelimiter('\t');
            String[] line;
            while ((line = parser.getLine()) != null) {
                taxonMap.put(Integer.parseInt(line[0]), nameFor(line));
            }

            LabeledCSVParser refs = CSVUtil.createLabeledCSVParser(new FileInputStream(referencesTempFile));
            refs.changeDelimiter('\t');
            String[] refsLine;
            while ((refsLine = refs.getLine()) != null) {
                refMap.put(Integer.parseInt(refsLine[0]), refsLine[1]);
            }

            LabeledCSVParser assoc = CSVUtil.createLabeledCSVParser(new FileInputStream(assocTempFile));
            assoc.changeDelimiter('\t');

            final Map<String, InteractType> interactTypeMap = new HashMap<String, InteractType>();
            interactTypeMap.put("Visits flowers of", InteractType.VISITS_FLOWERS_OF);
            interactTypeMap.put("Host of", InteractType.VISITS_FLOWERS_OF);
            interactTypeMap.put("Parasite of", InteractType.PARASITE_OF);
            interactTypeMap.put("Nests in", InteractType.INTERACTS_WITH);
            String[] assocLine;
            while ((assocLine = assoc.getLine()) != null) {
                final Integer taxonId = Integer.parseInt(assocLine[0]);
                final String[] parts = assocLine[2].split(":");
                if (parts.length > 1) {
                    String interactionString = parts[0];
                    String[] targetTaxonNames = parts[1].split(",");
                    for (String targetTaxonName : targetTaxonNames) {
                        final String reference = refMap.get(taxonId);
                        final String sourceTaxonName = taxonMap.get(taxonId);
                        if (StringUtils.isNotBlank(reference) && StringUtils.isNotBlank(sourceTaxonName)) {
                            final Study study = nodeFactory.getOrCreateStudy(getSourceCitation() + reference, getSourceCitation() + ". " + ReferenceUtil.createLastAccessedString(getArchiveURL()), reference);
                            final Specimen source = nodeFactory.createSpecimen(study, StringUtils.trim(sourceTaxonName));
                            final Specimen target = nodeFactory.createSpecimen(study, StringUtils.trim(targetTaxonName));
                            final InteractType relType = interactTypeMap.get(interactionString);
                            if (relType == null) {
                                throw new StudyImporterException("found unsupported interaction type [" + interactionString + "]");
                            }
                            source.interactsWith(target, relType);
                        }
                    }
                }
            }
        } catch (
                IOException e
                )

        {
            throw new StudyImporterException(e);
        } catch (
                NodeFactoryException e
                )

        {
            throw new StudyImporterException(e);
        }

        return null;
    }

    protected static String nameFor(String[] line) {
        String genus = StringUtils.trim(line[16]);
        String specificEpithet = StringUtils.trim(line[17]);
        final String speciesName = StringUtils.trim(StringUtils.join(Arrays.asList(genus, specificEpithet), " "));
        return StringUtils.isBlank(speciesName) ? StringUtils.trim(line[4]) : speciesName;
    }

    public String getArchiveURL() {
        return archiveURL;
    }

    public void setArchiveURL(String archiveURL) {
        this.archiveURL = archiveURL;
    }

}