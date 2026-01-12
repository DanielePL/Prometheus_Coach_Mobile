-- ============================================================================
-- PROMETHEUS COACH - NUTRITION TABLES SETUP
-- ============================================================================
-- Run this script in Supabase SQL Editor to create nutrition tracking tables
-- ============================================================================

-- ============================================================================
-- 1. NUTRITION GOALS TABLE
-- Coach-set nutrition targets for clients
-- ============================================================================
CREATE TABLE IF NOT EXISTS nutrition_goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    goal_type TEXT NOT NULL CHECK (goal_type IN ('cutting', 'bulking', 'maintenance', 'performance')),
    target_calories REAL NOT NULL CHECK (target_calories > 0),
    target_protein REAL NOT NULL CHECK (target_protein >= 0),
    target_carbs REAL NOT NULL CHECK (target_carbs >= 0),
    target_fat REAL NOT NULL CHECK (target_fat >= 0),
    meals_per_day INTEGER DEFAULT 3 CHECK (meals_per_day > 0 AND meals_per_day <= 10),
    start_date DATE DEFAULT CURRENT_DATE,
    end_date DATE,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index for fast lookup of active goals
CREATE INDEX IF NOT EXISTS idx_nutrition_goals_user_active
ON nutrition_goals(user_id, is_active) WHERE is_active = true;

-- ============================================================================
-- 2. NUTRITION LOGS TABLE
-- Daily nutrition tracking entries
-- ============================================================================
CREATE TABLE IF NOT EXISTS nutrition_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    -- Optional daily overrides (if different from goal)
    target_calories REAL,
    target_protein REAL,
    target_carbs REAL,
    target_fat REAL,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    -- One log per user per day
    UNIQUE(user_id, date)
);

-- Index for date-based queries
CREATE INDEX IF NOT EXISTS idx_nutrition_logs_user_date
ON nutrition_logs(user_id, date DESC);

-- ============================================================================
-- 3. MEALS TABLE
-- Individual meals within a nutrition log
-- ============================================================================
CREATE TABLE IF NOT EXISTS meals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nutrition_log_id UUID NOT NULL REFERENCES nutrition_logs(id) ON DELETE CASCADE,
    meal_type TEXT NOT NULL CHECK (meal_type IN ('breakfast', 'lunch', 'dinner', 'snack', 'shake')),
    meal_name TEXT,
    time TIME,
    photo_url TEXT,
    -- AI meal recognition fields (future feature)
    ai_analysis_id UUID,
    ai_confidence REAL CHECK (ai_confidence >= 0 AND ai_confidence <= 1),
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index for log-based queries
CREATE INDEX IF NOT EXISTS idx_meals_log
ON meals(nutrition_log_id);

-- ============================================================================
-- 4. MEAL ITEMS TABLE
-- Individual food items within a meal
-- ============================================================================
CREATE TABLE IF NOT EXISTS meal_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    meal_id UUID NOT NULL REFERENCES meals(id) ON DELETE CASCADE,
    food_id UUID, -- Reference to food database (future feature)
    item_name TEXT NOT NULL,
    quantity REAL NOT NULL CHECK (quantity > 0),
    quantity_unit TEXT DEFAULT 'g',
    -- Macros (required)
    calories REAL NOT NULL CHECK (calories >= 0),
    protein REAL NOT NULL CHECK (protein >= 0),
    carbs REAL NOT NULL CHECK (carbs >= 0),
    fat REAL NOT NULL CHECK (fat >= 0),
    -- Micros (optional)
    fiber REAL CHECK (fiber >= 0),
    sugar REAL CHECK (sugar >= 0),
    saturated_fat REAL CHECK (saturated_fat >= 0),
    sodium REAL CHECK (sodium >= 0),
    potassium REAL CHECK (potassium >= 0),
    calcium REAL CHECK (calcium >= 0),
    iron REAL CHECK (iron >= 0),
    vitamin_a REAL CHECK (vitamin_a >= 0),
    vitamin_c REAL CHECK (vitamin_c >= 0),
    vitamin_d REAL CHECK (vitamin_d >= 0),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index for meal-based queries
CREATE INDEX IF NOT EXISTS idx_meal_items_meal
ON meal_items(meal_id);

-- ============================================================================
-- 5. ROW LEVEL SECURITY (RLS) POLICIES
-- ============================================================================

-- Enable RLS on all tables
ALTER TABLE nutrition_goals ENABLE ROW LEVEL SECURITY;
ALTER TABLE nutrition_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE meals ENABLE ROW LEVEL SECURITY;
ALTER TABLE meal_items ENABLE ROW LEVEL SECURITY;

-- ---------------------------------------------------------------------------
-- NUTRITION_GOALS Policies
-- ---------------------------------------------------------------------------

-- Clients can read their own goals
CREATE POLICY "Users can view own nutrition goals"
ON nutrition_goals FOR SELECT
TO authenticated
USING (user_id = auth.uid());

-- Coaches can read goals for their clients
CREATE POLICY "Coaches can view client nutrition goals"
ON nutrition_goals FOR SELECT
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM coach_client_connections
        WHERE coach_id = auth.uid()
        AND user_id = nutrition_goals.user_id
        AND status = 'accepted'
    )
);

-- Coaches can create goals for their clients
CREATE POLICY "Coaches can create client nutrition goals"
ON nutrition_goals FOR INSERT
TO authenticated
WITH CHECK (
    EXISTS (
        SELECT 1 FROM coach_client_connections
        WHERE coach_id = auth.uid()
        AND user_id = nutrition_goals.user_id
        AND status = 'accepted'
    )
);

-- Coaches can update goals for their clients
CREATE POLICY "Coaches can update client nutrition goals"
ON nutrition_goals FOR UPDATE
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM coach_client_connections
        WHERE coach_id = auth.uid()
        AND user_id = nutrition_goals.user_id
        AND status = 'accepted'
    )
)
WITH CHECK (
    EXISTS (
        SELECT 1 FROM coach_client_connections
        WHERE coach_id = auth.uid()
        AND user_id = nutrition_goals.user_id
        AND status = 'accepted'
    )
);

-- ---------------------------------------------------------------------------
-- NUTRITION_LOGS Policies
-- ---------------------------------------------------------------------------

-- Users can manage their own logs
CREATE POLICY "Users can view own nutrition logs"
ON nutrition_logs FOR SELECT
TO authenticated
USING (user_id = auth.uid());

CREATE POLICY "Users can create own nutrition logs"
ON nutrition_logs FOR INSERT
TO authenticated
WITH CHECK (user_id = auth.uid());

CREATE POLICY "Users can update own nutrition logs"
ON nutrition_logs FOR UPDATE
TO authenticated
USING (user_id = auth.uid())
WITH CHECK (user_id = auth.uid());

CREATE POLICY "Users can delete own nutrition logs"
ON nutrition_logs FOR DELETE
TO authenticated
USING (user_id = auth.uid());

-- Coaches can read logs for their clients
CREATE POLICY "Coaches can view client nutrition logs"
ON nutrition_logs FOR SELECT
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM coach_client_connections
        WHERE coach_id = auth.uid()
        AND user_id = nutrition_logs.user_id
        AND status = 'accepted'
    )
);

-- ---------------------------------------------------------------------------
-- MEALS Policies
-- ---------------------------------------------------------------------------

-- Users can manage meals in their own logs
CREATE POLICY "Users can view own meals"
ON meals FOR SELECT
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM nutrition_logs
        WHERE nutrition_logs.id = meals.nutrition_log_id
        AND nutrition_logs.user_id = auth.uid()
    )
);

CREATE POLICY "Users can create own meals"
ON meals FOR INSERT
TO authenticated
WITH CHECK (
    EXISTS (
        SELECT 1 FROM nutrition_logs
        WHERE nutrition_logs.id = meals.nutrition_log_id
        AND nutrition_logs.user_id = auth.uid()
    )
);

CREATE POLICY "Users can update own meals"
ON meals FOR UPDATE
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM nutrition_logs
        WHERE nutrition_logs.id = meals.nutrition_log_id
        AND nutrition_logs.user_id = auth.uid()
    )
);

CREATE POLICY "Users can delete own meals"
ON meals FOR DELETE
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM nutrition_logs
        WHERE nutrition_logs.id = meals.nutrition_log_id
        AND nutrition_logs.user_id = auth.uid()
    )
);

-- Coaches can read meals for their clients
CREATE POLICY "Coaches can view client meals"
ON meals FOR SELECT
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM nutrition_logs
        JOIN coach_client_connections ON coach_client_connections.user_id = nutrition_logs.user_id
        WHERE nutrition_logs.id = meals.nutrition_log_id
        AND coach_client_connections.coach_id = auth.uid()
        AND coach_client_connections.status = 'accepted'
    )
);

-- ---------------------------------------------------------------------------
-- MEAL_ITEMS Policies
-- ---------------------------------------------------------------------------

-- Users can manage items in their own meals
CREATE POLICY "Users can view own meal items"
ON meal_items FOR SELECT
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM meals
        JOIN nutrition_logs ON nutrition_logs.id = meals.nutrition_log_id
        WHERE meals.id = meal_items.meal_id
        AND nutrition_logs.user_id = auth.uid()
    )
);

CREATE POLICY "Users can create own meal items"
ON meal_items FOR INSERT
TO authenticated
WITH CHECK (
    EXISTS (
        SELECT 1 FROM meals
        JOIN nutrition_logs ON nutrition_logs.id = meals.nutrition_log_id
        WHERE meals.id = meal_items.meal_id
        AND nutrition_logs.user_id = auth.uid()
    )
);

CREATE POLICY "Users can update own meal items"
ON meal_items FOR UPDATE
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM meals
        JOIN nutrition_logs ON nutrition_logs.id = meals.nutrition_log_id
        WHERE meals.id = meal_items.meal_id
        AND nutrition_logs.user_id = auth.uid()
    )
);

CREATE POLICY "Users can delete own meal items"
ON meal_items FOR DELETE
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM meals
        JOIN nutrition_logs ON nutrition_logs.id = meals.nutrition_log_id
        WHERE meals.id = meal_items.meal_id
        AND nutrition_logs.user_id = auth.uid()
    )
);

-- Coaches can read meal items for their clients
CREATE POLICY "Coaches can view client meal items"
ON meal_items FOR SELECT
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM meals
        JOIN nutrition_logs ON nutrition_logs.id = meals.nutrition_log_id
        JOIN coach_client_connections ON coach_client_connections.user_id = nutrition_logs.user_id
        WHERE meals.id = meal_items.meal_id
        AND coach_client_connections.coach_id = auth.uid()
        AND coach_client_connections.status = 'accepted'
    )
);

-- ============================================================================
-- 6. UPDATED_AT TRIGGERS
-- ============================================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for nutrition_goals
DROP TRIGGER IF EXISTS update_nutrition_goals_updated_at ON nutrition_goals;
CREATE TRIGGER update_nutrition_goals_updated_at
    BEFORE UPDATE ON nutrition_goals
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger for nutrition_logs
DROP TRIGGER IF EXISTS update_nutrition_logs_updated_at ON nutrition_logs;
CREATE TRIGGER update_nutrition_logs_updated_at
    BEFORE UPDATE ON nutrition_logs
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 7. HELPER FUNCTION: Get or create today's nutrition log
-- ============================================================================
CREATE OR REPLACE FUNCTION get_or_create_nutrition_log(p_user_id UUID)
RETURNS UUID AS $$
DECLARE
    v_log_id UUID;
BEGIN
    -- Try to get existing log for today
    SELECT id INTO v_log_id
    FROM nutrition_logs
    WHERE user_id = p_user_id AND date = CURRENT_DATE;

    -- Create if not exists
    IF v_log_id IS NULL THEN
        INSERT INTO nutrition_logs (user_id, date)
        VALUES (p_user_id, CURRENT_DATE)
        RETURNING id INTO v_log_id;
    END IF;

    RETURN v_log_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================================================
-- DONE!
-- ============================================================================
-- Tables created:
--   - nutrition_goals (coach-set targets)
--   - nutrition_logs (daily logs)
--   - meals (meal entries)
--   - meal_items (food items)
--
-- RLS Policies:
--   - Users can manage their own nutrition data
--   - Coaches can view and set goals for their clients
--   - Coaches can view client nutrition logs (read-only)
-- ============================================================================
