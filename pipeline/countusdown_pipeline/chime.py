"""Synthesise the warning chime: one soft "ding" as the v1.0 warning box appears.

    uv run python -m countusdown_pipeline.chime

Like the ambient bed it is built from maths, not samples: A5 with a quieter E6 above it,
a 3 ms attack so it doesn't click, and an exponential fade. Set by its peak, not loudness,
because it is mostly decay.
"""
from __future__ import annotations

import subprocess
from pathlib import Path

from .render import ASSETS
from .stems import peak_db

CHIME_SECONDS = 1.2
CHIME_PEAK_DB = -14.0
CHIME_PATH = ASSETS / "chime.ogg"

# Scaled by 0.5 so the raw sum stays under full scale: peak_db measures in s16, where anything
# louder clips at 0 dB and the level would come out a couple of dB too loud.
SOURCE = (
    "aevalsrc='0.5*(sin(2*PI*880*t)+0.5*sin(2*PI*1320*t))*(1-exp(-300*t))*exp(-4*t)'"
    f":s=48000:d={CHIME_SECONDS}"
)
# The tail is already very quiet by 1.1 s; the short fade just makes sure it ends on silence.
FILTERS = f"afade=t=out:st={CHIME_SECONDS - 0.1:.1f}:d=0.1"


def render_chime(out: Path = CHIME_PATH) -> Path:
    level = f"volume={CHIME_PEAK_DB - peak_db(SOURCE, FILTERS):.2f}dB"
    subprocess.run(
        ["ffmpeg", "-loglevel", "error", "-y", "-f", "lavfi", "-i", SOURCE,
         "-af", f"{FILTERS},{level}",
         "-ar", "48000", "-ac", "1", "-c:a", "libopus", "-b:a", "48k", str(out)],
        check=True,
    )
    return out


if __name__ == "__main__":
    print(render_chime())
