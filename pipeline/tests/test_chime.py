import re
import shutil
import subprocess

import pytest

from countusdown_pipeline.chime import CHIME_PEAK_DB, CHIME_SECONDS, render_chime


def test_chime_is_short():
    # One soft "ding" as the warning box appears, not a jingle (design doc 04.02.01).
    assert 0.5 <= CHIME_SECONDS <= 1.5


def test_chime_is_quiet():
    # It plays over whatever is on the TV: a nudge, not an alarm.
    assert CHIME_PEAK_DB <= -12


@pytest.mark.skipif(shutil.which("ffmpeg") is None, reason="needs ffmpeg")
def test_rendered_chime_is_that_short_and_that_quiet(tmp_path):
    out = render_chime(tmp_path / "chime.ogg")
    seconds = float(subprocess.run(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", str(out)],
        check=True, capture_output=True, text=True,
    ).stdout)
    assert seconds == pytest.approx(CHIME_SECONDS, abs=0.1)
    log = subprocess.run(
        ["ffmpeg", "-hide_banner", "-i", str(out), "-af", "volumedetect", "-f", "null", "-"],
        check=True, capture_output=True, text=True,
    ).stderr
    peak = float(re.search(r"max_volume: (-?[\d.]+) dB", log).group(1))
    assert peak <= CHIME_PEAK_DB + 1  # opus encoding moves the peak a little
