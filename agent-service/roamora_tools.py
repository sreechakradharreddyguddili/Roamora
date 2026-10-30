"""Read-only tools shared by LangChain/LangGraph, CrewAI and the MCP server."""
from __future__ import annotations

import json
import os
from typing import Any
from urllib.parse import urlencode

import requests

BACKEND_URL = os.getenv("ROAMORA_BACKEND_URL", "http://localhost:8080").rstrip("/")
TIMEOUT = (3.0, 12.0)


def _get(path: str, params: dict[str, Any]) -> str:
    try:
        response = requests.get(f"{BACKEND_URL}{path}", params=params, timeout=TIMEOUT)
        response.raise_for_status()
        payload = response.json()
        # Bound context size so external map/provider responses cannot flood the model.
        return json.dumps(payload, ensure_ascii=False, default=str)[:14000]
    except requests.RequestException as exc:
        return json.dumps({"available": False, "source": path, "error": str(exc)[:500]})
    except ValueError:
        return json.dumps({"available": False, "source": path, "error": "Backend returned non-JSON data."})


def search_nearby_places(latitude: float, longitude: float, category: str = "", radius_km: float = 8.0, limit: int = 20) -> str:
    """Search Roamora's mapped nearby places by coordinates and optional category. Category examples: HOTEL, RESTAURANT, CAFE, ATTRACTION."""
    params: dict[str, Any] = {"latitude": latitude, "longitude": longitude, "radiusKm": max(1, min(radius_km, 25)), "limit": max(1, min(limit, 25))}
    if category.strip():
        params["category"] = category.strip().upper()
    return _get("/api/v1/places/nearby", params)


def get_weather(latitude: float, longitude: float) -> str:
    """Get current weather from Roamora's configured weather provider for coordinates."""
    return _get("/api/v1/weather", {"latitude": latitude, "longitude": longitude})


def search_transport_hubs(city: str, country: str = "", query: str = "") -> str:
    """Search Roamora's mapped transport hubs by city, optional country and station name."""
    params: dict[str, Any] = {"city": city.strip()}
    if country.strip():
        params["country"] = country.strip()
    if query.strip():
        params["query"] = query.strip()
    return _get("/api/v1/transport-places", params)


def get_global_transport_places(city: str, country: str = "", query: str = "") -> str:
    """Search Roamora's read-only global transport place directory when available."""
    params: dict[str, Any] = {}
    if city.strip():
        params["city"] = city.strip()
    if country.strip():
        params["country"] = country.strip()
    if query.strip():
        params["query"] = query.strip()
    return _get("/api/v1/global-transport-places", params)
