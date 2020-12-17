package org.eol.globi.data;

import org.eol.globi.domain.LogContext;
import org.eol.globi.service.DatasetLocal;
import org.eol.globi.tool.NullImportLogger;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class DatasetImporterForMangalIT {

    @Test
    public void importAll() throws StudyImporterException {
        AtomicInteger counter = new AtomicInteger(0);
        DatasetImporterForMangal importer = new DatasetImporterForMangal(null, null);
        importer.setDataset(new DatasetLocal(inStream -> inStream));
        importer.setInteractionListener(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {

                InteractionListenerImpl.validLink(interaction, new NullImportLogger() {
                    @Override
                    public void warn(LogContext ctx, String message) {
                        fail(message + "for [" + interaction + "]");
                    }

                });
                counter.incrementAndGet();
            }
        });

        importer.importStudy();

        assertThat(counter.get() > 0, Is.is(true));
    }

}