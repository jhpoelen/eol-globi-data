package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.TaxonomyProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExternalIdUtil {
    public static String urlForExternalId(String externalId) {
        String url = null;
        if (externalId != null) {
            for (Map.Entry<String, String> idPrefixToUrlPrefix : getURLPrefixMap().entrySet()) {
                String idPrefix = idPrefixToUrlPrefix.getKey();
                if (StringUtils.startsWith(externalId, idPrefix)) {
                    if (isIRMNG(idPrefix)) {
                        url = urlForIRMNG(externalId, idPrefix);
                    } else {
                        url = idPrefixToUrlPrefix.getValue() + externalId.replaceAll(idPrefix, "");
                    }
                    String suffix = getURLSuffixMap().get(idPrefix);
                    if (StringUtils.isNotBlank(suffix)) {
                        url = url + suffix;
                    }
                }
                if (url != null) {
                    break;
                }
            }
        }
        return url;
    }

    public static boolean isIRMNG(String idPrefix) {
        return StringUtils.equals(TaxonomyProvider.INTERIM_REGISTER_OF_MARINE_AND_NONMARINE_GENERA.getIdPrefix(), idPrefix);
    }

    public static String urlForIRMNG(String externalId, String idPrefix) {
        String url;
        final String id = externalId.replaceAll(idPrefix, "");
        if (id.length() == 6) {
            url = "http://www.marine.csiro.au/mirrorsearch/ir_search.list_genera?fam_id=" + id;
        } else if (id.length() == 7) {
            url = "http://www.marine.csiro.au/mirrorsearch/ir_search.list_species?gen_id=" + id;
        } else {
            url = "http://www.marine.csiro.au/mirrorsearch/ir_search.list_species?sp_id=" + id;
        }
        return url;
    }

    public static Map<String, String> getURLPrefixMap() {
        return new HashMap<String, String>() {{
            put(TaxonomyProvider.ID_PREFIX_EOL, "http://eol.org/pages/");
            put(TaxonomyProvider.ID_PREFIX_WORMS, "http://www.marinespecies.org/aphia.php?p=taxdetails&id=");
            put(TaxonomyProvider.ID_PREFIX_ENVO, "http://purl.obolibrary.org/obo/ENVO_");
            put(TaxonomyProvider.ID_PREFIX_WIKIPEDIA, "http://wikipedia.org/wiki/");
            put(TaxonomyProvider.ID_PREFIX_GULFBASE, "http://gulfbase.org/biogomx/biospecies.php?species=");
            put(TaxonomyProvider.ID_PREFIX_GAME, "http://public.myfwc.com/FWRI/GAME/Survey.aspx?id=");
            put(TaxonomyProvider.ID_CMECS, "http://cmecscatalog.org/classification/aquaticSetting/");
            put(TaxonomyProvider.ID_BIO_INFO_REFERENCE, "http://bioinfo.org.uk/html/b");
            put(TaxonomyProvider.ID_PREFIX_GBIF, "http://www.gbif.org/species/");
            put(TaxonomyProvider.ID_PREFIX_INATURALIST, "http://www.inaturalist.org/observations/");
            put(TaxonomyProvider.ID_PREFIX_AUSTRALIAN_FAUNAL_DIRECTORY, "http://www.environment.gov.au/biodiversity/abrs/online-resources/fauna/afd/taxa/");
            put(TaxonomyProvider.ID_PREFIX_BIODIVERSITY_AUSTRALIA, "http://biodiversity.org.au/apni.taxon/");
            put(TaxonomyProvider.ID_PREFIX_INDEX_FUNGORUM, "http://www.indexfungorum.org/names/NamesRecord.asp?RecordID=");
            put(TaxonomyProvider.ID_PREFIX_NCBI, "https://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=");
            put(TaxonomyProvider.ID_PREFIX_NBN, "https://data.nbn.org.uk/Taxa/");
            put(TaxonomyProvider.ID_PREFIX_DOI, "http://dx.doi.org/");
            put(TaxonomyProvider.INTERIM_REGISTER_OF_MARINE_AND_NONMARINE_GENERA.getIdPrefix(), "http://www.marine.csiro.au/mirrorsearch/ir_search.list_species?sp_id=");
            put(TaxonomyProvider.OPEN_TREE_OF_LIFE.getIdPrefix(), "https://tree.opentreeoflife.org/taxonomy/browse?id=");
            put(TaxonomyProvider.ID_PREFIX_HTTP, TaxonomyProvider.ID_PREFIX_HTTP);
            put(TaxonomyProvider.ID_PREFIX_ITIS, "http://www.itis.gov/servlet/SingleRpt/SingleRpt?search_topic=TSN&search_value=");
        }};
    }

    public static Map<String, String> getURLSuffixMap() {
        return new HashMap<String, String>() {{
            put(TaxonomyProvider.ID_BIO_INFO_REFERENCE, ".htm");
        }};
    }

    public static boolean isSupported(String externalId) {
        boolean supported = false;
        if (StringUtils.isNotBlank(externalId)) {
            for (TaxonomyProvider prefix : TaxonomyProvider.values()) {
                if (StringUtils.startsWith(externalId, prefix.getIdPrefix())) {
                    supported = true;
                }
            }
        }
        return supported;
    }

    public static String getUrlFromExternalId(String result) {
        String externalId = null;
        try {
            JsonNode jsonNode = new ObjectMapper().readTree(result);
            JsonNode data = jsonNode.get("data");
            if (data != null) {
                for (JsonNode row : data) {
                    for (JsonNode cell : row) {
                        externalId = cell.asText();
                    }
                }
            }
        } catch (IOException e) {
            // ignore
        }
        return buildJsonUrl(urlForExternalId(externalId));
    }

    public static String buildJsonUrl(String url) {
        return StringUtils.isBlank(url) ? "{}" : "{\"url\":\"" + url + "\"}";
    }

    public static String toCitation(String contributor, String description, String publicationYear) {
        String[] array = {contributor, publicationYear, description};
        List<String> nonBlanks = new ArrayList<String>();
        for (String string : array) {
            if (StringUtils.isNotBlank(string)) {
                nonBlanks.add(string);
            }
        }
        return StringUtils.join(nonBlanks, ". ").trim();
    }

    public static String selectValue(Map<String, String> link, String[] candidateIdsInIncreasingPreference) {
        String propertyName = null;
        for (String candidateId : candidateIdsInIncreasingPreference) {
            if (hasProperty(link, candidateId)) {
                propertyName = candidateId;
            }
        }
        return propertyName == null ? "" : link.get(propertyName);
    }

    public static boolean hasProperty(Map<String, String> link, String propertyName) {
        return link.containsKey(propertyName) && org.apache.commons.lang.StringUtils.isNotBlank(link.get(propertyName));
    }
}
