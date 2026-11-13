"""Expand phrases.yaml x voices.yaml into the list of clips to render."""
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class Clip:
    key: str
    kind: str  # "minutes" | "target" | "late"
    tier: str | None
    variant: int
    minutes: int
    voice: str
    text: str


def clip_key(kind: str, tier: str | None, variant: int, minutes: int, voice: str) -> str:
    """The file name (without extension) the app looks clips up by. Changing it breaks the app."""
    if kind == "minutes":
        return f"minutes_{tier}_{variant}_{minutes:02d}_{voice}"
    return f"{kind}_{variant}_{voice}"


def _fill(template: str, minutes: int) -> str:
    return template.format(minutes=minutes, unit="minute" if minutes == 1 else "minutes")


def build_manifest(phrases: dict, voices: list[dict]) -> list[Clip]:
    clips: list[Clip] = []
    for voice in (v["id"] for v in voices):
        for tier, templates in phrases["tiers"].items():
            for minutes in phrases["minutes"][tier]:
                for variant, template in enumerate(templates):
                    clips.append(Clip(clip_key("minutes", tier, variant, minutes, voice),
                                      "minutes", tier, variant, minutes, voice, _fill(template, minutes)))
        for kind in ("target", "late"):
            for variant, text in enumerate(phrases[kind]):
                clips.append(Clip(clip_key(kind, None, variant, 0, voice), kind, None, variant, 0, voice, text))
    return clips
