package org.eol.globi.data;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.*;

public class TableInteractionListenerProxyTest {

    @Test
    public void interactionListenerTest() throws IOException, StudyImporterException {
        final List<Map<String, String>> links = new ArrayList<Map<String, String>>();
        JsonNode config = new ObjectMapper().readTree("{ \"dcterms:bibliographicCitation\":\"some citation\", \"url\":\"some resource url\" }");
        final TableInteractionListenerProxy listener = new TableInteractionListenerProxy("http://someurl", config, new InteractionListener() {
            @Override
            public void newLink(Map<String, String> properties) throws StudyImporterException {
                links.add(properties);
            }
        });
        listener.newLink(new HashMap<String, String>());

        assertThat(links.size(), is(1));
        assertThat(links.get(0).get(StudyImporterForTSV.STUDY_SOURCE_CITATION), startsWith("some citation . Accessed at some resource url on "));
        assertThat(links.get(0).get(StudyImporterForTSV.REFERENCE_CITATION), startsWith("some citation . Accessed at some resource url on "));
    }

}