package com.weatherwise.transport.global;

import com.weatherwise.enums.TransportMode;
import com.weatherwise.enums.TransportPlaceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OurAirportsImportService {

    private static final String SOURCE = "OURAIRPORTS";

    private final GlobalTransportPlaceRepository repository;
    private final HttpClient httpClient;
    private final String airportsUrl;

    public OurAirportsImportService(
            GlobalTransportPlaceRepository repository,
            @Value("${app.global-transport.ourairports.airports-url:https://ourairports.com/data/airports.csv}")
            String airportsUrl
    ) {
        this.repository = repository;
        this.airportsUrl = airportsUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public int importAirports() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(airportsUrl))
                    .timeout(Duration.ofMinutes(5))
                    .header("User-Agent", "Roamora/1.0")
                    .GET()
                    .build();

            HttpResponse<java.io.InputStream> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofInputStream()
                    );

            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "OurAirports returned HTTP " + response.statusCode()
                );
            }

            int imported = 0;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            response.body(),
                            StandardCharsets.UTF_8
                    ), 64 * 1024)) {

                String headerLine = reader.readLine();
                if (headerLine == null) {
                    throw new IllegalStateException(
                            "OurAirports airports.csv is empty"
                    );
                }

                Map<String, Integer> columns =
                        indexColumns(parseCsvLine(headerLine));

                require(columns, "id");
                require(columns, "ident");
                require(columns, "name");
                require(columns, "latitude_deg");
                require(columns, "longitude_deg");
                require(columns, "iso_country");
                require(columns, "iso_region");
                require(columns, "municipality");
                require(columns, "icao_code");
                require(columns, "iata_code");
                require(columns, "type");

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;

                    String[] row = parseCsvLine(line);

                    String ident = value(row, columns, "ident");
                    String name = value(row, columns, "name");
                    if (ident.isBlank() || name.isBlank()) continue;

                    String airportType = value(row, columns, "type");
                    if (!isUsefulAirportType(airportType)) continue;

                    Double latitude = decimal(row, columns, "latitude_deg");
                    Double longitude = decimal(row, columns, "longitude_deg");
                    if (latitude == null || longitude == null) continue;

                    String sourceId = value(row, columns, "id");
                    if (sourceId.isBlank()) sourceId = ident;

                    String countryCode = value(row, columns, "iso_country");
                    String region = value(row, columns, "iso_region");
                    String city = value(row, columns, "municipality");
                    String icao = value(row, columns, "icao_code");
                    String iata = value(row, columns, "iata_code");

                    repository.upsert(new GlobalTransportPlace(
                            SOURCE,
                            sourceId,
                            name,
                            TransportPlaceType.AIRPORT,
                            city,
                            null,
                            countryCode,
                            region,
                            latitude,
                            longitude,
                            null,
                            firstNonBlank(iata, ident),
                            null,
                            null,
                            airportType,
                            icao,
                            iata,
                            null,
                            List.of(TransportMode.FLIGHT)
                    ));

                    imported++;
                }
            }

            return imported;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "OurAirports import interrupted", e
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "OurAirports import failed", e
            );
        }
    }

    private boolean isUsefulAirportType(String type) {
        return switch (type) {
            case "large_airport", "medium_airport",
                 "small_airport", "heliport",
                 "seaplane_base" -> true;
            default -> false;
        };
    }

    private static Map<String, Integer> indexColumns(String[] headers) {
        Map<String, Integer> result = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            result.put(
                    headers[i].replace("﻿", "").trim().toLowerCase(),
                    i
            );
        }
        return result;
    }

    private static void require(Map<String, Integer> columns, String name) {
        if (!columns.containsKey(name)) {
            throw new IllegalStateException(
                    "Missing OurAirports column: " + name
            );
        }
    }

    private static String value(
            String[] row, Map<String, Integer> columns, String name
    ) {
        Integer index = columns.get(name);
        if (index == null || index >= row.length) return "";
        return row[index] == null ? "" : row[index].trim();
    }

    private static Double decimal(
            String[] row, Map<String, Integer> columns, String name
    ) {
        String value = value(row, columns, name);
        if (value.isBlank()) return null;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private static String[] parseCsvLine(String line) {
        ArrayList<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (quoted && i + 1 < line.length()
                        && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        values.add(current.toString());
        return values.toArray(String[]::new);
    }
}
