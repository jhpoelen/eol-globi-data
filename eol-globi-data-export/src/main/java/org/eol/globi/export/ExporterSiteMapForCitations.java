package org.eol.globi.export;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.export.SiteMapUtils;
import org.eol.globi.domain.Study;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ExporterSiteMapForCitations implements GraphExporter {

    @Override
    public void export(GraphDatabaseService graphDb, String baseDirName) throws StudyImporterException {
        Set<String> accordingToHits = new HashSet<String>();
        // just do it once
        final List<Study> allStudies = NodeUtil.findAllStudies(graphDb);
        for (Study allStudy : allStudies) {
            final String doi = allStudy.getDOI();
            if (StringUtils.isNotBlank(doi)) {
                accordingToHits.add(doi);
            } else if (StringUtils.isNotBlank(allStudy.getCitation())) {
                accordingToHits.add(allStudy.getCitation());
            }
            accordingToHits.add(allStudy.getSource());
        }
        SiteMapUtils.generateSiteMap(accordingToHits, baseDirName, "accordingTo=");
    }


}
