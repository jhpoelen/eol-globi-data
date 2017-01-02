package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudyImporterForDunne extends StudyImporterNodesAndLinks {


    public StudyImporterForDunne(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    StudyNode createStudy() throws NodeFactoryException {
        return nodeFactory.getOrCreateStudy2(getNamespace(), ReferenceUtil.sourceCitationLastAccessed(getDataset()), getSourceDOI());
    }

    @Override
    public StudyNode importStudy() throws StudyImporterException {
        StudyNode study = createStudy();
        try {
            LabeledCSVParser nodes = parserFactory.createParser(getNodeResource(), CharsetConstant.UTF8);
            nodes.changeDelimiter(getDelimiter());

            Map<Integer, Taxon> taxonForNode = new HashMap<Integer, Taxon>();


            while (nodes.getLine() != null) {
                Integer nodeId = getNodeId(nodes);
                if (nodeId != null) {
                    final String tsn = nodes.getValueByLabel("TSN");
                    taxonForNode.put(nodeId, new TaxonImpl(nodes.getValueByLabel("Name"), TaxonomyProvider.ID_PREFIX_ITIS + tsn));
                }
            }

            LabeledCSVParser links = parserFactory.createParser(getLinkResource(), CharsetConstant.UTF8);
            links.changeDelimiter(getDelimiter());

            while (links.getLine() != null) {
                List<Location> locations = new ArrayList<>();
                if (getLocation() != null) {
                    Location loc = nodeFactory.getOrCreateLocation(getLocation().getLat(), getLocation().getLng(), null);
                    if (loc != null) {
                        locations.add(loc);
                    }
                }

                for (Location location : locations) {
                    addLink(study, taxonForNode, links, location);
                }
            }

        } catch (IOException e) {
            throw new StudyImporterException("failed to find data file(s)", e);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to create nodes", e);
        }


        return study;
    }

    protected Integer getNodeId(LabeledCSVParser nodes) {
        String nodeID = nodes.getValueByLabel("ID");
        return nodeID == null ? null : Integer.parseInt(nodeID);
    }

    private void addLink(StudyNode study, Map<Integer, Taxon> taxonForNode, LabeledCSVParser links, Location location) throws StudyImporterException {
        Integer consumerNodeID = Integer.parseInt(links.getValueByLabel("Consumer"));
        Integer resourceNodeID = Integer.parseInt(links.getValueByLabel("Resource"));
        SpecimenNode consumer = nodeFactory.createSpecimen(study, taxonForNode.get(consumerNodeID));
        consumer.setExternalId(getNamespace() + ":NodeID:" + consumerNodeID);
        consumer.caughtIn(location);
        SpecimenNode resource = nodeFactory.createSpecimen(study, taxonForNode.get(resourceNodeID));
        resource.setExternalId(getNamespace() + ":NodeID:" + resourceNodeID);
        resource.caughtIn(location);
        consumer.interactsWith(resource, InteractType.ATE);
    }


}
