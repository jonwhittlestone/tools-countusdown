"""Listen to every rendered clip with Whisper and flag any that don't say their script.

    make audio-verify    (runs in a throwaway env with faster-whisper; nothing added to the project)

A TTS model occasionally mumbles, skips or invents a word; this catches it without a human
listening to all the clips. Score = share of script words heard (order-insensitive).
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ASSETS = Path(__file__).resolve().parents[2] / "app" / "src" / "main" / "assets" / "audio"
NUMBERS = {"1": "one", "2": "two", "3": "three", "4": "four", "5": "five", "10": "ten", "15": "fifteen",
           "20": "twenty", "25": "twenty five", "30": "thirty", "45": "forty five", "60": "sixty"}


def words(text: str) -> list[str]:
    text = re.sub(r"\d+", lambda m: f" {NUMBERS.get(m.group(), m.group())} ", text.lower())
    return re.findall(r"[a-z']+", text.replace("-", " "))


def score(script: str, heard: str) -> float:
    want, got = words(script), words(heard)
    pool = list(got)
    hit = 0
    for w in want:
        if w in pool:
            pool.remove(w)
            hit += 1
    return hit / max(len(want), 1)


def decode(path: Path):
    """16 kHz mono float samples via ffmpeg (sidesteps faster-whisper's PyAV version coupling)."""
    import subprocess

    import numpy as np

    raw = subprocess.run(["ffmpeg", "-loglevel", "error", "-i", str(path), "-f", "f32le", "-ac", "1",
                          "-ar", "16000", "-"], check=True, capture_output=True).stdout
    return np.frombuffer(raw, dtype=np.float32)


def main(threshold: float = 0.8) -> int:
    from faster_whisper import WhisperModel

    model = WhisperModel("small.en", device="cpu", compute_type="int8")
    clips = json.loads((ASSETS / "manifest.json").read_text())["clips"]
    bad = []
    for i, (key, script) in enumerate(sorted(clips.items()), 1):
        segments, _ = model.transcribe(decode(ASSETS / "voice" / f"{key}.ogg"), language="en", beam_size=1)
        heard = " ".join(s.text for s in segments).strip()
        s = score(script, heard)
        flag = "  <-- CHECK" if s < threshold else ""
        print(f"[{i}/{len(clips)}] {s:.2f} {key}: {heard}{flag}", flush=True)
        if flag:
            bad.append(key)
    print(f"\n{len(clips) - len(bad)}/{len(clips)} clips OK" + (f"; check: {', '.join(bad)}" if bad else ""))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
