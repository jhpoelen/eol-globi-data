package org.eol.globi.service;

import org.junit.Test;

import java.net.URI;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

public class DatasetRegistryGitHubIT {

    @Test
    public void discoverDatasetsInGitHub() throws DatasetFinderException {
        AtomicBoolean usedStreamFactory = new AtomicBoolean(false);
        Collection<String> urls = new DatasetRegistryGitHubArchive(inStream -> {
            usedStreamFactory.set(true);
            return inStream;
        }).findNamespaces();
        assertThat(urls.size(), is(not(0)));
        assertThat(usedStreamFactory.get(), is(true));
    }

    @Test
    public void datasetFor() throws DatasetFinderException {
        URI uri = new DatasetRegistryGitHubArchive(inStream -> inStream).datasetFor("globalbioticinteractions/template-dataset").getArchiveURI();
        assertThat(uri.toString(), startsWith("https://github.com/globalbioticinteractions/template-dataset/archive/"));
        assertThat(uri.toString(), endsWith(".zip"));
    }

}