"""Render every voice clip in the manifest to Opus, plus the manifest the app reads.

Run on doylestone02 with the heavy TTS group installed:
    uv sync --group tts && uv run python -m countusdown_pipeline.render
Existing clips are skipped, so an interrupted run resumes; --force re-renders all.
"""
from __future__ import annotations

import argparse
import json
import subprocess
import sys
import tempfile
import time
from pathlib import Path

import yaml

from .manifest import Clip, build_manifest

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT.parent / "app" / "src" / "main" / "assets" / "audio"
VOICE_DIR = ASSETS / "voice"

# Every clip ends up at the same perceived loudness (EBU R128), so no voice or tier
# sounds louder than another: firmness comes from the words alone (design doc Q10).
LOUDNORM = "loudnorm=I=-20:TP=-2:LRA=7"


def to_opus(wav: Path, out: Path) -> None:
    """Trim leading/trailing silence, normalise loudness, encode mono Opus."""
    subprocess.run(
        ["ffmpeg", "-loglevel", "error", "-y", "-i", str(wav), "-af",
         "silenceremove=start_periods=1:start_threshold=-50dB,areverse,"
         "silenceremove=start_periods=1:start_threshold=-50dB,areverse,"
         f"{LOUDNORM},apad=pad_dur=0.25",
         "-ar", "48000", "-ac", "1", "-c:a", "libopus", "-b:a", "32k", str(out)],
        check=True,
    )


class Kokoro:
    """hexgrad/Kokoro-82M: built-in voices, British English pipeline ('b')."""

    model = "hexgrad/Kokoro-82M"

    def __init__(self) -> None:
        from kokoro import KPipeline

        self.pipeline = KPipeline(lang_code="b", repo_id=self.model)
        self.sr = 24000

    def synth(self, text: str, voice: str):
        import numpy as np

        # speed < 1 slows delivery slightly: calmer, and easier for children to follow.
        chunks = [audio for _, _, audio in self.pipeline(text, voice=voice, speed=0.92)]
        return np.concatenate([c.numpy() if hasattr(c, "numpy") else c for c in chunks])


class Chatterbox:
    """ResembleAI/chatterbox with its built-in default voice; low exaggeration = calm delivery."""

    model = "ResembleAI/chatterbox"

    def __init__(self) -> None:
        from chatterbox.tts import ChatterboxTTS

        self.tts = ChatterboxTTS.from_pretrained(device="cpu")
        self.sr = self.tts.sr

    def synth(self, text: str, voice: str):
        wav = self.tts.generate(text, exaggeration=0.3, cfg_weight=0.4)
        return wav.squeeze(0).numpy()


ENGINES = {"kokoro": Kokoro, "chatterbox": Chatterbox}


def render(force: bool = False, only_voice: str | None = None) -> None:
    import soundfile as sf

    phrases = yaml.safe_load((ROOT / "phrases.yaml").read_text())
    voices = yaml.safe_load((ROOT / "voices.yaml").read_text())["voices"]
    clips = build_manifest(phrases, voices)
    by_voice = {v["id"]: v for v in voices}
    VOICE_DIR.mkdir(parents=True, exist_ok=True)

    loaded: dict[str, object] = {}
    todo = [c for c in clips if (force or not (VOICE_DIR / f"{c.key}.ogg").exists())
            and (only_voice is None or c.voice == only_voice)]
    print(f"{len(clips)} clips in manifest, {len(todo)} to render", flush=True)
    started = time.time()
    with tempfile.TemporaryDirectory() as tmp:
        for i, clip in enumerate(todo, 1):
            spec = by_voice[clip.voice]
            engine = loaded.get(spec["engine"]) or loaded.setdefault(spec["engine"], ENGINES[spec["engine"]]())
            wav = Path(tmp) / "clip.wav"
            sf.write(wav, engine.synth(clip.text, spec["voice"]), engine.sr)
            to_opus(wav, VOICE_DIR / f"{clip.key}.ogg")
            print(f"[{i}/{len(todo)}] {clip.key}  ({time.time() - started:.0f}s)", flush=True)

    write_app_manifest(clips, voices, phrases)


def write_app_manifest(clips: list[Clip], voices: list[dict], phrases: dict) -> None:
    """The JSON the app reads: which voices and variants exist, and how each clip was made."""
    manifest = {
        "voices": [v["id"] for v in voices],
        "variants": {
            **{tier: len(t) for tier, t in phrases["tiers"].items()},
            "target": len(phrases["target"]),
            "late": len(phrases["late"]),
        },
        "minutes": phrases["minutes"],
        "provenance": {
            v["id"]: {"engine": v["engine"], "model": ENGINES[v["engine"]].model, "voice": v["voice"]}
            for v in voices
        },
        "clips": {c.key: c.text for c in clips},
    }
    (ASSETS / "manifest.json").write_text(json.dumps(manifest, indent=1) + "\n")
    print(f"wrote {ASSETS / 'manifest.json'}")


if __name__ == "__main__":
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--force", action="store_true", help="re-render clips that already exist")
    ap.add_argument("--voice", help="only render this voice id")
    args = ap.parse_args()
    render(force=args.force, only_voice=args.voice)
    sys.exit(0)
