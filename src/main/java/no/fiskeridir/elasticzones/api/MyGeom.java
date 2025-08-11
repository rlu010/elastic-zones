package no.fiskeridir.elasticzones.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Filter;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Document(indexName = "ezy")
public class MyGeom {
    private String type;
    private Integer id;
    @Filter
    Map<String, String> properties;

    public String getType() {
        return type;
    } // Jeg er lat, trenger jeg å postere typen, eller mappe til en geojson

    public void setType(String type) {
        this.type = type;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public static class Properties {
        private int FID;
        private String name;

        public int getFID() {
            return FID;
        }

        public void setFID(int FID) {
            this.FID = FID;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
