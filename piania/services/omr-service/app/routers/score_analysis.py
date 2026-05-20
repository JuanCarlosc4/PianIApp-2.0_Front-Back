from fastapi import APIRouter, UploadFile, File, HTTPException
import tempfile
import shutil
import json
import music21

router = APIRouter(prefix="/analysis", tags=["Score Analysis"])

@router.post("/score")
async def analyze_score(file: UploadFile = File(...)):
    filename = file.filename or ""
    if not filename.lower().endswith((".xml", ".musicxml")):
        raise HTTPException(status_code=400, detail="Invalid file type. Must be MusicXML.")

    try:
        # Guardar temporalmente el archivo
        with tempfile.NamedTemporaryFile(delete=False, suffix=".xml") as tmp:
            shutil.copyfileobj(file.file, tmp)
            tmp_path = tmp.name

        # Parsear con music21
        score = music21.converter.parse(tmp_path)

        # Tonalidad
        key = score.analyze("key")
        tonalidad_str = f"{key.tonic.name} {key.mode}"

        # Compás
        time_sigs = score.getTimeSignatures()
        time_sig = time_sigs[0] if len(time_sigs) > 0 else None
        compas_str = time_sig.ratioString if time_sig else None

        # Número de compases
        numero_compases = len(score.parts[0].getElementsByClass("Measure")) if len(score.parts) > 0 else 0

        # Tempo
        tempos = score.flatten().getElementsByClass("MetronomeMark")
        bpm = int(round(tempos[0].getQuarterBPM())) if len(tempos) > 0 else 0

        # Dificultad estimada
        total_notas = len(score.flatten().notes)
        densidad = total_notas / numero_compases if numero_compases > 0 else 0
        dificultad = min(10.0, max(1.0, densidad / 1.5))

        return {
            "tonalidad": tonalidad_str,
            "compas": compas_str,
            "numeroCompases": numero_compases,
            "tempoDetectado": bpm,
            "dificultadEstimada": round(dificultad, 1)
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
