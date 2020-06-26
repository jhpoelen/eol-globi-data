package org.globalbioticinteractions.dataset;

import org.eol.globi.service.ResourceService;
import org.eol.globi.util.InputStreamFactory;
import org.eol.globi.util.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class ResourceServiceWithMapping implements ResourceService {
    private final InputStreamFactory factory;
    private final Dataset dataset;

    public ResourceServiceWithMapping(InputStreamFactory factory, Dataset dataset) {
        this.factory = factory;
        this.dataset = dataset;
    }

    @Override
    public InputStream retrieve(URI resourceName) throws IOException {
        URI mappedResource = DatasetUtil.getNamedResourceURI(dataset, resourceName);
        URI absoluteResourceURI = ResourceUtil.getAbsoluteResourceURI(dataset.getArchiveURI(), mappedResource);
        return ResourceUtil.asInputStream(absoluteResourceURI, factory);
    }

    @Override
    public URI getLocalURI(URI resourceName) throws IOException {
        URI mappedResource = DatasetUtil.getNamedResourceURI(dataset, resourceName);
        return ResourceUtil.getAbsoluteResourceURI(dataset.getArchiveURI(), mappedResource);
    }
}
