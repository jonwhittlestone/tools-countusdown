from pathlib import Path

import yaml

from countusdown_pipeline.manifest import build_manifest, clip_key

ROOT = Path(__file__).resolve().parents[1]
PHRASES = yaml.safe_load((ROOT / "phrases.yaml").read_text())
VOICES = yaml.safe_load((ROOT / "voices.yaml").read_text())["voices"]


def test_clip_keys_are_stable_and_app_friendly():
    # The Android app builds the same key to find a clip, so the format is a contract.
    assert clip_key("minutes", "firm", 2, 5, "emma") == "minutes_firm_2_05_emma"
    assert clip_key("target", None, 0, 0, "george") == "target_0_george"
    assert clip_key("late", None, 1, 0, "chatter") == "late_1_chatter"


def test_every_variant_minute_and_voice_is_enumerated():
    clips = build_manifest(PHRASES, VOICES)
    voices = len(VOICES)
    minute_clips = sum(len(PHRASES["minutes"][t]) * len(PHRASES["tiers"][t]) for t in PHRASES["tiers"])
    fixed = len(PHRASES["target"]) + len(PHRASES["late"])
    assert len(clips) == (minute_clips + fixed) * voices
    assert len({c.key for c in clips}) == len(clips), "keys must be unique"


def test_text_fills_in_minutes_with_singular_unit():
    clips = {c.key: c for c in build_manifest(PHRASES, VOICES)}
    assert clips["minutes_firm_0_01_emma"].text == "In 1 minute we must leave, or we will be late."
    assert clips["minutes_gentle_0_30_emma"].text == "Everyone, we leave in 30 minutes."
    assert "{" not in clips["target_0_emma"].text


def test_each_tier_only_says_its_own_minutes():
    clips = build_manifest(PHRASES, VOICES)
    assert {c.minutes for c in clips if c.tier == "firm"} == {1, 2, 3, 4, 5}
    assert {c.minutes for c in clips if c.tier == "nudge"} == {10}


def test_verify_score_is_order_insensitive_and_reads_numbers():
    from countusdown_pipeline.verify import score

    assert score("In 5 minutes we must leave.", "In five minutes, we must leave!") == 1.0
    assert score("Everyone, we leave in 30 minutes.", "Everyone we leave in minutes") < 1.0
