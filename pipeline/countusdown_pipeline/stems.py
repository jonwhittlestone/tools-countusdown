"""Synthesise the ambient bed: three 60-second loops built from maths, not samples.

    uv run python -m countusdown_pipeline.stems

Every periodic component completes a whole number of cycles in 60 s, so each file loops
seamlessly in ExoPlayer. No third-party recordings means no licences to track.
"""
from __future__ import annotations

import re
import subprocess
from pathlib import Path

from .render import ASSETS

BED_DIR = ASSETS / "bed"
SECONDS = 60

# Layer 1, from the start: soft brown noise like distant rain, swelling gently (0.1 Hz = 6 swells/min).
# Layer 2, from 20 min left: a quiet open chord (A2, E3, A3, E4) breathing at 0.05 Hz.
# Layer 3, from 10 min left: a soft low 80 Hz knock every 4 s (15 a minute). It was 50 a minute,
# which was too stressful (design doc 03.12). An 8 ms attack keeps just enough knock for TV speakers.
PULSE_GAP_S = 4
PULSE_PEAK_DB = -14.0

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
        f"aevalsrc='(sin(2*PI*80*t)+0.5*sin(2*PI*160*t))*(1-exp(-400*mod(t,{PULSE_GAP_S})))"
        f"*exp(-9*mod(t,{PULSE_GAP_S}))':s=48000:d={SECONDS}",
        # The s16 step clips the 1.5x-amplitude knock flat at full scale, which gives version E its
        # slightly firmer edge; volume then sets it to PULSE_PEAK_DB.
        "lowpass=f=400,aformat=sample_fmts=s16",
    ),
}


# Sparse ticks are mostly silence, so loudness normalisation would push each tick up; the pulse
# is set by its peak instead.
PEAK_LEVELS = {"layer3_pulse": PULSE_PEAK_DB}


def peak_db(source: str, filters: str) -> float:
    """The loudest sample of the raw synthesis, in dBFS (ffmpeg's volumedetect)."""
    log = subprocess.run(
        ["ffmpeg", "-hide_banner", "-f", "lavfi", "-i", source, "-af", f"{filters},volumedetect",
         "-f", "null", "-"],
        check=True, capture_output=True, text=True,
    ).stderr
    return float(re.search(r"max_volume: (-?[\d.]+) dB", log).group(1))


def synth(name: str, source: str, filters: str) -> Path:
    out = BED_DIR / f"{name}.ogg"
    if name in PEAK_LEVELS:
        level = f"volume={PEAK_LEVELS[name] - peak_db(source, filters):.2f}dB"
    else:
        level = "loudnorm=I=-30:TP=-6"
    subprocess.run(
        ["ffmpeg", "-loglevel", "error", "-y", "-f", "lavfi", "-i", source,
         "-af", f"{filters},{level}",
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
