package org.eol.globi.data;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.eol.globi.domain.Study;
import org.eol.globi.service.Dataset;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class StudyImporterForArthopodEasyCapture extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForArthopodEasyCapture.class);

    public StudyImporterForArthopodEasyCapture(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        final String rssFeedUrl = getRssFeedUrlString();
        if (org.apache.commons.lang.StringUtils.isBlank(rssFeedUrl)) {
            throw new StudyImporterException("failed to import [" + getDataset().getNamespace() + "]: no [" + "rssFeedURL" + "] specified");
        }

        final String msgPrefix = "importing archive(s) from [" + getRssFeedUrlString() + "]";
        LOG.info(msgPrefix + "...");
        final List<StudyImporter> studyImporters = getStudyImportersForRSSFeed(getDataset(), parserFactory, nodeFactory, getRssFeedUrlString());
        for (StudyImporter importer : studyImporters) {
            if (importer != null) {
                if (getLogger() != null) {
                    importer.setLogger(getLogger());
                }
                importer.importStudy();
            }
        }
        LOG.info(msgPrefix + " done.");
        return null;
    }

    public String getRssFeedUrlString() {
        return getDataset().getOrDefault("rssFeedURL", "");
    }

    public static List<StudyImporter> getStudyImportersForRSSFeed(Dataset datasetOrig, ParserFactory parserFactory, NodeFactory
            nodeFactory, String rssUrlString) throws StudyImporterException {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed;
        try {
            feed = input.build(new XmlReader(new URL(rssUrlString)));
        } catch (FeedException | IOException e) {
            throw new StudyImporterException("failed to read rss feed [" + rssUrlString + "]", e);
        }

        List<StudyImporter> importers = new ArrayList<StudyImporter>();
        final List entries = feed.getEntries();
        for (Object entry : entries) {
            if (entry instanceof SyndEntry) {
                SyndEntry syndEntry = (SyndEntry) entry;
                final StudyImporterForSeltmann studyImporter = new StudyImporterForSeltmann(parserFactory, nodeFactory);
                ObjectNode objectNode = new ObjectMapper().createObjectNode();
                objectNode.put("citation", StringUtils.trim(syndEntry.getDescription().getValue()));
                URI archiveURI = URI.create(StringUtils.trim(syndEntry.getLink()));
                Dataset dataset = new Dataset(datasetOrig.getNamespace(), archiveURI);
                dataset.setConfig(objectNode);
                studyImporter.setDataset(dataset);
                importers.add(studyImporter);
            }
        }
        return importers;
    }

}
