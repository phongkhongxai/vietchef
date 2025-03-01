package com.spring2025.vietchefs.models.payload.responseModel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleDistanceResponse {
    private String status;
    private List<String> destination_addresses;
    private List<String> origin_addresses;
    private List<Row> rows;

    public String getStatus() {
        return status;
    }

    public List<String> getDestination_addresses() {
        return destination_addresses;
    }

    public List<String> getOrigin_addresses() {
        return origin_addresses;
    }

    public List<Row> getRows() {
        return rows;
    }

    public static class Row {
        private List<Element> elements;

        public List<Element> getElements() {
            return elements;
        }
    }

    public static class Element {
        private String status;
        private Distance distance;
        private Duration duration;

        public String getStatus() {
            return status;
        }

        public Distance getDistance() {
            return distance;
        }

        public Duration getDuration() {
            return duration;
        }
    }

    public static class Distance {
        private String text;
        private int value; // mét

        public String getText() {
            return text;
        }

        public int getValue() {
            return value;
        }
    }

    public static class Duration {
        private String text;
        private int value; // giây

        public String getText() {
            return text;
        }

        public int getValue() {
            return value;
        }
    }
}
