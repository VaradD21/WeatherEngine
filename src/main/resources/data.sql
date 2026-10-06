INSERT INTO personas (code, display_name) VALUES
    ('health_conscious', 'Health-conscious'),
    ('outdoor_fitness', 'Outdoor Fitness Enthusiast'),
    ('commuter', 'Commuter'),
    ('beachgoer', 'Beachgoer & Surfer'),
    ('traveler', 'Traveler'),
    ('parent', 'Parent & Family'),
    ('gardener', 'Agriculture & Gardener'),
    ('event_planner', 'Event Planner')
ON CONFLICT (code) DO NOTHING;

INSERT INTO persona_widgets (persona_id, widget_code, display_order)
SELECT p.id, w.widget_code, w.ord
FROM personas p
JOIN (VALUES
    ('health_conscious', 'aqi_card', 1), ('health_conscious', 'pollen_card', 2), ('health_conscious', 'uv_index_card', 3), ('health_conscious', 'humidity_card', 4),
    ('outdoor_fitness', 'sunrise_sunset_card', 1), ('outdoor_fitness', 'best_running_hours_card', 2), ('outdoor_fitness', 'wind_speed_card', 3), ('outdoor_fitness', 'heat_alert_card', 4),
    ('commuter', 'traffic_card', 1), ('commuter', 'visibility_card', 2), ('commuter', 'storm_fog_alert_card', 3),
    ('beachgoer', 'tide_card', 1), ('beachgoer', 'wave_height_card', 2), ('beachgoer', 'sea_temp_card', 3),
    ('traveler', 'saved_destinations_card', 1), ('traveler', 'flight_alert_card', 2), ('traveler', 'packing_suggestion_card', 3),
    ('parent', 'school_commute_card', 1), ('parent', 'rain_alert_card', 2), ('parent', 'severe_weather_card', 3),
    ('gardener', 'soil_moisture_card', 1), ('gardener', 'rainfall_prediction_card', 2), ('gardener', 'frost_alert_card', 3), ('gardener', 'planting_guidance_card', 4),
    ('event_planner', 'extended_forecast_card', 1), ('event_planner', 'rain_probability_card', 2), ('event_planner', 'comfort_index_card', 3)
) AS w(persona_code, widget_code, ord) ON p.code = w.persona_code
ON CONFLICT DO NOTHING;
