-- =====================================================
-- SYSTEM TEMPLATES SEED DATA
-- Run this in Supabase SQL Editor
-- =====================================================

-- 1. Clear existing system templates (optional - comment out if you want to keep existing)
-- DELETE FROM template_exercises WHERE template_id IN (SELECT id FROM workout_templates WHERE template_type = 'system');
-- DELETE FROM workout_templates WHERE template_type = 'system';
-- DELETE FROM template_categories;

-- 2. Insert Categories
INSERT INTO template_categories (id, name, icon, display_order) VALUES
    (gen_random_uuid(), 'Push/Pull/Legs', 'fitness_center', 1),
    (gen_random_uuid(), 'Full Body', 'accessibility', 2),
    (gen_random_uuid(), 'Upper/Lower', 'straighten', 3),
    (gen_random_uuid(), 'Bro Split', 'sports_gymnastics', 4),
    (gen_random_uuid(), 'Anfänger', 'school', 5),
    (gen_random_uuid(), 'Kraft/Powerlifting', 'fitness_center', 6)
ON CONFLICT (name) DO NOTHING;

-- 3. Insert System Templates

-- Push Day Classic
INSERT INTO workout_templates (
    id, coach_id, name, description, category_id, template_type, default_level,
    target_muscles, equipment, scaling_config
) VALUES (
    gen_random_uuid(),
    NULL, -- system template has no coach
    'Push Day Classic',
    'Klassisches Push-Workout: Brust, Schultern, Trizeps. Ideal für PPL-Split.',
    (SELECT id FROM template_categories WHERE name = 'Push/Pull/Legs'),
    'system',
    'intermediate',
    ARRAY['chest', 'shoulders', 'triceps'],
    ARRAY['barbell', 'dumbbells', 'cables'],
    '{"beginner": {"sets_multiplier": 0.7, "reps_multiplier": 0.8}, "intermediate": {"sets_multiplier": 1.0, "reps_multiplier": 1.0}, "advanced": {"sets_multiplier": 1.3, "reps_multiplier": 1.1}}'::jsonb
);

-- Pull Day Classic
INSERT INTO workout_templates (
    id, coach_id, name, description, category_id, template_type, default_level,
    target_muscles, equipment, scaling_config
) VALUES (
    gen_random_uuid(),
    NULL,
    'Pull Day Classic',
    'Klassisches Pull-Workout: Rücken, Bizeps, Hintere Schultern.',
    (SELECT id FROM template_categories WHERE name = 'Push/Pull/Legs'),
    'system',
    'intermediate',
    ARRAY['back', 'biceps', 'rear_delts'],
    ARRAY['barbell', 'dumbbells', 'cables', 'pullup_bar'],
    '{"beginner": {"sets_multiplier": 0.7, "reps_multiplier": 0.8}, "intermediate": {"sets_multiplier": 1.0, "reps_multiplier": 1.0}, "advanced": {"sets_multiplier": 1.3, "reps_multiplier": 1.1}}'::jsonb
);

-- Leg Day Classic
INSERT INTO workout_templates (
    id, coach_id, name, description, category_id, template_type, default_level,
    target_muscles, equipment, scaling_config
) VALUES (
    gen_random_uuid(),
    NULL,
    'Leg Day Classic',
    'Komplettes Beintraining: Quadrizeps, Hamstrings, Waden, Glutes.',
    (SELECT id FROM template_categories WHERE name = 'Push/Pull/Legs'),
    'system',
    'intermediate',
    ARRAY['quads', 'hamstrings', 'glutes', 'calves'],
    ARRAY['barbell', 'leg_press', 'machines'],
    '{"beginner": {"sets_multiplier": 0.7, "reps_multiplier": 0.8}, "intermediate": {"sets_multiplier": 1.0, "reps_multiplier": 1.0}, "advanced": {"sets_multiplier": 1.3, "reps_multiplier": 1.1}}'::jsonb
);

-- Full Body A
INSERT INTO workout_templates (
    id, coach_id, name, description, category_id, template_type, default_level,
    target_muscles, equipment, scaling_config
) VALUES (
    gen_random_uuid(),
    NULL,
    'Full Body A',
    'Ganzkörper-Workout mit Fokus auf Grundübungen. Perfekt für 3x pro Woche.',
    (SELECT id FROM template_categories WHERE name = 'Full Body'),
    'system',
    'beginner',
    ARRAY['chest', 'back', 'legs', 'shoulders'],
    ARRAY['barbell', 'dumbbells'],
    '{"beginner": {"sets_multiplier": 1.0, "reps_multiplier": 1.0}, "intermediate": {"sets_multiplier": 1.2, "reps_multiplier": 1.0}, "advanced": {"sets_multiplier": 1.4, "reps_multiplier": 1.1}}'::jsonb
);

-- Full Body B
INSERT INTO workout_templates (
    id, coach_id, name, description, category_id, template_type, default_level,
    target_muscles, equipment, scaling_config
) VALUES (
    gen_random_uuid(),
    NULL,
    'Full Body B',
    'Ganzkörper-Variante B mit alternativen Übungen. Wechsel mit Full Body A.',
    (SELECT id FROM template_categories WHERE name = 'Full Body'),
    'system',
    'beginner',
    ARRAY['chest', 'back', 'legs', 'arms'],
    ARRAY['barbell', 'dumbbells', 'cables'],
    '{"beginner": {"sets_multiplier": 1.0, "reps_multiplier": 1.0}, "intermediate": {"sets_multiplier": 1.2, "reps_multiplier": 1.0}, "advanced": {"sets_multiplier": 1.4, "reps_multiplier": 1.1}}'::jsonb
);

-- Upper Body
INSERT INTO workout_templates (
    id, coach_id, name, description, category_id, template_type, default_level,
    target_muscles, equipment, scaling_config
) VALUES (
    gen_random_uuid(),
    NULL,
    'Upper Body',
    'Komplettes Oberkörper-Workout für Upper/Lower Split.',
    (SELECT id FROM template_categories WHERE name = 'Upper/Lower'),
    'system',
    'intermediate',
    ARRAY['chest', 'back', 'shoulders', 'arms'],
    ARRAY['barbell', 'dumbbells', 'cables'],
    '{"beginner": {"sets_multiplier": 0.7, "reps_multiplier": 0.8}, "intermediate": {"sets_multiplier": 1.0, "reps_multiplier": 1.0}, "advanced": {"sets_multiplier": 1.2, "reps_multiplier": 1.1}}'::jsonb
);

-- Lower Body
INSERT INTO workout_templates (
    id, coach_id, name, description, category_id, template_type, default_level,
    target_muscles, equipment, scaling_config
) VALUES (
    gen_random_uuid(),
    NULL,
    'Lower Body',
    'Komplettes Unterkörper-Workout für Upper/Lower Split.',
    (SELECT id FROM template_categories WHERE name = 'Upper/Lower'),
    'system',
    'intermediate',
    ARRAY['quads', 'hamstrings', 'glutes', 'calves'],
    ARRAY['barbell', 'leg_press', 'machines'],
    '{"beginner": {"sets_multiplier": 0.7, "reps_multiplier": 0.8}, "intermediate": {"sets_multiplier": 1.0, "reps_multiplier": 1.0}, "advanced": {"sets_multiplier": 1.2, "reps_multiplier": 1.1}}'::jsonb
);

-- Chest Day (Bro Split)
INSERT INTO workout_templates (
    id, coach_id, name, description, category_id, template_type, default_level,
    target_muscles, equipment, scaling_config
) VALUES (
    gen_random_uuid(),
    NULL,
    'Chest Day',
    'Intensives Brust-Workout mit hohem Volumen.',
    (SELECT id FROM template_categories WHERE name = 'Bro Split'),
    'system',
    'intermediate',
    ARRAY['chest'],
    ARRAY['barbell', 'dumbbells', 'cables', 'machines'],
    '{"beginner": {"sets_multiplier": 0.6, "reps_multiplier": 0.8}, "intermediate": {"sets_multiplier": 1.0, "reps_multiplier": 1.0}, "advanced": {"sets_multiplier": 1.3, "reps_multiplier": 1.1}}'::jsonb
);

-- Back Day (Bro Split)
INSERT INTO workout_templates (
    id, coach_id, name, description, category_id, template_type, default_level,
    target_muscles, equipment, scaling_config
) VALUES (
    gen_random_uuid(),
    NULL,
    'Back Day',
    'Komplettes Rücken-Workout für maximale Breite und Dicke.',
    (SELECT id FROM template_categories WHERE name = 'Bro Split'),
    'system',
    'intermediate',
    ARRAY['back', 'rear_delts'],
    ARRAY['barbell', 'dumbbells', 'cables', 'pullup_bar'],
    '{"beginner": {"sets_multiplier": 0.6, "reps_multiplier": 0.8}, "intermediate": {"sets_multiplier": 1.0, "reps_multiplier": 1.0}, "advanced": {"sets_multiplier": 1.3, "reps_multiplier": 1.1}}'::jsonb
);

-- Shoulder Day (Bro Split)
INSERT INTO workout_templates (
    id, coach_id, name, description, category_id, template_type, default_level,
    target_muscles, equipment, scaling_config
) VALUES (
    gen_random_uuid(),
    NULL,
    'Shoulder Day',
    '360° Schulter-Training: Front, Seite, hinten.',
    (SELECT id FROM template_categories WHERE name = 'Bro Split'),
    'system',
    'intermediate',
    ARRAY['shoulders'],
    ARRAY['barbell', 'dumbbells', 'cables'],
    '{"beginner": {"sets_multiplier": 0.6, "reps_multiplier": 0.8}, "intermediate": {"sets_multiplier": 1.0, "reps_multiplier": 1.0}, "advanced": {"sets_multiplier": 1.3, "reps_multiplier": 1.1}}'::jsonb
);

-- Arm Day (Bro Split)
INSERT INTO workout_templates (
    id, coach_id, name, description, category_id, template_type, default_level,
    target_muscles, equipment, scaling_config
) VALUES (
    gen_random_uuid(),
    NULL,
    'Arm Day',
    'Bizeps und Trizeps isoliert für maximales Wachstum.',
    (SELECT id FROM template_categories WHERE name = 'Bro Split'),
    'system',
    'intermediate',
    ARRAY['biceps', 'triceps'],
    ARRAY['barbell', 'dumbbells', 'cables'],
    '{"beginner": {"sets_multiplier": 0.6, "reps_multiplier": 0.8}, "intermediate": {"sets_multiplier": 1.0, "reps_multiplier": 1.0}, "advanced": {"sets_multiplier": 1.3, "reps_multiplier": 1.1}}'::jsonb
);

-- Anfänger Ganzkörper
INSERT INTO workout_templates (
    id, coach_id, name, description, category_id, template_type, default_level,
    target_muscles, equipment, scaling_config
) VALUES (
    gen_random_uuid(),
    NULL,
    'Anfänger Ganzkörper',
    'Perfekter Einstieg: Einfache Übungen, wenig Volumen, hoher Lerneffekt.',
    (SELECT id FROM template_categories WHERE name = 'Anfänger'),
    'system',
    'beginner',
    ARRAY['chest', 'back', 'legs', 'shoulders'],
    ARRAY['machines', 'dumbbells'],
    '{"beginner": {"sets_multiplier": 1.0, "reps_multiplier": 1.0}, "intermediate": {"sets_multiplier": 1.3, "reps_multiplier": 1.0}, "advanced": {"sets_multiplier": 1.5, "reps_multiplier": 1.0}}'::jsonb
);

-- Kraft-Fokus: Squat Day
INSERT INTO workout_templates (
    id, coach_id, name, description, category_id, template_type, default_level,
    target_muscles, equipment, scaling_config
) VALUES (
    gen_random_uuid(),
    NULL,
    'Squat Day',
    'Kraftaufbau-Fokus auf Kniebeuge mit Assistenzübungen.',
    (SELECT id FROM template_categories WHERE name = 'Kraft/Powerlifting'),
    'system',
    'advanced',
    ARRAY['quads', 'glutes', 'core'],
    ARRAY['barbell', 'squat_rack'],
    '{"beginner": {"sets_multiplier": 0.6, "reps_multiplier": 1.0}, "intermediate": {"sets_multiplier": 0.8, "reps_multiplier": 1.0}, "advanced": {"sets_multiplier": 1.0, "reps_multiplier": 1.0}}'::jsonb
);

-- Kraft-Fokus: Bench Day
INSERT INTO workout_templates (
    id, coach_id, name, description, category_id, template_type, default_level,
    target_muscles, equipment, scaling_config
) VALUES (
    gen_random_uuid(),
    NULL,
    'Bench Day',
    'Kraftaufbau-Fokus auf Bankdrücken mit Assistenzübungen.',
    (SELECT id FROM template_categories WHERE name = 'Kraft/Powerlifting'),
    'system',
    'advanced',
    ARRAY['chest', 'triceps', 'shoulders'],
    ARRAY['barbell', 'bench'],
    '{"beginner": {"sets_multiplier": 0.6, "reps_multiplier": 1.0}, "intermediate": {"sets_multiplier": 0.8, "reps_multiplier": 1.0}, "advanced": {"sets_multiplier": 1.0, "reps_multiplier": 1.0}}'::jsonb
);

-- Kraft-Fokus: Deadlift Day
INSERT INTO workout_templates (
    id, coach_id, name, description, category_id, template_type, default_level,
    target_muscles, equipment, scaling_config
) VALUES (
    gen_random_uuid(),
    NULL,
    'Deadlift Day',
    'Kraftaufbau-Fokus auf Kreuzheben mit Assistenzübungen.',
    (SELECT id FROM template_categories WHERE name = 'Kraft/Powerlifting'),
    'system',
    'advanced',
    ARRAY['back', 'hamstrings', 'glutes', 'core'],
    ARRAY['barbell'],
    '{"beginner": {"sets_multiplier": 0.6, "reps_multiplier": 1.0}, "intermediate": {"sets_multiplier": 0.8, "reps_multiplier": 1.0}, "advanced": {"sets_multiplier": 1.0, "reps_multiplier": 1.0}}'::jsonb
);

-- 4. VERIFY RESULTS
SELECT
    tc.name as category,
    COUNT(wt.id) as template_count
FROM template_categories tc
LEFT JOIN workout_templates wt ON wt.category_id = tc.id
GROUP BY tc.name, tc.display_order
ORDER BY tc.display_order;

-- Show all templates
SELECT
    wt.name,
    wt.template_type,
    wt.default_level,
    tc.name as category,
    wt.target_muscles
FROM workout_templates wt
LEFT JOIN template_categories tc ON wt.category_id = tc.id
ORDER BY tc.display_order, wt.name;
