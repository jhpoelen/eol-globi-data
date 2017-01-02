package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationNode;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Term;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ExporterOccurrencesTest extends GraphDBTestCase {

    @Test
    public void exportMissingLength() throws IOException, NodeFactoryException, ParseException {
        createTestData(null);
        resolveNames();
        String expected = getExpectedHeader();
        expected += getExpectedData();

        StudyNode myStudy1 = nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        exportOccurrences().exportStudy(myStudy1, row, true);

        assertThat(row.getBuffer().toString().trim(), equalTo(expected.trim()));
    }

    private String getExpectedData() {
        return "\nglobi:occur:2\tEOL:327955\t\t\t\t\tJUVENILE\t\t\t\t\t\t\t\t\t\t\t\t\t1992-03-30T08:00:00Z\t\t\t12.0\t-1.0\t\t\t-60.0 m\tDIGESTATE\tBONE" +
                "\nglobi:occur:6\tEOL:328607\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t1992-03-30T08:00:00Z\t\t\t12.0\t-1.0\t\t\t-60.0 m\t\t" +
                "\nglobi:occur:8\tEOL:328607\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t1992-03-30T08:00:00Z\t\t\t12.0\t-1.0\t\t\t-60.0 m\t\t";
    }

    private String getExpectedHeader() {
        return "occurrenceID\ttaxonID\tinstitutionCode\tcollectionCode\tcatalogNumber\tsex\tlifeStage\treproductiveCondition\tbehavior\testablishmentMeans\toccurrenceRemarks\tindividualCount\tpreparations\tfieldNotes\tbasisOfRecord\tsamplingProtocol\tsamplingEffort\tidentifiedBy\tdateIdentified\teventDate\tmodified\tlocality\tdecimalLatitude\tdecimalLongitude\tverbatimLatitude\tverbatimLongitude\tverbatimElevation\tphysiologicalState\tbodyPart";
    }

    @Test
    public void exportNoHeader() throws IOException, NodeFactoryException, ParseException {
        createTestData(null);
        resolveNames();
        String expected = getExpectedData();

        StudyNode myStudy1 = nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        exportOccurrences().exportStudy(myStudy1, row, false);

        assertThat(row.getBuffer().toString(), equalTo(expected));
    }

    private ExporterOccurrences exportOccurrences() {
        return new ExporterOccurrences();
    }

    @Test
    public void exportToCSV() throws NodeFactoryException, IOException, ParseException {
        createTestData(123.0);
        resolveNames();
        String expected = "";
        expected += getExpectedHeader();
        expected += getExpectedData();

        StudyNode myStudy1 = nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        exportOccurrences().exportStudy(myStudy1, row, true);

        assertThat(row.getBuffer().toString(), equalTo(expected));

    }

    @Test
    public void dontExportToCSVSpecimenEmptyStomach() throws NodeFactoryException, IOException {
        StudyNode myStudy = nodeFactory.createStudy("myStudy");
        Specimen specimen = nodeFactory.createSpecimen(myStudy, "Homo sapiens", "EOL:123");
        specimen.setBasisOfRecord(new Term("test:123", "aBasisOfRecord"));
        resolveNames();

        StringWriter row = new StringWriter();

        exportOccurrences().exportStudy(myStudy, row, true);

        String expected = "";
        expected += getExpectedHeader();
        expected += "\nglobi:occur:2\tEOL:123\t\t\t\t\t\t\t\t\t\t\t\t\taBasisOfRecord\t\t\t\t\t\t\t\t\t\t\t\t\t\t";

        assertThat(row.getBuffer().toString(), equalTo(expected));
    }

    private void createTestData(Double length) throws NodeFactoryException, ParseException {
        StudyNode myStudy = nodeFactory.createStudy("myStudy");
        SpecimenNode specimen = nodeFactory.createSpecimen(myStudy, "Homo sapiens", "EOL:327955");
        specimen.setStomachVolumeInMilliLiter(666.0);
        specimen.setLifeStage(new Term("GLOBI:JUVENILE", "JUVENILE"));
        specimen.setPhysiologicalState(new Term("GLOBI:DIGESTATE", "DIGESTATE"));
        specimen.setBodyPart(new Term("GLOBI:BONE", "BONE"));
        nodeFactory.setUnixEpochProperty(specimen, ExportTestUtil.utcTestDate());
        if (null != length) {
            specimen.setLengthInMm(length);
        }

        Location location = nodeFactory.getOrCreateLocation(12.0, -1.0, -60.0);
        specimen.caughtIn(location);
        Specimen wolf1 = eatWolf(specimen, myStudy);
        wolf1.caughtIn(location);
        Specimen wolf2 = eatWolf(specimen, myStudy);
        wolf2.caughtIn(location);
    }

    private Specimen eatWolf(Specimen specimen, StudyNode study) throws NodeFactoryException {
        SpecimenNode otherSpecimen = nodeFactory.createSpecimen(study, "Canis lupus", "EOL:328607");
        otherSpecimen.setVolumeInMilliLiter(124.0);
        nodeFactory.setUnixEpochProperty(otherSpecimen, ExportTestUtil.utcTestDate());
        specimen.ate(otherSpecimen);
        return otherSpecimen;
    }


    @Test
    public void darwinCoreMetaTable() throws IOException {
        ExportTestUtil.assertFileInMeta(exportOccurrences());
    }

}
