package com.weatherwise.transport.gtfs;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class GtfsFeedLoader {

    private static final Duration CONNECT_TIMEOUT =
            Duration.ofSeconds(30);

    private static final Duration REQUEST_TIMEOUT =
            Duration.ofMinutes(2);

    private static final String USER_AGENT =
            "Roamora/1.0 (GTFS transport data ingestion)";

    private final HttpClient httpClient;

    public GtfsFeedLoader() {

        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(CONNECT_TIMEOUT)
                        .followRedirects(
                                HttpClient.Redirect.NORMAL
                        )
                        .build();
    }

    // ============================================================
    // LOAD FEED
    // ============================================================

    public GtfsFeedData load(GtfsFeed feed) {

        if (feed == null) {

            throw new IllegalArgumentException(
                    "GTFS feed must not be null."
            );
        }

        if (feed.getFeedUrl() == null
                || feed.getFeedUrl().isBlank()) {

            throw new IllegalArgumentException(
                    "GTFS feed URL is not configured."
            );
        }

        try {

            byte[] zipData =
                    downloadFeed(
                            feed.getFeedUrl()
                    );

            GtfsFeedData data =
                    parseFeed(zipData);

            int totalRecords =
                    data.stops().size()
                            + data.routes().size()
                            + data.trips().size()
                            + data.stopTimes().size()
                            + data.calendars().size()
                            + data.calendarDates().size();

            if (totalRecords == 0) {

                throw new IllegalStateException(
                        "GTFS feed downloaded successfully, "
                                + "but no supported GTFS records were parsed."
                );
            }

            return data;

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Unable to parse GTFS feed: "
                            + feed.getProviderName(),
                    exception
            );

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "GTFS download was interrupted: "
                            + feed.getFeedUrl(),
                    exception
            );
        }
    }

    // ============================================================
    // DOWNLOAD
    // ============================================================

    private byte[] downloadFeed(
            String feedUrl
    ) throws IOException, InterruptedException {

        URI uri;

        try {

            uri = URI.create(feedUrl);

        } catch (IllegalArgumentException exception) {

            throw new IllegalStateException(
                    "Invalid GTFS feed URL: "
                            + feedUrl,
                    exception
            );
        }

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(uri)
                        .timeout(REQUEST_TIMEOUT)
                        .header(
                                "User-Agent",
                                USER_AGENT
                        )
                        .header(
                                "Accept",
                                "application/zip, "
                                        + "application/octet-stream, "
                                        + "*/*"
                        )
                        .GET()
                        .build();

        HttpResponse<byte[]> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofByteArray()
                );

        int statusCode =
                response.statusCode();

        System.out.println(
                "GTFS HTTP STATUS: "
                        + statusCode
        );

        System.out.println(
                "GTFS FINAL URL: "
                        + response.uri()
        );

        byte[] body =
                response.body();

        System.out.println(
                "GTFS DOWNLOAD BYTES: "
                        + (body == null
                        ? 0
                        : body.length)
        );

        if (statusCode < 200
                || statusCode >= 300) {

            throw new IllegalStateException(
                    "GTFS provider returned HTTP "
                            + statusCode
                            + " for "
                            + response.uri()
            );
        }

        if (body == null
                || body.length == 0) {

            throw new IllegalStateException(
                    "GTFS provider returned an empty response."
            );
        }

        if (!isZip(body)) {

            String preview =
                    new String(
                            body,
                            0,
                            Math.min(
                                    body.length,
                                    200
                            ),
                            StandardCharsets.UTF_8
                    );

            throw new IllegalStateException(
                    "GTFS provider did not return a ZIP archive. "
                            + "Response preview: "
                            + preview
            );
        }

        return body;
    }

    // ============================================================
    // ZIP VALIDATION
    // ============================================================

    private boolean isZip(
            byte[] data
    ) {

        if (data == null
                || data.length < 4) {

            return false;
        }

        return data[0] == 0x50
                && data[1] == 0x4B
                && (
                (
                        data[2] == 0x03
                                && data[3] == 0x04
                )
                        ||
                        (
                                data[2] == 0x05
                                        && data[3] == 0x06
                        )
                        ||
                        (
                                data[2] == 0x07
                                        && data[3] == 0x08
                        )
        );
    }

    // ============================================================
    // PARSE COMPLETE FEED
    // ============================================================

    private GtfsFeedData parseFeed(
            byte[] zipData
    ) throws IOException {

        List<GtfsStop> stops =
                new ArrayList<>();

        List<GtfsRoute> routes =
                new ArrayList<>();

        List<GtfsTrip> trips =
                new ArrayList<>();

        List<GtfsStopTime> stopTimes =
                new ArrayList<>();

        List<GtfsCalendar> calendars =
                new ArrayList<>();

        List<GtfsCalendarDate> calendarDates =
                new ArrayList<>();

        try (
                ByteArrayInputStream inputStream =
                        new ByteArrayInputStream(zipData);

                ZipInputStream zipInputStream =
                        new ZipInputStream(inputStream)
        ) {

            ZipEntry entry;

            while (
                    (entry =
                            zipInputStream.getNextEntry())
                            != null
            ) {

                if (entry.isDirectory()) {
                    continue;
                }

                String originalName =
                        entry.getName();

                String fileName =
                        extractFileName(
                                originalName
                        );

                byte[] fileData =
                        zipInputStream.readAllBytes();

                System.out.println(
                        "GTFS FILE FOUND: "
                                + originalName
                );

                switch (fileName) {

                    // ------------------------------------------------
                    // STOPS
                    // ------------------------------------------------

                    case "stops.txt" -> {

                        List<GtfsStop> parsed =
                                parseStops(fileData);

                        stops.addAll(parsed);

                        System.out.println(
                                "GTFS STOPS PARSED: "
                                        + parsed.size()
                        );
                    }

                    // ------------------------------------------------
                    // ROUTES
                    // ------------------------------------------------

                    case "routes.txt" -> {

                        List<GtfsRoute> parsed =
                                parseRoutes(fileData);

                        routes.addAll(parsed);

                        System.out.println(
                                "GTFS ROUTES PARSED: "
                                        + parsed.size()
                        );
                    }

                    // ------------------------------------------------
                    // TRIPS
                    // ------------------------------------------------

                    case "trips.txt" -> {

                        List<GtfsTrip> parsed =
                                parseTrips(fileData);

                        trips.addAll(parsed);

                        System.out.println(
                                "GTFS TRIPS PARSED: "
                                        + parsed.size()
                        );
                    }

                    // ------------------------------------------------
                    // CALENDAR
                    // ------------------------------------------------

                    case "calendar.txt" -> {

                        List<GtfsCalendar> parsed =
                                parseCalendar(fileData);

                        calendars.addAll(parsed);

                        System.out.println(
                                "GTFS CALENDAR PARSED: "
                                        + parsed.size()
                        );
                    }

                    // ------------------------------------------------
                    // CALENDAR DATES
                    // ------------------------------------------------

                    case "calendar_dates.txt" -> {

                        List<GtfsCalendarDate> parsed =
                                parseCalendarDates(fileData);

                        calendarDates.addAll(parsed);

                        System.out.println(
                                "GTFS CALENDAR DATES PARSED: "
                                        + parsed.size()
                        );
                    }

                    // ------------------------------------------------
                    // STOP TIMES
                    // ------------------------------------------------

                    case "stop_times.txt" -> {

                        List<GtfsStopTime> parsed =
                                parseStopTimes(fileData);

                        stopTimes.addAll(parsed);

                        System.out.println(
                                "GTFS STOP TIMES PARSED: "
                                        + parsed.size()
                        );
                    }

                    // ------------------------------------------------
                    // OTHER FILES
                    // ------------------------------------------------

                    default -> {

                        System.out.println(
                                "GTFS FILE IGNORED: "
                                        + originalName
                        );
                    }
                }
            }
        }

        System.out.println(
                "================================"
        );

        System.out.println(
                "GTFS TOTAL STOPS: "
                        + stops.size()
        );

        System.out.println(
                "GTFS TOTAL ROUTES: "
                        + routes.size()
        );

        System.out.println(
                "GTFS TOTAL TRIPS: "
                        + trips.size()
        );

        System.out.println(
                "GTFS TOTAL CALENDARS: "
                        + calendars.size()
        );

        System.out.println(
                "GTFS TOTAL CALENDAR DATES: "
                        + calendarDates.size()
        );

        System.out.println(
                "GTFS TOTAL STOP TIMES: "
                        + stopTimes.size()
        );

        System.out.println(
                "================================"
        );

        return new GtfsFeedData(
                stops,
                routes,
                trips,
                stopTimes,
                calendars,
                calendarDates
        );
    }

    // ============================================================
    // FILE NAME
    // ============================================================

    private String extractFileName(
            String entryName
    ) {

        if (entryName == null
                || entryName.isBlank()) {

            return "";
        }

        String normalized =
                entryName
                        .replace('\\', '/')
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        int slash =
                normalized.lastIndexOf('/');

        if (slash >= 0) {

            normalized =
                    normalized.substring(
                            slash + 1
                    );
        }

        return normalized;
    }

    // ============================================================
    // STOPS
    // ============================================================

    private List<GtfsStop> parseStops(
            byte[] fileData
    ) throws IOException {

        List<GtfsStop> result =
                new ArrayList<>();

        try (
                BufferedReader reader =
                        createReader(fileData);

                CSVParser parser =
                        createParser(reader)
        ) {

            for (CSVRecord record : parser) {

                String stopId =
                        get(
                                record,
                                "stop_id"
                        );

                if (stopId == null) {
                    continue;
                }

                result.add(
                        new GtfsStop(
                                stopId,

                                get(
                                        record,
                                        "stop_code"
                                ),

                                get(
                                        record,
                                        "stop_name"
                                ),

                                getDouble(
                                        record,
                                        "stop_lat"
                                ),

                                getDouble(
                                        record,
                                        "stop_lon"
                                ),

                                get(
                                        record,
                                        "location_type"
                                ),

                                get(
                                        record,
                                        "parent_station"
                                ),

                                get(
                                        record,
                                        "stop_timezone"
                                )
                        )
                );
            }
        }

        return result;
    }

    // ============================================================
    // ROUTES
    // ============================================================

    private List<GtfsRoute> parseRoutes(
            byte[] fileData
    ) throws IOException {

        List<GtfsRoute> result =
                new ArrayList<>();

        try (
                BufferedReader reader =
                        createReader(fileData);

                CSVParser parser =
                        createParser(reader)
        ) {

            for (CSVRecord record : parser) {

                String routeId =
                        get(
                                record,
                                "route_id"
                        );

                if (routeId == null) {
                    continue;
                }

                result.add(
                        new GtfsRoute(
                                routeId,

                                get(
                                        record,
                                        "route_short_name"
                                ),

                                get(
                                        record,
                                        "route_long_name"
                                ),

                                get(
                                        record,
                                        "route_type"
                                ),

                                get(
                                        record,
                                        "agency_id"
                                )
                        )
                );
            }
        }

        return result;
    }

    // ============================================================
    // TRIPS
    // ============================================================

    private List<GtfsTrip> parseTrips(
            byte[] fileData
    ) throws IOException {

        List<GtfsTrip> result =
                new ArrayList<>();

        try (
                BufferedReader reader =
                        createReader(fileData);

                CSVParser parser =
                        createParser(reader)
        ) {

            for (CSVRecord record : parser) {

                String tripId =
                        get(
                                record,
                                "trip_id"
                        );

                if (tripId == null) {
                    continue;
                }

                result.add(
                        new GtfsTrip(
                                tripId,

                                get(
                                        record,
                                        "route_id"
                                ),

                                get(
                                        record,
                                        "service_id"
                                ),

                                get(
                                        record,
                                        "trip_headsign"
                                ),

                                get(
                                        record,
                                        "direction_id"
                                )
                        )
                );
            }
        }

        return result;
    }

    // ============================================================
    // CALENDAR
    // ============================================================

    private List<GtfsCalendar> parseCalendar(
            byte[] fileData
    ) throws IOException {

        List<GtfsCalendar> result =
                new ArrayList<>();

        try (
                BufferedReader reader =
                        createReader(fileData);

                CSVParser parser =
                        createParser(reader)
        ) {

            for (CSVRecord record : parser) {

                String serviceId =
                        get(
                                record,
                                "service_id"
                        );

                if (serviceId == null) {
                    continue;
                }

                LocalDate startDate =
                        parseGtfsDate(
                                get(
                                        record,
                                        "start_date"
                                )
                        );

                LocalDate endDate =
                        parseGtfsDate(
                                get(
                                        record,
                                        "end_date"
                                )
                        );

                if (startDate == null
                        || endDate == null) {

                    continue;
                }

                result.add(
                        new GtfsCalendar(
                                serviceId,

                                parseBooleanFlag(
                                        get(
                                                record,
                                                "monday"
                                        )
                                ),

                                parseBooleanFlag(
                                        get(
                                                record,
                                                "tuesday"
                                        )
                                ),

                                parseBooleanFlag(
                                        get(
                                                record,
                                                "wednesday"
                                        )
                                ),

                                parseBooleanFlag(
                                        get(
                                                record,
                                                "thursday"
                                        )
                                ),

                                parseBooleanFlag(
                                        get(
                                                record,
                                                "friday"
                                        )
                                ),

                                parseBooleanFlag(
                                        get(
                                                record,
                                                "saturday"
                                        )
                                ),

                                parseBooleanFlag(
                                        get(
                                                record,
                                                "sunday"
                                        )
                                ),

                                startDate,
                                endDate
                        )
                );
            }
        }

        return result;
    }

    // ============================================================
    // CALENDAR DATES
    // ============================================================

    private List<GtfsCalendarDate> parseCalendarDates(
            byte[] fileData
    ) throws IOException {

        List<GtfsCalendarDate> result =
                new ArrayList<>();

        try (
                BufferedReader reader =
                        createReader(fileData);

                CSVParser parser =
                        createParser(reader)
        ) {

            for (CSVRecord record : parser) {

                String serviceId =
                        get(
                                record,
                                "service_id"
                        );

                LocalDate exceptionDate =
                        parseGtfsDate(
                                get(
                                        record,
                                        "date"
                                )
                        );

                Integer exceptionType =
                        getInteger(
                                record,
                                "exception_type"
                        );

                if (serviceId == null
                        || exceptionDate == null
                        || exceptionType == null) {

                    continue;
                }

                if (exceptionType != 1
                        && exceptionType != 2) {

                    continue;
                }

                result.add(
                        new GtfsCalendarDate(
                                serviceId,
                                exceptionDate,
                                exceptionType
                        )
                );
            }
        }

        return result;
    }

    // ============================================================
    // STOP TIMES
    // ============================================================

    private List<GtfsStopTime> parseStopTimes(
            byte[] fileData
    ) throws IOException {

        List<GtfsStopTime> result =
                new ArrayList<>();

        try (
                BufferedReader reader =
                        createReader(fileData);

                CSVParser parser =
                        createParser(reader)
        ) {

            for (CSVRecord record : parser) {

                String tripId =
                        get(
                                record,
                                "trip_id"
                        );

                String stopId =
                        get(
                                record,
                                "stop_id"
                        );

                if (tripId == null
                        || stopId == null) {

                    continue;
                }

                result.add(
                        new GtfsStopTime(
                                tripId,

                                get(
                                        record,
                                        "arrival_time"
                                ),

                                get(
                                        record,
                                        "departure_time"
                                ),

                                stopId,

                                getInteger(
                                        record,
                                        "stop_sequence"
                                ),

                                get(
                                        record,
                                        "stop_headsign"
                                )
                        )
                );
            }
        }

        return result;
    }

    // ============================================================
    // CSV
    // ============================================================

    private CSVParser createParser(
            BufferedReader reader
    ) throws IOException {

        return CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .get()
                .parse(reader);
    }

    private BufferedReader createReader(
            byte[] fileData
    ) {

        return new BufferedReader(
                new InputStreamReader(
                        new ByteArrayInputStream(
                                fileData
                        ),
                        StandardCharsets.UTF_8
                )
        );
    }

    // ============================================================
    // CSV VALUE
    // ============================================================

    private String get(
            CSVRecord record,
            String column
    ) {

        if (!record.isMapped(column)) {
            return null;
        }

        String value =
                record.get(column);

        if (value == null
                || value.isBlank()) {

            return null;
        }

        return value
                .replace("\uFEFF", "")
                .trim();
    }

    // ============================================================
    // DOUBLE
    // ============================================================

    private Double getDouble(
            CSVRecord record,
            String column
    ) {

        String value =
                get(
                        record,
                        column
                );

        if (value == null) {
            return null;
        }

        try {

            return Double.parseDouble(value);

        } catch (NumberFormatException exception) {

            return null;
        }
    }

    // ============================================================
    // INTEGER
    // ============================================================

    private Integer getInteger(
            CSVRecord record,
            String column
    ) {

        String value =
                get(
                        record,
                        column
                );

        if (value == null) {
            return null;
        }

        try {

            return Integer.parseInt(value);

        } catch (NumberFormatException exception) {

            return null;
        }
    }

    // ============================================================
    // GTFS DATE
    // ============================================================

    private LocalDate parseGtfsDate(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return null;
        }

        String normalized =
                value.trim();

        if (!normalized.matches(
                "\\d{8}"
        )) {

            return null;
        }

        try {

            return LocalDate.of(
                    Integer.parseInt(
                            normalized.substring(
                                    0,
                                    4
                            )
                    ),

                    Integer.parseInt(
                            normalized.substring(
                                    4,
                                    6
                            )
                    ),

                    Integer.parseInt(
                            normalized.substring(
                                    6,
                                    8
                            )
                    )
            );

        } catch (Exception exception) {

            return null;
        }
    }

    // ============================================================
    // GTFS BOOLEAN
    // ============================================================

    private boolean parseBooleanFlag(
            String value
    ) {

        return "1".equals(
                value
        );
    }
}