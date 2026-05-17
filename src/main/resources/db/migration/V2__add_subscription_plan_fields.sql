-- Add structured feature columns to subscription_plans
ALTER TABLE subscription_plans
    ADD COLUMN IF NOT EXISTS max_services integer NOT NULL DEFAULT 10,
    ADD COLUMN IF NOT EXISTS max_featured_services integer NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS allow_featured boolean NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS allow_premium_badge boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS require_complete_profile boolean NOT NULL DEFAULT true;
