from countusdown_pipeline.stems import PULSE_GAP_S, PULSE_PEAK_DB, SECONDS


def test_pulse_ticks_no_more_than_15_times_a_minute():
    # 50 ticks a minute was too stressful (design doc 03.12); one every 4 s is calm.
    assert PULSE_GAP_S >= 4
    assert 60 / PULSE_GAP_S <= 15


def test_pulse_loops_seamlessly():
    # A whole number of ticks in the file, or ExoPlayer's loop seam would stutter.
    assert SECONDS % PULSE_GAP_S == 0


def test_pulse_is_quieter_than_it_was():
    # The old pulse peaked at about -11 dBFS; version E is about 3 dB quieter.
    assert PULSE_PEAK_DB <= -14
