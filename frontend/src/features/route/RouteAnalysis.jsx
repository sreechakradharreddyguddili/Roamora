import {
  CloudRain,
  CloudSun,
  Droplets,
  Gauge,
  Map,
  ShieldCheck,
  Thermometer,
  Wind,
} from "lucide-react";

import "./RouteAnalysis.css";

function formatNumber(value, decimals = 1) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return "—";
  }

  return Number(value).toFixed(decimals);
}

function formatDuration(minutes) {
  if (
    minutes === null ||
    minutes === undefined ||
    Number.isNaN(Number(minutes))
  ) {
    return "—";
  }

  const totalMinutes = Math.round(Number(minutes));
  const hours = Math.floor(totalMinutes / 60);
  const remainingMinutes = totalMinutes % 60;

  if (hours === 0) {
    return `${remainingMinutes} min`;
  }

  if (remainingMinutes === 0) {
    return `${hours} hr`;
  }

  return `${hours} hr ${remainingMinutes} min`;
}

function getWeatherValue(weather, keys) {
  if (!weather) {
    return null;
  }

  for (const key of keys) {
    if (
      weather[key] !== undefined &&
      weather[key] !== null
    ) {
      return weather[key];
    }
  }

  return null;
}

function getWeatherLabel(weather) {
  const code = getWeatherValue(weather, [
    "weatherCode",
    "weathercode",
    "conditionCode",
  ]);

  if (code === null) {
    return "Weather data available";
  }

  const numericCode = Number(code);

  if (numericCode === 0) {
    return "Clear sky";
  }

  if (numericCode <= 3) {
    return "Partly cloudy";
  }

  if (numericCode <= 48) {
    return "Foggy conditions";
  }

  if (numericCode <= 67) {
    return "Rain expected";
  }

  if (numericCode <= 77) {
    return "Snow conditions";
  }

  if (numericCode <= 82) {
    return "Rain showers";
  }

  if (numericCode <= 86) {
    return "Snow showers";
  }

  return "Storm conditions";
}

function WeatherCard({ title, weather }) {
  const temperature = getWeatherValue(weather, [
    "temperature",
    "temperatureC",
    "temperature_2m",
  ]);

  const precipitation = getWeatherValue(weather, [
    "precipitation",
    "precipitationMm",
    "rain",
    "rainMm",
  ]);

  const windSpeed = getWeatherValue(weather, [
    "windSpeed",
    "windSpeedKmh",
    "windspeed",
    "wind_speed",
  ]);

  const humidity = getWeatherValue(weather, [
    "humidity",
    "relativeHumidity",
    "relative_humidity",
  ]);

  return (
    <article className="weather-condition-card">
      <div className="weather-condition-header">
        <div>
          <span>{title}</span>
          <strong>{getWeatherLabel(weather)}</strong>
        </div>

        <CloudSun size={23} />
      </div>

      <div className="weather-main-value">
        <Thermometer size={18} />
        <strong>
          {temperature !== null
            ? `${formatNumber(temperature, 0)}°C`
            : "—"}
        </strong>
      </div>

      <div className="weather-condition-grid">
        <div>
          <Droplets size={15} />
          <span>Humidity</span>
          <strong>
            {humidity !== null
              ? `${formatNumber(humidity, 0)}%`
              : "—"}
          </strong>
        </div>

        <div>
          <CloudRain size={15} />
          <span>Precipitation</span>
          <strong>
            {precipitation !== null
              ? `${formatNumber(precipitation, 1)} mm`
              : "—"}
          </strong>
        </div>

        <div>
          <Wind size={15} />
          <span>Wind</span>
          <strong>
            {windSpeed !== null
              ? `${formatNumber(windSpeed, 1)} km/h`
              : "—"}
          </strong>
        </div>
      </div>
    </article>
  );
}

function RouteAnalysis({ analysis, selectedRoute }) {
  if (!analysis) {
    return null;
  }

  const route = selectedRoute || analysis.alternatives?.[0];

  const originWeather =
    analysis.originWeather ||
    analysis.weather?.origin ||
    analysis.originWeatherData || {
      temperature: analysis.originTemperature,
      windSpeed: analysis.originWindSpeed,
      humidity: analysis.originHumidity,
      precipitation: analysis.originPrecipitation,
      weatherCode: analysis.originWeatherCode,
    };

  const destinationWeather =
    analysis.destinationWeather ||
    analysis.weather?.destination ||
    analysis.destinationWeatherData || {
      temperature: analysis.destinationTemperature,
      windSpeed: analysis.destinationWindSpeed,
      humidity: analysis.destinationHumidity,
      precipitation: analysis.destinationPrecipitation,
      weatherCode: analysis.destinationWeatherCode,
    };

  const distance =
    route?.distanceKm ??
    analysis.distanceKm;

  const duration =
    route?.durationMinutes ??
    analysis.durationMinutes;

  const riskScore =
    route?.riskScore ??
    analysis.riskScore;

  const riskLevel =
    route?.riskLevel ??
    analysis.riskLevel ??
    "UNKNOWN";

  const recommendation =
    route?.recommendation ??
    analysis.recommendation ??
    "Review the route conditions before starting your journey.";

  return (
    <div className="route-analysis">
      <div className="route-summary-grid">
        <div className="route-summary-card">
          <div className="route-summary-icon">
            <Map size={19} />
          </div>

          <span>Distance</span>
          <strong>
            {distance !== undefined &&
            distance !== null
              ? `${formatNumber(distance, 1)} km`
              : "—"}
          </strong>
        </div>

        <div className="route-summary-card">
          <div className="route-summary-icon">
            <Gauge size={19} />
          </div>

          <span>Travel time</span>
          <strong>
            {formatDuration(duration)}
          </strong>
        </div>

        <div className="route-summary-card">
          <div className="route-summary-icon">
            <ShieldCheck size={19} />
          </div>

          <span>Risk score</span>
          <strong>
            {riskScore !== undefined &&
            riskScore !== null
              ? `${riskScore}/100`
              : "—"}
          </strong>
        </div>

        <div className="route-summary-card">
          <div className="route-summary-icon">
            <CloudRain size={19} />
          </div>

          <span>Risk level</span>
          <strong className="route-risk-text">
            {String(riskLevel).replaceAll("_", " ")}
          </strong>
        </div>
      </div>

      <div className="route-weather-grid">
        <WeatherCard
          title="Origin conditions"
          weather={originWeather}
        />

        <WeatherCard
          title="Destination conditions"
          weather={destinationWeather}
        />
      </div>

      <div className="route-recommendation">
        <div className="route-recommendation-icon">
          <ShieldCheck size={20} />
        </div>

        <div>
          <span>ROUTE RECOMMENDATION</span>
          <p>{recommendation}</p>
        </div>
      </div>
    </div>
  );
}

export default RouteAnalysis;