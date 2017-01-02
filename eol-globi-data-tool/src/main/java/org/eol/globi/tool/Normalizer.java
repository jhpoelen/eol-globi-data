package org.eol.globi.tool;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.NodeFactoryImpl;
import org.eol.globi.data.ParserFactory;
import org.eol.globi.data.ParserFactoryImpl;
import org.eol.globi.data.StudyImporter;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterFactory;
import org.eol.globi.db.GraphService;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.export.GraphExporterImpl;
import org.eol.globi.geo.EcoregionFinder;
import org.eol.globi.geo.EcoregionFinderFactoryImpl;
import org.eol.globi.opentree.OpenTreeTaxonIndex;
import org.eol.globi.service.DOIResolverImpl;
import org.eol.globi.service.EcoregionFinderProxy;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.taxon.CorrectionService;
import org.eol.globi.taxon.TaxonIndexImpl;
import org.eol.globi.taxon.TaxonomyImporter;
import org.eol.globi.util.HttpUtil;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class Normalizer {
    private static final Log LOG = LogFactory.getLog(Normalizer.class);
    public static final String OPTION_HELP = "h";
    public static final String OPTION_SKIP_IMPORT = "skipImport";
    public static final String OPTION_SKIP_TAXON_CACHE = "skipTaxonCache";
    public static final String OPTION_SKIP_RESOLVE = "skipResolve";
    public static final String OPTION_SKIP_EXPORT = "skipExport";
    public static final String OPTION_SKIP_LINK_THUMBNAILS = "skipLinkThumbnails";
    public static final String OPTION_SKIP_LINK = "skipLink";
    public static final String OPTION_SKIP_REPORT = "skipReport";
    public static final String OPTION_USE_DARK_DATA = "useDarkData";

    private EcoregionFinder ecoregionFinder = null;

    public static void main(final String[] args) throws StudyImporterException, ParseException {
        CommandLine cmdLine = parseOptions(args);
        if (cmdLine.hasOption(OPTION_HELP)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar eol-globi-data-tool-[VERSION]-jar-with-dependencies.jar", getOptions());
        } else {
            new Normalizer().run(cmdLine);
        }
    }


    protected static CommandLine parseOptions(String[] args) throws ParseException {
        CommandLineParser parser = new BasicParser();
        return parser.parse(getOptions(), args);
    }

    private static Options getOptions() {
        Options options = new Options();
        options.addOption(OPTION_SKIP_IMPORT, false, "skip the import of all GloBI datasets");
        options.addOption(OPTION_SKIP_EXPORT, false, "skip the export for GloBI datasets to aggregated archives.");
        options.addOption(OPTION_SKIP_TAXON_CACHE, false, "skip usage of taxon cache");
        options.addOption(OPTION_SKIP_RESOLVE, false, "skip taxon name resolve to external taxonomies");
        options.addOption(OPTION_SKIP_LINK_THUMBNAILS, false, "skip linking of names to thumbnails");
        options.addOption(OPTION_SKIP_LINK, false, "skip taxa cross-reference step");
        options.addOption(OPTION_SKIP_REPORT, false, "skip report generation step");
        options.addOption(OPTION_USE_DARK_DATA, false, "use only dark datasets (requires permission)");

        Option helpOpt = new Option(OPTION_HELP, "help", false, "print this help information");
        options.addOption(helpOpt);
        return options;
    }

    public void run(CommandLine cmdLine) throws StudyImporterException {
        final GraphDatabaseService graphService = GraphService.getGraphService("./");
        try {
            importDatasets(cmdLine, graphService);
            resolveAndLinkTaxa(cmdLine, graphService);
            generateReports(cmdLine, graphService);
            exportData(cmdLine, graphService);
        } finally {
            graphService.shutdown();
            HttpUtil.shutdown();
        }

    }

    public void exportData(CommandLine cmdLine, GraphDatabaseService graphService) throws StudyImporterException {
        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_EXPORT)) {
            exportData(graphService, "./");
        } else {
            LOG.info("skipping data export...");
        }
    }

    public void generateReports(CommandLine cmdLine, GraphDatabaseService graphService) {
        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_REPORT)) {
            new ReportGenerator(graphService).run();
        } else {
            LOG.info("skipping report generation ...");
        }
    }

    public void importDatasets(CommandLine cmdLine, GraphDatabaseService graphService) {
        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_IMPORT)) {
            Collection<Class<? extends StudyImporter>> importers = StudyImporterFactory.getOpenImporters();
            importData(graphService, importers);
        } else {
            LOG.info("skipping data import...");
        }
    }

    public void resolveAndLinkTaxa(CommandLine cmdLine, GraphDatabaseService graphService) {
        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_TAXON_CACHE)) {
            LOG.info("resolving names with taxon cache ...");
            final TaxonCacheService enricher = new TaxonCacheService("/taxa/taxonCache.tsv.gz", "/taxa/taxonMap.tsv.gz");
            TaxonIndexImpl index = new TaxonIndexImpl(enricher, new CorrectionService() {
                @Override
                public String correct(String taxonName) {
                    return taxonName;
                }
            }, graphService);
            index.setIndexResolvedTaxaOnly(true);

            TaxonFilter taxonCacheFilter = new TaxonFilter() {

                private KnownBadNameFilter knownBadNameFilter = new KnownBadNameFilter();

                @Override
                public boolean shouldInclude(Taxon taxon) {
                    return taxon != null
                            && knownBadNameFilter.shouldInclude(taxon)
                            && (!StringUtils.startsWith(taxon.getExternalId(), TaxonomyProvider.INATURALIST_TAXON.getIdPrefix()));
                }
            };

            new NameResolver(graphService, index, taxonCacheFilter).resolve();

            enricher.shutdown();
            LOG.info("resolving names with taxon cache done.");
        } else {
            LOG.info("skipping taxon cache ...");
        }

        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_RESOLVE)) {
            new NameResolver(graphService).resolve();
            new TaxonInteractionIndexer(graphService).index();
        } else {
            LOG.info("skipping taxa resolving ...");
        }

        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_LINK)) {
            linkTaxa(graphService);
        } else {
            LOG.info("skipping taxa linking ...");
        }

        if (cmdLine == null || !cmdLine.hasOption(OPTION_SKIP_LINK_THUMBNAILS)) {
            new ImageLinker().linkImages(graphService, null);
        } else {
            LOG.info("skipping linking of taxa to thumbnails ...");
        }
    }

    protected boolean shouldUseDarkData(CommandLine cmdLine) {
        return cmdLine != null && cmdLine.hasOption(OPTION_USE_DARK_DATA);
    }

    private void linkTaxa(GraphDatabaseService graphService) {
        try {
            new LinkerGlobalNames().link(graphService);
        } catch (PropertyEnricherException e) {
            LOG.warn("Problem linking taxa using Global Names Resolver", e);
        }

        String ottUrl = System.getProperty("ott.url");
        try {
            if (StringUtils.isNotBlank(ottUrl)) {
                new LinkerOpenTreeOfLife().link(graphService, new OpenTreeTaxonIndex(new URI(ottUrl).toURL()));
            }
        } catch (MalformedURLException e) {
            LOG.warn("failed to link against OpenTreeOfLife", e);
        } catch (URISyntaxException e) {
            LOG.warn("failed to link against OpenTreeOfLife", e);
        }

        new LinkerTaxonIndex().link(graphService);

    }

    private EcoregionFinder getEcoregionFinder() {
        if (null == ecoregionFinder) {
            ecoregionFinder = new EcoregionFinderProxy(new EcoregionFinderFactoryImpl().createAll());
        }
        return ecoregionFinder;
    }

    public void setEcoregionFinder(EcoregionFinder finder) {
        this.ecoregionFinder = finder;
    }

    protected void exportData(GraphDatabaseService graphService, String baseDir) throws StudyImporterException {
        new GraphExporterImpl().export(graphService, baseDir);
    }


    private void importData(GraphDatabaseService graphService, Collection<Class<? extends StudyImporter>> importers) {
        NodeFactoryImpl factory = new NodeFactoryImpl(graphService);
        for (Class<? extends StudyImporter> importer : importers) {
            try {
                importData(importer, factory);
            } catch (StudyImporterException e) {
                LOG.error("problem encountered while importing [" + importer.getName() + "]", e);
            }
        }
        EcoregionFinder regionFinder = getEcoregionFinder();
        if (regionFinder != null) {
            regionFinder.shutdown();
        }
    }

    protected void importData(Class<? extends StudyImporter> importer, NodeFactoryImpl factory) throws StudyImporterException {
        StudyImporter studyImporter = createStudyImporter(importer, factory);
        LOG.info("[" + importer + "] importing ...");
        studyImporter.importStudy();
        LOG.info("[" + importer + "] imported.");
    }

    private StudyImporter createStudyImporter(Class<? extends StudyImporter> studyImporter, NodeFactoryImpl factory) throws StudyImporterException {
        factory.setEcoregionFinder(getEcoregionFinder());
        ParserFactory parserFactory = new ParserFactoryImpl();
        StudyImporter importer = new StudyImporterFactory(parserFactory, factory).instantiateImporter(studyImporter);
        if (importer.shouldCrossCheckReference()) {
            factory.setDoiResolver(new DOIResolverImpl());
        } else {
            factory.setDoiResolver(null);
        }

        importer.setLogger(new StudyImportLogger());
        return importer;
    }

}