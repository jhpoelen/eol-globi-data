package org.eol.globi.data;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import org.apache.commons.io.IOUtils;
import org.eol.globi.geo.LatLng;
import org.geotools.data.FeatureReader;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import static org.eol.globi.data.StudyImporterForTSV.*;
import static org.junit.internal.matchers.StringContains.containsString;

public class StudyImporterForSzoboszlaiTest extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException, NodeFactoryException {
        StudyImporter importer = new StudyImporterForSzoboszlai(new ParserFactoryImpl(), nodeFactory);
        importer.importStudy();

        assertThat(nodeFactory.findTaxonByName("Thunnus thynnus"), is(notNullValue()));
    }

    @Test
    public void importLines() throws IOException, StudyImporterException {
        StudyImporterForSzoboszlai studyImporterForSzoboszlai = new StudyImporterForSzoboszlai(new ParserFactoryImpl(), nodeFactory);
        final List<Map<String, String>> maps = new ArrayList<Map<String, String>>();
        studyImporterForSzoboszlai.importLinks(IOUtils.toInputStream(firstFewLines()), new InteractionListener(){
            @Override
            public void newLink(Map<String, String> properties) {
                maps.add(properties);
            }
        });
        assertThat(maps.size(), is(4));
        Map<String, String> firstLink = maps.get(0);
        assertThat(firstLink.get(SOURCE_TAXON_ID), is(nullValue()));
        assertThat(firstLink.get(SOURCE_TAXON_NAME), is("Thunnus thynnus"));
        assertThat(firstLink.get(TARGET_TAXON_ID), is("ITIS:161828"));
        assertThat(firstLink.get(TARGET_TAXON_NAME), is("Engraulis mordax"));
        assertThat(firstLink.get(STUDY_SOURCE_CITATION), containsString("Szoboszlai AI, Thayer JA, Wood SA, Sydeman WJ, Koehn LE (2015) Data from: Forage species in predator diets: synthesis of data from the California Current. Dryad Digital Repository. http://dx.doi.org/10.5061/dryad.nv5d2"));
        assertThat(firstLink.get(STUDY_SOURCE_CITATION), containsString("Szoboszlai AI, Thayer JA, Wood SA, Sydeman WJ, Koehn LE (2015) Forage species in predator diets: synthesis of data from the California Current. Ecological Informatics 29(1): 45-56. http://dx.doi.org/10.1016/j.ecoinf.2015.07.003"));
        assertThat(firstLink.get(STUDY_SOURCE_CITATION), containsString("Accessed at"));
        assertThat(firstLink.get(REFERENCE_CITATION), is("Blunt, CE. 1958. California bluefin tuna-wary wanderer of the Pacific. Outdoor California. v.19. pp.14"));
        assertThat(firstLink.get(REFERENCE_DOI), is(nullValue()));
        assertThat(firstLink.get(REFERENCE_URL), is(nullValue()));
        assertThat(firstLink.get(LOCALITY_NAME), is("USA, CA"));
        assertThat(firstLink.get(LOCALITY_ID), is(nullValue()));
        assertThat(firstLink.get(DECIMAL_LONGITUDE), is(nullValue()));
        assertThat(firstLink.get(DECIMAL_LATITUDE), is(nullValue()));
        assertThat(firstLink.get(INTERACTION_TYPE_ID), is("RO:0002439"));
        assertThat(firstLink.get(INTERACTION_TYPE_NAME), is("preysOn"));
    }

    @Test
    public void importShapes() {
        Map<Integer, LatLng> localityMap = new TreeMap<Integer, LatLng>();
        try {
            URL resource = getClass().getResource("szoboszlai/CCPDDlocationdata_v1/LocatPolygonsPoints.shp");
            assertNotNull(resource);
            FileDataStore dataStore = FileDataStoreFinder.getDataStore(resource);
            assertNotNull(dataStore);
            FeatureReader<SimpleFeatureType, SimpleFeature> featureReader = dataStore.getFeatureReader();
            while (featureReader.hasNext()) {
                SimpleFeature feature = featureReader.next();
                Object geom = feature.getAttribute("the_geom");
                if (geom instanceof com.vividsolutions.jts.geom.Point) {
                    Coordinate coordinate = ((Point) geom).getCoordinate();
                    Object localNum = feature.getAttribute("LocatNum");
                    if (localNum instanceof Integer) {
                        Integer locatNum = (Integer) localNum;
                        double lng = coordinate.x;
                        double lat = coordinate.y;
                        localityMap.put(locatNum, new LatLng(lat, lng));
                    }
                }
            }
        } catch (IOException e) {
            fail("datastore not found");
        }

        LatLng centroid = localityMap.get(2361);
        assertThat(centroid, is(notNullValue()));
        assertThat(centroid.getLat(), is(34.00824202376044));
        assertThat(centroid.getLng(), is(-120.72716166720323));
    }


    private String firstFewLines() {
        return "PredPreyNum,PredatorClassName,Predator Family Name,PredatorSciName,PredatorCommName,PreyGroup,PreyGroupTSN,PreySciName,PreySciNameTSN,CiteAuth,CiteYear,CiteTitle,CiteSource,CiteVolume,CitePages,LocatName,LocatSpan,DateName,DateName,Length,ObsTypeName,adjusted sample size,\"Annual=1, Seasonal (<6 mo, 2=summer april-sept, 3=winter oct-ma\",#,% #,FO,% FO,mass or vol,% mass or vol,Index,% Index\n" +
                "47636,Actinopterygii,Scombridae,Thunnus thynnus,Pacific bluefin tuna,Engraulidae,553173,Engraulis mordax,161828,\"Blunt, CE\",1958,California bluefin tuna-wary wanderer of the Pacific,Outdoor California,19,14,\"USA, CA\",span CA-N to CA-S,1957,1957,4,Stomach Content,168,1,,,,x,,,,\n" +
                "39811,Mammalia,Otariidae,Zalophus californianus,California sea lion,Sebastes,166705,Sebastes,166705,\"Weise, MJ, JT Harvey\",2005,\"California Sea Lion (Zalophus californianus) impacts on salmonids near A�o Nuevo Island, California. \",NOAA Report,0,1-31,\"USA, CA, Ano Nuevo Island\",CA-C,2002,2002,4,Scat,,1,,x,,x,x,x,x,\n" +
                "39814,Mammalia,Otariidae,Zalophus californianus,California sea lion,Merlucciidae,164789,Merluccius productus,164792,\"Weise, MJ, JT Harvey\",2005,\"California Sea Lion (Zalophus californianus) impacts on salmonids near A�o Nuevo Island, California. \",NOAA Report,0,1-31,\"USA, CA, Ano Nuevo Island\",CA-C,2002,2002,4,Scat,,1,,x,,x,x,x,x,\n" +
                "39815,Mammalia,Otariidae,Zalophus californianus,California sea lion,Loliginidae,82369,Loligo opalescens,82371,\"Weise, MJ, JT Harvey\",2005,\"California Sea Lion (Zalophus californianus) impacts on salmonids near A�o Nuevo Island, California. \",NOAA Report,0,1-31,\"USA, CA, Ano Nuevo Island\",CA-C,2002,2002,4,Scat,,1,,x,,x,,x,x,\n";
    }
}