// Shared types between Worker and Android app contract.
// Keep this file in sync with the Kotlin model classes.

export type DurationClass = "few-hours" | "half-day" | "full-day" | "weekend";
export type GroupType = "couples" | "groups" | "solo" | "adventure" | "family";
export type Crowd = "low" | "medium" | "high";
export type TempPref = "cool" | "temperate" | "warm";

export interface UserPrefs {
  city: string;                   // e.g. "Bengaluru"
  durationClass: DurationClass;
  groupType: GroupType;
  vibe: string[];                 // e.g. ["scenic","quiet"]
  crowd: Crowd;
  tempPref: TempPref;
  stayRequired: boolean;
  monthIso: string;               // "Jan".."Dec"
  budget: number;                 // 1..4
  city_allowlist?: string[];      // optional override (e.g. include nearby getaway towns)
}

export interface Place {
  id: string;
  name: string;
  city: string;
  region: string;
  address: string;
  geo: { lat: number; lng: number; geohash: string };
  vibe: string[];
  crowd: Crowd;
  good_for: GroupType[];
  temp_class: TempPref;
  has_stay: boolean;
  duration_class: DurationClass;
  budget: number;
  season: string[];
  drive_time_from_blr_min: number;
  photo_urls: string[];
  short_summary: string;
  maps_query: string;
}

export interface RankedPlace {
  place_id: string;
  why: string;
}

export interface RecommendResponse {
  ranked: RankedPlace[];
  candidate_count: number;
  cached: boolean;
}

export interface Env {
  REC_CACHE?: KVNamespace;
  FIREBASE_PROJECT_ID: string;
  FIREBASE_API_KEY: string;
  GEMINI_API_KEY: string;
  APP_CHECK_PROJECT_NUMBER?: string;
}
