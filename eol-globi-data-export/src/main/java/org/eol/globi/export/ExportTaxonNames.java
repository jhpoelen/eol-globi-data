package org.eol.globi.export;

import org.eol.globi.domain.Study;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

public class ExportTaxonNames implements StudyExporter {

    @Override
    public void exportStudy(final Study study, Writer writer, boolean includeHeader) throws IOException {
        if (includeHeader) {
            doExport(study, writer);
        }
    }

    protected void doExport(Study study, Writer writer) {
        String query = "START taxon = node:taxons('*:*')\n" +
                "MATCH taxon-[?:SAME_AS*0..1]->linkedTaxon\n" +
                "RETURN linkedTaxon.externalId? as id" +
                ", linkedTaxon.name? as name" +
                ", linkedTaxon.rank? as rank" +
                ", linkedTaxon.commonNames? as commonNames" +
                ", linkedTaxon.path? as path" +
                ", linkedTaxon.pathIds? as pathIds" +
                ", linkedTaxon.pathNames? as pathNames";

        HashMap<String, Object> params = new HashMap<String, Object>() {{
        }};

        ExportUtil.writeResults(writer, study.getUnderlyingNode().getGraphDatabase(), query, params, true);
    }
}
