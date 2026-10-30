import {
  useEffect,
  useRef,
  useState,
} from "react";

import {
  ArrowRight,
  Bike,
  Bus,
  Car,
  Footprints,
  Loader2,
  MapPin,
  Plane,
  Ship,
  Train,
} from "lucide-react";

import { searchLocations } from "../../api/routeApi";
import { useSearchParams } from "react-router-dom";

import "./SearchForm.css";

const TRANSPORT_MODES = [
  {
    value: "CAR",
    label: "Car",
    icon: Car,
    group: "Road",
  },
  {
    value: "MOTORCYCLE",
    label: "Motorcycle",
    icon: Bike,
    group: "Road",
  },
  {
    value: "TAXI",
    label: "Taxi",
    icon: Car,
    group: "Road",
  },
  {
    value: "AUTO_RICKSHAW",
    label: "Auto",
    icon: Car,
    group: "Road",
  },
  {
    value: "BICYCLE",
    label: "Bicycle",
    icon: Bike,
    group: "Road",
  },
  {
    value: "SCOOTER",
    label: "Scooter",
    icon: Bike,
    group: "Road",
  },
  {
    value: "WALKING",
    label: "Walking",
    icon: Footprints,
    group: "Road",
  },
  {
    value: "BUS",
    label: "Bus",
    icon: Bus,
    group: "Public",
  },
  {
    value: "COACH",
    label: "Coach",
    icon: Bus,
    group: "Public",
  },
  {
    value: "METRO",
    label: "Metro",
    icon: Train,
    group: "Public",
  },
  {
    value: "SUBWAY",
    label: "Subway",
    icon: Train,
    group: "Public",
  },
  {
    value: "TRAM",
    label: "Tram",
    icon: Train,
    group: "Public",
  },
  {
    value: "LIGHT_RAIL",
    label: "Light rail",
    icon: Train,
    group: "Public",
  },
  {
    value: "MONORAIL",
    label: "Monorail",
    icon: Train,
    group: "Public",
  },
  {
    value: "TRAIN",
    label: "Train",
    icon: Train,
    group: "Public",
  },
  {
    value: "COMMUTER_RAIL",
    label: "Commuter rail",
    icon: Train,
    group: "Public",
  },
  {
    value: "HIGH_SPEED_TRAIN",
    label: "High-speed rail",
    icon: Train,
    group: "Public",
  },
  {
    value: "FLIGHT",
    label: "Flight",
    icon: Plane,
    group: "Air & water",
  },
  {
    value: "FERRY",
    label: "Ferry",
    icon: Ship,
    group: "Air & water",
  },
  {
    value: "SHIP",
    label: "Ship",
    icon: Ship,
    group: "Air & water",
  },
];

function getToday() {
  const now = new Date();

  const year = now.getFullYear();
  const month = String(
    now.getMonth() + 1
  ).padStart(2, "0");
  const day = String(
    now.getDate()
  ).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function getCurrentTime() {
  const now = new Date();

  const hours = String(
    now.getHours()
  ).padStart(2, "0");

  const minutes = String(
    now.getMinutes()
  ).padStart(2, "0");

  return `${hours}:${minutes}`;
}

function SearchForm({
  onSearch,
  loading,
}) {
  const [searchParams] = useSearchParams();
  const [origin, setOrigin] =
    useState(searchParams.get("origin") || "");

  const [destination, setDestination] =
    useState(searchParams.get("destination") || "");

  const [transportMode, setTransportMode] =
    useState("CAR");

  const [departureDate, setDepartureDate] =
    useState(getToday());

  const [departureTime, setDepartureTime] =
    useState(getCurrentTime());

  const [validationError, setValidationError] =
    useState("");

  const [selectedOrigin, setSelectedOrigin] = useState(null);
  const [selectedDestination, setSelectedDestination] = useState(null);

  // URL query parameters are external navigation state; synchronize prefilled fields when the copilot opens the planner.
  useEffect(() => {
    const initialOrigin = searchParams.get("origin");
    const initialDestination = searchParams.get("destination");
    // eslint-disable-next-line react-hooks/set-state-in-effect
    if (initialOrigin) setOrigin(initialOrigin);
    if (initialDestination) setDestination(initialDestination);
    if (initialOrigin || initialDestination) {
      setSelectedOrigin(null);
      setSelectedDestination(null);
      setValidationError("");
    }
  }, [searchParams]);

  const [
    originSuggestions,
    setOriginSuggestions,
  ] = useState([]);

  const [
    destinationSuggestions,
    setDestinationSuggestions,
  ] = useState([]);

  const [
    originLoading,
    setOriginLoading,
  ] = useState(false);

  const [
    destinationLoading,
    setDestinationLoading,
  ] = useState(false);

  const [
    activeLocation,
    setActiveLocation,
  ] = useState(null);

  const formRef = useRef(null);

  const originRequestId =
    useRef(0);

  const destinationRequestId =
    useRef(0);

  useEffect(() => {
    const handleClickOutside = (
      event
    ) => {
      if (
        formRef.current &&
        !formRef.current.contains(
          event.target
        )
      ) {
        setActiveLocation(null);
        document.activeElement?.blur();
      }
    };

    document.addEventListener(
      "mousedown",
      handleClickOutside
    );

    return () => {
      document.removeEventListener(
        "mousedown",
        handleClickOutside
      );
    };
  }, []);

  useEffect(() => {
    const query = origin.trim();

    if (query.length < 2) {
      return undefined;
    }

    const requestId =
      ++originRequestId.current;

    const timer = setTimeout(
      async () => {
        setOriginLoading(true);

        try {
          const results =
            await searchLocations(
              query
            );

          if (
            requestId ===
            originRequestId.current
          ) {
            setOriginSuggestions(
              results
            );
          }
        } catch {
          if (
            requestId ===
            originRequestId.current
          ) {
            setOriginSuggestions([]);
          }
        } finally {
          if (
            requestId ===
            originRequestId.current
          ) {
            setOriginLoading(false);
          }
        }
      },
      350
    );

    return () => {
      clearTimeout(timer);
    };
  }, [origin]);

  useEffect(() => {
    const query =
      destination.trim();

    if (query.length < 2) {
      return undefined;
    }

    const requestId =
      ++destinationRequestId.current;

    const timer = setTimeout(
      async () => {
        setDestinationLoading(true);

        try {
          const results =
            await searchLocations(
              query
            );

          if (
            requestId ===
            destinationRequestId.current
          ) {
            setDestinationSuggestions(
              results
            );
          }
        } catch {
          if (
            requestId ===
            destinationRequestId.current
          ) {
            setDestinationSuggestions(
              []
            );
          }
        } finally {
          if (
            requestId ===
            destinationRequestId.current
          ) {
            setDestinationLoading(
              false
            );
          }
        }
      },
      350
    );

    return () => {
      clearTimeout(timer);
    };
  }, [destination]);

  const handleSubmit = (
    event
  ) => {
    event.preventDefault();

    const cleanOrigin =
      origin.trim();

    const cleanDestination =
      destination.trim();

    if (
      !cleanOrigin ||
      !cleanDestination
    ) {
      setValidationError(
        "Please enter both your starting point and destination."
      );
      return;
    }

    if (
      cleanOrigin.toLowerCase() ===
      cleanDestination.toLowerCase()
    ) {
      setValidationError(
        "Origin and destination must be different."
      );
      return;
    }

    if (!departureDate) {
      setValidationError(
        "Please select a departure date."
      );
      return;
    }

    if (!departureTime) {
      setValidationError(
        "Please select a departure time."
      );
      return;
    }

    const now = new Date();
    const selectedDeparture = new Date(
      `${departureDate}T${departureTime}`
    );

    if (
      departureDate === getToday() &&
      selectedDeparture.getTime() < now.getTime() - 60000
    ) {
      setValidationError(
        "That departure time has already passed. Choose a later time or use ‘Use now’."
      );
      return;
    }

    setActiveLocation(null);
    setValidationError("");

    onSearch(
      cleanOrigin,
      cleanDestination,
      transportMode,
      departureDate,
      departureTime,
      selectedOrigin,
      selectedDestination
    );
  };

  const handleLocationChange = (
    setter,
    value,
    locationType
  ) => {
    setter(value);

    if (locationType === "origin") {
      setSelectedOrigin(null);
      setOriginSuggestions([]);
    } else {
      setSelectedDestination(null);
      setDestinationSuggestions([]);
    }

    setValidationError("");
  };

  const handleLocationSelect = (
    locationType,
    location
  ) => {
    const displayName =
      location?.name || "";

    if (!displayName) {
      return;
    }

    if (locationType === "origin") {
      setOrigin(displayName);
      setSelectedOrigin(location);
      setOriginSuggestions([]);
    } else {
      setDestination(displayName);
      setSelectedDestination(location);
      setDestinationSuggestions([]);
    }

    setActiveLocation(null);
    setValidationError("");
  };

  const handleTransportChange = (
    value
  ) => {
    setTransportMode(value);
    setValidationError("");
  };

  const handleUseNow = () => {
    setDepartureDate(getToday());
    setDepartureTime(
      getCurrentTime()
    );
    setValidationError("");
  };

  const renderLocationSuggestions = (
    locationType,
    suggestions
  ) => {
    if (
      activeLocation !==
        locationType ||
      suggestions.length === 0
    ) {
      return null;
    }

    return (
      <div className="route-location-dropdown">
        {suggestions.map(
          (
            location,
            index
          ) => (
            <button
              key={`${location.name}-${location.country}-${location.latitude}-${location.longitude}-${index}`}
              type="button"
              className="route-location-option"
              onMouseDown={(event) =>
                event.preventDefault()
              }
              onClick={() =>
                handleLocationSelect(
                  locationType,
                  location
                )
              }
            >
              <MapPin
                size={17}
              />

              <span>
                <strong>
                  {location.name}
                </strong>

                {location.country && (
                  <small>
                    {location.country}
                  </small>
                )}
              </span>
            </button>
          )
        )}
      </div>
    );
  };

  return (
    <form
      ref={formRef}
      className="route-search-form"
      onSubmit={handleSubmit}
    >
      <div className="route-search-fields">
        <div className="route-location-field">
          <label htmlFor="route-origin">
            <span>FROM</span>
          </label>

          <div className="route-input-container">
            <div
              className={`route-input-wrapper ${
                activeLocation ===
                "origin"
                  ? "location-active"
                  : ""
              }`}
            >
              <MapPin size={19} />

              <input
                id="route-origin"
                type="text"
                value={origin}
                onChange={(event) =>
                  handleLocationChange(
                    setOrigin,
                    event.target.value,
                    "origin"
                  )
                }
                onFocus={() =>
                  setActiveLocation(
                    "origin"
                  )
                }
                placeholder="City, address, airport or station"
                autoComplete="off"
                disabled={loading}
                required
              />

              {originLoading && (
                <Loader2
                  size={17}
                  className="route-location-spinner"
                />
              )}
            </div>

            {renderLocationSuggestions(
              "origin",
              originSuggestions
            )}
          </div>
        </div>

        <div
          className="route-search-arrow"
          aria-hidden="true"
        >
          <ArrowRight size={20} />
        </div>

        <div className="route-location-field">
          <label htmlFor="route-destination">
            <span>TO</span>
          </label>

          <div className="route-input-container">
            <div
              className={`route-input-wrapper ${
                activeLocation ===
                "destination"
                  ? "location-active"
                  : ""
              }`}
            >
              <MapPin size={19} />

              <input
                id="route-destination"
                type="text"
                value={destination}
                onChange={(event) =>
                  handleLocationChange(
                    setDestination,
                    event.target.value,
                    "destination"
                  )
                }
                onFocus={() =>
                  setActiveLocation(
                    "destination"
                  )
                }
                placeholder="City, address, airport or station"
                autoComplete="off"
                disabled={loading}
                required
              />

              {destinationLoading && (
                <Loader2
                  size={17}
                  className="route-location-spinner"
                />
              )}
            </div>

            {renderLocationSuggestions(
              "destination",
              destinationSuggestions
            )}
          </div>
        </div>
      </div>

      <div className="route-trip-details">
        <div className="route-trip-field">
          <label htmlFor="route-departure-date">
            <span>DEPARTURE DATE</span>
          </label>

          <input
            id="route-departure-date"
            type="date"
            value={departureDate}
            min={getToday()}
            onChange={(event) => {
              setDepartureDate(
                event.target.value
              );
              setValidationError("");
            }}
            disabled={loading}
          />
        </div>

        <div className="route-trip-field">
          <label htmlFor="route-departure-time">
            <span>DEPARTURE TIME</span>
          </label>

          <input
            id="route-departure-time"
            type="time"
            value={departureTime}
            onChange={(event) => {
              setDepartureTime(
                event.target.value
              );
              setValidationError("");
            }}
            disabled={loading}
          />
        </div>

        <button
          type="button"
          className="route-now-button"
          onClick={handleUseNow}
          disabled={loading}
        >
          Use now
        </button>
      </div>

      <div className="route-transport-section">
        <div className="route-transport-heading">
          <div>
            <span>TRAVEL MODE</span>

            <small>
              Choose how you want to travel
            </small>
          </div>

          <strong>
            {TRANSPORT_MODES.find(
              (mode) =>
                mode.value ===
                transportMode
            )?.label || "Car"}
          </strong>
        </div>

        <div className="route-transport-groups">
          {[
            "Road",
            "Public",
            "Air & water",
          ].map((group) => {
            const modes =
              TRANSPORT_MODES.filter(
                (mode) =>
                  mode.group === group
              );

            return (
              <div
                key={group}
                className="route-transport-group"
              >
                <span className="route-transport-group-label">
                  {group}
                </span>

                <div className="route-transport-options">
                  {modes.map(
                    ({
                      value,
                      label,
                      icon: Icon,
                    }) => (
                      <button
                        key={value}
                        type="button"
                        className={`transport-option ${
                          transportMode ===
                          value
                            ? "active"
                            : ""
                        }`}
                        onClick={() =>
                          handleTransportChange(
                            value
                          )
                        }
                        disabled={loading}
                        aria-pressed={
                          transportMode ===
                          value
                        }
                      >
                        <Icon size={16} />

                        <span>
                          {label}
                        </span>
                      </button>
                    )
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {validationError && (
        <div
          className="route-search-validation"
          role="alert"
        >
          {validationError}
        </div>
      )}

      <div className="route-search-submit-row">
        <button
          type="submit"
          className="route-search-submit"
          disabled={loading}
        >
          {loading ? (
            <>
              <Loader2
                size={18}
                className="route-search-spinner"
              />

              <span>
                Analyzing journey...
              </span>
            </>
          ) : (
            <>
              <span>
                Plan my journey
              </span>

              <ArrowRight
                size={19}
              />
            </>
          )}
        </button>
      </div>
    </form>
  );
}

export default SearchForm;
