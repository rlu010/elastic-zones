package no.fiskeridir.elasticzones.api;

import org.apache.commons.text.StringSubstitutor;

import java.util.HashMap;
import java.util.Map;

public class QueryBuilde {

    public static String CueryFromCoordinates(Double lat, Double lon){
        Map<String, String> params = new HashMap<>();
        params.put("latitude", String.valueOf(lat));
        params.put("longitude",String.valueOf(lon));

        String query = """
                {
                    "bool": {
                        "filter": {
                            "geo_shape": {
                                "geometry": {
                                    "relation": "contains",
                                    "shape": {
                                        "coordinates": [
                                            9.3013,
                                            57.3239
                                        ],
                                        "type": "point"
                                    }
                                }
                            }
                        },
                        "must": {
                            "match_all": {}
                        }
                    }
                }
                """;

        return StringSubstitutor.replace(query, params);
    }
}
