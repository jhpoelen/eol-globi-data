package org.eol.globi.tool;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryImpl;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterForSimons;
import org.eol.globi.domain.Study;
import org.eol.globi.geo.Ecoregion;
import org.eol.globi.geo.EcoregionFinder;
import org.eol.globi.geo.EcoregionFinderException;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class NormalizerTest extends GraphDBTestCase {

    @Test
    public void handleOptions() throws ParseException {
        CommandLine commandLine = Normalizer.parseOptions(new String[]{"-h"});
        assertThat(commandLine.hasOption("h"), is(true));
        commandLine = Normalizer.parseOptions(new String[]{"--help"});
        assertThat(commandLine.hasOption("h"), is(true));

        commandLine = Normalizer.parseOptions(new String[]{"--skipImport"});
        assertThat(commandLine.hasOption("-skipImport"), is(true));
        assertThat(commandLine.hasOption("skipImport"), is(true));

        commandLine = Normalizer.parseOptions(new String[]{"-skipImport", "--skipExport"});
        assertThat(commandLine.hasOption("-skipImport"), is(true));
        assertThat(commandLine.hasOption("skipImport"), is(true));
        assertThat(commandLine.hasOption("skipExport"), is(true));
        assertThat(commandLine.hasOption("skipLink"), is(false));

        commandLine = Normalizer.parseOptions(new String[]{"-skipLink", "--skipExport"});
        assertThat(commandLine.hasOption("skipLink"), is(true));
    }

    @Test
    public void doSingleImport() throws IOException, StudyImporterException {
        createNormalizer().importData(StudyImporterForSimons.class, new NodeFactoryImpl(getGraphDb()));
        GraphDatabaseService graphService = getGraphDb();

        List<Study> allStudies = NodeUtil.findAllStudies(graphService);
        assertThat(allStudies.size(), is(1));
        assertThat(allStudies.get(0).getTitle(), is("Simons 1997"));

        assertNotNull(graphService.getNodeById(1));
        assertNotNull(graphService.getNodeById(200));
    }

    private Normalizer createNormalizer() {
        Normalizer dataNormalizationTool = new Normalizer();
        dataNormalizationTool.setEcoregionFinder(new EcoregionFinder() {
            @Override
            public Collection<Ecoregion> findEcoregion(double lat, double lng) throws EcoregionFinderException {
                final Ecoregion ecoregion = new Ecoregion();
                ecoregion.setName("some name");
                ecoregion.setPath("some | path");
                ecoregion.setId("someId");
                ecoregion.setGeometry("POINT(1,2)");
                return new ArrayList<Ecoregion>() {{
                    add(ecoregion);
                }};
            }

            @Override
            public void shutdown() {

            }
        });
        return dataNormalizationTool;
    }

    @Test
    public void doSingleImportExport() throws IOException, StudyImporterException {
        Normalizer dataNormalizationTool = createNormalizer();

        GraphDatabaseService graphService = getGraphDb();
        dataNormalizationTool.importData(StudyImporterForSimons.class, new NodeFactoryImpl(graphService));


        String baseDir = "./target/normalizer-test/";
        FileUtils.deleteQuietly(new File(baseDir));
        dataNormalizationTool.exportData(graphService, baseDir);
    }

}