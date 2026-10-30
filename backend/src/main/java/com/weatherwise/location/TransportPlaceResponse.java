package com.weatherwise.location;

import java.util.List;

public record TransportPlaceResponse(

        String query,

        String city,

        String country,

        List<TransportPlace> places

) {
}