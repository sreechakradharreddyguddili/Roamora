"""Standalone MCP server exposing read-only Roamora travel tools over Streamable HTTP."""
import os
from mcp.server.fastmcp import FastMCP
from roamora_tools import search_nearby_places, get_weather, search_transport_hubs, get_global_transport_places

mcp = FastMCP(
    "Roamora Travel Tools",
    instructions="Read-only travel discovery tools. Provider data can be incomplete; never infer live schedules or prices.",
    host=os.getenv("ROAMORA_MCP_HOST", "127.0.0.1"),
    port=int(os.getenv("ROAMORA_MCP_PORT", "8001")),
)


@mcp.tool()
def nearby_places(latitude: float, longitude: float, category: str = "", radius_km: float = 8.0, limit: int = 20) -> str:
    """Find mapped hotels, resorts, restaurants, cafes and attractions near coordinates."""
    return search_nearby_places(latitude, longitude, category, radius_km, limit)


@mcp.tool()
def current_weather(latitude: float, longitude: float) -> str:
    """Get current weather from Roamora's configured weather provider."""
    return get_weather(latitude, longitude)


@mcp.tool()
def transport_hubs(city: str, country: str = "", query: str = "") -> str:
    """Find mapped railway stations, bus stops, metro stations, airports and ferry terminals."""
    return search_transport_hubs(city, country, query)


@mcp.tool()
def global_transport_directory(city: str, country: str = "", query: str = "") -> str:
    """Search the Roamora global transport directory; schedules require a real operator feed."""
    return get_global_transport_places(city, country, query)


if __name__ == "__main__":
    mcp.run(transport="streamable-http")
