from fastapi import APIRouter, UploadFile, File, HTTPException
import tempfile
import shutil
import os
import logging

# Silenciar logs TensorFlow
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "3"
logging.getLogger("tensorflow").setLevel(logging.ERROR)

from basic_pitch.inference import predict
from basic_pitch import ICASSP_2022_MODEL_PATH

router = APIRouter(prefix="/analysis", tags=["Audio Transcription"])

@router.post("/audio")
async def transcribe_audio(file: UploadFile = File(...)):
    filename = file.filename or ""
    lower_filename = filename.lower()
    allowed_extensions = (".wav", ".mp3", ".m4a", ".3gp")

    if not lower_filename.endswith(allowed_extensions):
        raise HTTPException(status_code=400, detail="Invalid file type. Must be WAV, MP3, M4A or 3GP.")

    try:
        suffix = os.path.splitext(lower_filename)[1] or ".wav"
        with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
            shutil.copyfileobj(file.file, tmp)
            tmp_path = tmp.name

        model_output, midi_data, note_events = predict(
            tmp_path,
            ICASSP_2022_MODEL_PATH,
            onset_threshold=0.5,
            frame_threshold=0.3,
            minimum_frequency=None,
            maximum_frequency=None
        )

        notas_detectadas = []

        for note in note_events:
            start_time = note[0]
            pitch = note[2]

            notas_detectadas.append({
                "timestamp": round(start_time, 2),
                "midiNote": int(pitch)
            })

        notas_detectadas.sort(key=lambda x: x["timestamp"])

        return notas_detectadas

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
