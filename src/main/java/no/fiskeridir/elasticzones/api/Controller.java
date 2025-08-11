package no.fiskeridir.elasticzones.api;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.GeoShapeRelation;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.BulkFailureException;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.client.elc.Queries;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchOperations;
import org.springframework.data.elasticsearch.core.geo.GeoJson;
import org.springframework.data.elasticsearch.core.geo.GeoJsonPoint;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.data.elasticsearch.support.HttpHeaders;
import org.springframework.data.geo.Point;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@RestController(value = "/api/v1")
public class Controller {


    private final ElasticsearchOperations elasticsearchOperations;
    private final SearchOperations searchOperations;

    @Autowired
    public Controller(ElasticsearchOperations elasticsearchOperations, SearchOperations searchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.searchOperations = searchOperations;
    }

    @GetMapping("/get-zone")
    public ResponseEntity<MyGeom> ping(@RequestParam Double latitude, @RequestParam Double longitude) {
        //var query = new StringQuery("{\"match_all\":{}},\"filter\":{\"geo_shape\":{\"geometry\":{\"shape\":{\"type\":\"point\",\"coordinates\":["+latitude+","+longitude+"]},\"relation\":\"within\"}}}}");

        var query = new StringQuery("{\"bool\":{\"must\":{\"match_all\":{}},\"filter\":{\"geo_shape\":{\"geometry\":{\"shape\":{\"type\":\"point\",\"coordinates\":["+longitude+","+latitude+"]},\"relation\":\"contains\"}}}}}");
        System.out.println(query.getSource());
        System.out.println(QueryBuilde.CueryFromCoordinates(latitude,longitude));
        var query2 = new StringQuery(QueryBuilde.CueryFromCoordinates(latitude,longitude));
        GeoPoint point = new GeoPoint(latitude, longitude);

        GeoJson kk = GeoJson.of("{\"type\":\"point\",\"coordinates\":["+longitude+","+latitude+"]}");
        var coordinates = GeoJsonPoint.of(latitude,longitude);
        var q = new Criteria("geometry").contains(coordinates);

        var excludeGeom = new FetchSourceFilterBuilder().withExcludes("geometry").build();
        var qq = new CriteriaQuery(q);
        qq.addSourceFilter(excludeGeom);

        var a = elasticsearchOperations.search(qq,MyGeom.class);
        if (a.get().findFirst().isPresent()){
            System.out.println("Her er det. FANT FAKTISK NOE");
            System.out.println(a.get().findFirst().get().getContent().getId());
            System.out.println(a.get().findFirst().get().getContent().getProperties().get("name"));
        }

        var data = JsonData.fromJson("{\"type\":\"point\",\"coordinates\":["+longitude+","+latitude+"]}");

        NativeQuery query5  = NativeQuery.builder().withQuery(quer ->
                quer.bool(bol -> bol.must(mus -> mus.matchAll(
                        ma -> ma))
                        .filter(fil -> fil.geoShape(
                                gs ->
                                        gs.field("geometry").
                                                shape(sh -> sh.relation(GeoShapeRelation.Contains).shape(data)))
                        )))
                .build();

        System.out.println("HER :" + query5.getQuery().toString());
        var result = elasticsearchOperations.search(query5,MyGeom.class);

        var r = result.get().findFirst();

        return r.map(myGeomSearchHit -> ResponseEntity.ok(myGeomSearchHit.getContent())).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/get-zonez")
    public ResponseEntity<List<GeoFeature>> getZone(@RequestParam Double latitude, @RequestParam Double longitude) {
        var cords = GeoJsonPoint.of(latitude, longitude);

        var excludeGeom = new FetchSourceFilterBuilder().withExcludes("geometry").build();

        var query = CriteriaQuery.builder(
                new Criteria("geometry").contains(cords))
                .withSourceFilter(excludeGeom).build();

        var result = searchOperations.search(query, GeoFeature.class);

        if (result.hasSearchHits()){
            var list = result.get().map(SearchHit::getContent).toList();
            return ResponseEntity.ok().body(list);
        }
        return ResponseEntity.notFound().build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @PostMapping(value = "/post-file", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadGeoJson(@RequestParam("file") MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(inputStream);

            if (!root.has("features") || !root.get("features").isArray()) {
                return ResponseEntity.badRequest().body("Invalid GeoJSON: missing 'features' array.");
            }

            for (JsonNode featureNode : root.get("features")) {
                GeoFeature feature = new GeoFeature();
                feature.setId(UUID.randomUUID().toString());
                feature.setType(featureNode.get("type").asText());
                feature.setGeometry(mapper.convertValue(featureNode.get("geometry"), Map.class));
                feature.setProperties(mapper.convertValue(featureNode.get("properties"), Map.class));


                elasticsearchOperations.save(feature);
            }

            return ResponseEntity.ok("GeoJSON features uploaded and stored successfully.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error processing file: " + e.getMessage());
        }
    }

    @PostMapping(value = "/upload-big-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        try {


            final int BATCH = 1_000;
            ObjectMapper m = new ObjectMapper();

            try (InputStream in = file.getInputStream();
                 JsonParser p = m.getFactory().createParser(in)) {

                // move to the start of the "features" array
                if (p.nextToken() != JsonToken.START_OBJECT)
                    return ResponseEntity.badRequest().body("root must be object");
                while (p.nextToken() != null && !"features".equals(p.getCurrentName())) {
                    p.skipChildren();
                }
                if (p.nextToken() != JsonToken.START_ARRAY)
                    return ResponseEntity.badRequest().body("features' must be array");

                List<IndexQuery> queries = new ArrayList<>(BATCH);

                while (p.nextToken() == JsonToken.START_OBJECT) {
                    // capture ONE feature as raw JSON -----------------------------
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(8 * 1024);
                    try (JsonGenerator g = m.getFactory().createGenerator(baos)) {
                        g.copyCurrentStructure(p);           // streams directly, no tree
                    }

                    // (optional) extract id from the bytes, or just generate one
                    String id = UUID.randomUUID().toString();

                    String toStore = baos.toString(StandardCharsets.UTF_8);

                        queries.add(new IndexQueryBuilder()
                                .withId(id)
                                .withSource(toStore)
                                .build());                // :contentReference[oaicite:1]{index=1}


                    if (queries.size() == BATCH) {
                        elasticsearchOperations.bulkIndex(queries,          // one _bulk request
                                IndexCoordinates.of("soner"));            // :contentReference[oaicite:2]{index=2}
                        queries.clear();
                    }
                }
                if (!queries.isEmpty()) {
                    elasticsearchOperations.bulkIndex(queries, IndexCoordinates.of("soner"));
                }
            } catch (BulkFailureException e) {
                System.out.println(e.getMessage());
                System.out.println(e.getFailedDocuments());
                throw e;
            } catch (ElasticsearchException e) {
                System.out.println(e.getMessage());
                throw e;
            }

        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok("File uploaded successfully.");
    }

    /** Detect and transparently inflate .gz uploads (saves bandwidth + disk I/O). */
    private static InputStream maybeWrapGzip(InputStream in) throws IOException {
        in.mark(2);
        int b1 = in.read(), b2 = in.read();
        in.reset();
        return (b1 == 0x1f && b2 == 0x8b) ? new GZIPInputStream(in, 64 * 1024) : in;
    }

}
