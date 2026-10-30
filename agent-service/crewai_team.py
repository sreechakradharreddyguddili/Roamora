"""Optional CrewAI specialist team for a structured travel-plan draft."""
from __future__ import annotations

import os

from crewai import Agent, Crew, LLM, Process, Task
from crewai.tools import tool

from roamora_tools import search_nearby_places, search_transport_hubs


@tool("Search mapped places")
def crew_search_places(latitude: float, longitude: float, category: str = "ATTRACTION") -> str:
    """Search real mapped places near coordinates. Results may not include ratings or prices."""
    return search_nearby_places(latitude, longitude, category, 12, 20)


@tool("Search mapped transport hubs")
def crew_search_transport(city: str, country: str = "") -> str:
    """Search mapped transport hubs in a city. This does not imply live schedules."""
    return search_transport_hubs(city, country)


def run_crew_plan(question: str, journey_context: str = "") -> str:
    api_key = os.getenv("ROAMORA_AI_API_KEY", "").strip()
    if not api_key:
        raise RuntimeError("ROAMORA_AI_API_KEY is not configured for CrewAI.")
    base_url = os.getenv("ROAMORA_AI_BASE_URL", "https://api.openai.com/v1").rstrip("/")
    if base_url.endswith("/chat/completions"):
        base_url = base_url[: -len("/chat/completions")]
    model_name = os.getenv("ROAMORA_AI_MODEL", "gpt-4o-mini")
    llm = LLM(model=f"openai/{model_name}", api_key=api_key, base_url=base_url, temperature=0.2)

    researcher = Agent(
        role="Destination Researcher",
        goal="Identify practical destination highlights using verified mapped-place results.",
        backstory="You are a careful travel researcher. You distinguish provider facts from general suggestions.",
        tools=[crew_search_places], llm=llm, verbose=False, allow_delegation=False,
    )
    mobility = Agent(
        role="Mobility Researcher",
        goal="Identify transport hubs and explain what timetable data is and is not available.",
        backstory="You never fabricate departures, arrivals, vehicle locations or live delays.",
        tools=[crew_search_transport], llm=llm, verbose=False, allow_delegation=False,
    )
    planner = Agent(
        role="Travel Plan Coordinator",
        goal="Combine research into a clear, feasible draft and identify missing information.",
        backstory="You are a practical travel planner who clearly labels assumptions and unverified details.",
        llm=llm, verbose=False, allow_delegation=False,
    )

    research_task = Task(
        description=("Research destination places for this request. Use mapped-place tools only when the user supplied usable coordinates in the context. "
                     "If coordinates are absent, explain what is needed instead of inventing results. Request: {request}\nContext: {context}"),
        expected_output="A concise list of verified mapped-place results or an explicit data limitation.",
        agent=researcher,
    )
    mobility_task = Task(
        description=("Research transport hubs for the destination in the request. Do not invent timetables or live positions. "
                     "Request: {request}\nContext: {context}"),
        expected_output="Mapped transport hub findings and a clear note about schedule/live-feed availability.",
        agent=mobility,
    )
    plan_task = Task(
        description=("Create a practical draft using the preceding research. Separate confirmed data, suggestions and unknowns. "
                     "Never claim a booking was made. User request: {request}\nJourney context: {context}"),
        expected_output="A structured travel plan with destination ideas, mobility notes, budget caveats and missing details.",
        agent=planner,
        context=[research_task, mobility_task],
    )
    crew = Crew(agents=[researcher, mobility, planner], tasks=[research_task, mobility_task, plan_task], process=Process.sequential, verbose=False)
    result = crew.kickoff(inputs={"request": question[:1200], "context": (journey_context or "No journey context supplied.")[:12000]})
    return str(result).strip()
