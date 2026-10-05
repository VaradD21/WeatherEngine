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

-- Health-conscious
INSERT INTO persona_widgets (persona_id, widget_code, display_order)
SELECT id, w.code, w.ord FROM personas,
    (VALUES ('aqi_card', 1), ('pollen_card', 2), ('uv_index_card', 3), ('humidity_card', 4)) AS w(code, ord)
WHERE personas.code = 'health_conscious'
ON CONFLICT DO NOTHING;

-- Outdoor fitness
INSERT INTO persona_widgets (persona_id, widget_code, display_order)
SELECT id, w.code, w.ord FROM personas,
    (VALUES ('sunrise_sunset_card', 1), ('best_running_hours_card', 2), ('wind_speed_card', 3), ('heat_alert_card', 4)) AS w(code, ord)
WHERE personas.code = 'outdoor_fitness'
ON CONFLICT DO NOTHING;

-- Commuter
INSERT INTO persona_widgets (persona_id, widget_code, display_order)
SELECT id, w.code, w.ord FROM personas,
    (VALUES ('traffic_card', 1), ('visibility_card', 2), ('storm_fog_alert_card', 3)) AS w(code, ord)
WHERE personas.code = 'commuter'
ON CONFLICT DO NOTHING;

-- Beachgoer / surfer
INSERT INTO persona_widgets (persona_id, widget_code, display_order)
SELECT id, w.code, w.ord FROM personas,
    (VALUES ('tide_card', 1), ('wave_height_card', 2), ('sea_temp_card', 3)) AS w(code, ord)
WHERE personas.code = 'beachgoer'
ON CONFLICT DO NOTHING;

-- Traveler
INSERT INTO persona_widgets (persona_id, widget_code, display_order)
SELECT id, w.code, w.ord FROM personas,
    (VALUES ('saved_destinations_card', 1), ('flight_alert_card', 2), ('packing_suggestion_card', 3)) AS w(code, ord)
WHERE personas.code = 'traveler'
ON CONFLICT DO NOTHING;

-- Parent / family
INSERT INTO persona_widgets (persona_id, widget_code, display_order)
SELECT id, w.code, w.ord FROM personas,
    (VALUES ('school_commute_card', 1), ('rain_alert_card', 2), ('severe_weather_card', 3)) AS w(code, ord)
WHERE personas.code = 'parent'
ON CONFLICT DO NOTHING;

-- Agriculture / gardener
INSERT INTO persona_widgets (persona_id, widget_code, display_order)
SELECT id, w.code, w.ord FROM personas,
    (VALUES ('soil_moisture_card', 1), ('rainfall_prediction_card', 2), ('frost_alert_card', 3), ('planting_guidance_card', 4)) AS w(code, ord)
WHERE personas.code = 'gardener'
ON CONFLICT DO NOTHING;

-- Event planner
INSERT INTO persona_widgets (persona_id, widget_code, display_order)
SELECT id, w.code, w.ord FROM personas,
    (VALUES ('extended_forecast_card', 1), ('rain_probability_card', 2), ('comfort_index_card', 3)) AS w(code, ord)
WHERE personas.code = 'event_planner'
ON CONFLICT DO NOTHING;
