package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.service.Dataset;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;

public class ParserFactoryForDataset implements ParserFactory {

    private final Dataset dataset;

    public ParserFactoryForDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public LabeledCSVParser createParser(String studyResource, String characterEncoding) throws IOException {
        return createParserz(dataset.getResourceURI(studyResource).toString(), characterEncoding);
    }

    private LabeledCSVParser createParserz(String studyResource, String characterEncoding) throws IOException {
        InputStream is = ResourceUtil.asInputStream(studyResource, ParserFactoryLocal.class);
        return CSVTSVUtil.createLabeledCSVParser(FileUtils.getUncompressedBufferedReader(is, characterEncoding));
    }


}
