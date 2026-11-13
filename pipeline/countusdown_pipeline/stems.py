"""Synthesise the ambient bed: three 60-second loops built from maths, not samples.

    uv run python -m countusdown_pipeline.stems

Every periodic component completes a whole number of cycles in 60 s, so each file loops
seamlessly in ExoPlayer. No third-party recordings means no licences to track.
"""
from __future__ import annotations

import subprocess
from pathlib import Path

from .render import ASSETS

BED_DIR = ASSETS / "bed"
SECONDS = 60

# Layer 1, from the start: soft brown noise like distant rain, swelling gently (0.1 Hz = 6 swells/min).
# Layer 2, from 20 min left: a quiet open chord (A2, E3, A3, E4) breathing at 0.05 Hz.
# Layer 3, from 10 min left: a soft low pulse at 50 bpm, slower than a resting heart, so it reads as calm.
STEMS = {
    "layer1_rain": (
        f"anoisesrc=color=brown:amplitude=0.6:duration={SECONDS}:sample_rate=48000",
        "highpass=f=80,lowpass=f=900,tremolo=f=0.1:d=0.35",
    ),
    "layer2_pad": (
        f"aevalsrc='(sin(2*PI*110*t)+0.7*sin(2*PI*165*t)+0.5*sin(2*PI*220*t)+0.3*sin(2*PI*330*t))"
        f"*(0.6+0.4*sin(2*PI*0.05*t))*0.12':s=48000:d={SECONDS}",
        "lowpass=f=1200",
    ),
    "layer3_pulse": (
        f"aevalsrc='(sin(2*PI*80*t)+0.5*sin(2*PI*160*t))*exp(-9*mod(t,1.2))*0.35':s=48000:d={SECONDS}",
        "lowpass=f=400",
    ),
}


def synth(name: str, source: str, filters: str) -> Path:
    out = BED_DIR / f"{name}.ogg"
    subprocess.run(
        ["ffmpeg", "-loglevel", "error", "-y", "-f", "lavfi", "-i", source,
         "-af", f"{filters},loudnorm=I=-30:TP=-6",
         "-ar", "48000", "-ac", "1", "-c:a", "libopus", "-b:a", "48k", str(out)],
        check=True,
    )
    return out


def main() -> None:
    BED_DIR.mkdir(parents=True, exist_ok=True)
    for name, (source, filters) in STEMS.items():
        print(synth(name, source, filters))


if __name__ == "__main__":
    main()
