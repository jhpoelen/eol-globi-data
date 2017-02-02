package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.PropertyEnricherFactory;
import org.eol.globi.taxon.TaxonIndexNeo4j;
import org.eol.globi.taxon.TaxonNameCorrector;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;

public class NameResolverTest extends GraphDBTestCase {

    private static PropertyEnricher taxonEnricher;

    @BeforeClass
    public static void init() {
        taxonEnricher = PropertyEnricherFactory.createTaxonEnricher();
    }

    @AfterClass
    public static void shutdown() {
        taxonEnricher.shutdown();
    }

    @Test
    public void doNameResolving() throws NodeFactoryException, PropertyEnricherException {
        Specimen human = nodeFactory.createSpecimen(nodeFactory.createStudy(new StudyImpl("bla", null, null, null)), new TaxonImpl("Homo sapiens", null));
        Specimen animal = nodeFactory.createSpecimen(nodeFactory.createStudy(new StudyImpl("bla", null, null, null)), new TaxonImpl("Canis lupus", "EOL:1"));
        human.ate(animal);
        Specimen fish = nodeFactory.createSpecimen(nodeFactory.createStudy(new StudyImpl("bla", null, null, null)), new TaxonImpl("Arius felis", null));
        human.ate(fish);

        assertNull(taxonIndex.findTaxonById("EOL:1"));
        assertNull(taxonIndex.findTaxonByName("Homo sapiens"));

        final TaxonNameCorrector taxonNameCorrector = new TaxonNameCorrector();
        final NameResolver nameResolver = new NameResolver(getGraphDb(), new TaxonIndexNeo4j(taxonEnricher, taxonNameCorrector, getGraphDb()));
        nameResolver.setBatchSize(1L);
        nameResolver.resolve();

        assertAnimalia(taxonIndex.findTaxonById("EOL:1"));

        assertThat(taxonIndex.findTaxonByName("Arius felis"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Ariopsis felis"), is(notNullValue()));

        Taxon homoSapiens = taxonIndex.findTaxonByName("Homo sapiens");
        assertNotNull(homoSapiens);
        assertThat(homoSapiens.getExternalId(), is("EOL:327955"));
    }

    public void assertAnimalia(Taxon animalia) {
        assertNotNull(animalia);
        assertThat(animalia.getName(), is("Animalia"));
        assertThat(animalia.getPath(), is("Animalia"));
    }

    @Test
    public void iNaturalistTaxon() throws NodeFactoryException {
        Specimen someOrganism = nodeFactory.createSpecimen(nodeFactory.createStudy(new StudyImpl("bla1", null, null, null)), new TaxonImpl("Heterotheca grandiflora", null));
        Specimen someOtherOrganism = nodeFactory.createSpecimen(nodeFactory.createStudy(new StudyImpl("bla", null, null, null)), new TaxonImpl("Blaus bla", "INAT_TAXON:58831"));
        Specimen someOtherOrganism2 = nodeFactory.createSpecimen(nodeFactory.createStudy(new StudyImpl("bla", null, null, null)), new TaxonImpl("Redus rha", "INAT_TAXON:126777"));
        someOtherOrganism.ate(someOtherOrganism2);

        final TaxonNameCorrector taxonNameCorrector = new TaxonNameCorrector();
        final NameResolver nameResolver = new NameResolver(getGraphDb(), new TaxonIndexNeo4j(taxonEnricher, taxonNameCorrector, getGraphDb()));
        nameResolver.setBatchSize(1L);
        nameResolver.resolve();

        Taxon resolvedTaxon = taxonIndex.findTaxonById("INAT_TAXON:58831");
        assertThat(resolvedTaxon, is(notNullValue()));
        assertThat(resolvedTaxon.getExternalId(), is("INAT_TAXON:58831"));
        assertThat(resolvedTaxon.getName(), is("Heterotheca grandiflora"));
        Taxon resolvedTaxon2 = taxonIndex.findTaxonByName("Heterotheca grandiflora");
        assertThat(resolvedTaxon2, is(notNullValue()));
        assertThat(resolvedTaxon2.getExternalId(), is("EOL:578180"));
    }

    @Test
    public void progressMessage() {
        assertThat(NameResolver.getProgressMsg(10000L, 5555), is("[1800.18] taxon/s over [5.56] s"));
    }

}
