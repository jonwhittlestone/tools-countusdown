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
    raise NotImplementedError("commit #9")


def build_manifest(phrases: dict, voices: list[dict]) -> list[Clip]:
    raise NotImplementedError("commit #9")
