# WeatherEngine UI Test Screenshots Directory

This directory stores reference and manual verification screenshots for the WeatherEngine Android application across required visual themes, personas, and system states as specified in Section 8.10.

## Required Matrix of UI Screenshots:

1. `home_light_mode.png`: Home screen in standard Light theme with default metrics.
2. `home_dark_mode.png`: Home screen in Dark theme verifying text contrast (WCAG AA).
3. `home_persona_health_conscious.png`: Home screen with Health-Conscious persona (AQI, Pollen, UV, Humidity cards).
4. `home_persona_outdoor_fitness.png`: Home screen with Outdoor Fitness persona (Sunrise, Best Running Window, Wind, Heat Alert cards).
5. `home_persona_beachgoer.png`: Home screen with Beachgoer persona (Wave Height, Sea Temp, Tide cards).
6. `home_persona_traveler.png`: Home screen with Traveler persona (Flight Alert, Packing Suggestion, Saved Destinations cards).
7. `home_persona_parent.png`: Home screen with Parent persona (School Commute, Rain Alert, Severe Weather cards).
8. `home_persona_gardener.png`: Home screen with Gardener persona (Soil Moisture, Rainfall Prediction, Frost Alert, Planting Guidance cards).
9. `home_persona_event_planner.png`: Home screen with Event Planner persona (Extended Forecast, Rain Probability, Comfort Index cards).
10. `home_loading_skeleton.png`: Loading shimmer/skeleton state during initial weather bundle fetch.
11. `home_error_state.png`: Error state displaying friendly retry message when upstream network/API is down.
12. `home_empty_personas.png`: Zero-persona empty state with prompt to configure active personas.
13. `home_offline_banner.png`: Offline cached state showing stale data banner with last-updated timestamp.
