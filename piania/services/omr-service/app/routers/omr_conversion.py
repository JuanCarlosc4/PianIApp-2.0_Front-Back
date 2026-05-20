from fastapi import APIRouter, UploadFile, File, HTTPException
from fastapi.responses import FileResponse
import os
import shutil
import subprocess
import tempfile
import zipfile
from pathlib import Path
from PIL import Image
import logging
import sys

import music21

logger = logging.getLogger(__name__)
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler(sys.stdout)]
)

router = APIRouter(prefix="/analysis", tags=["OMR"])

OMR_OUTPUT_DIR = os.environ.get("OMR_OUTPUT_DIR", "omr_outputs")

# Audiveris locations (inside our Docker image the .deb installs into /opt/audiveris)
DEFAULT_AUDIVERIS_WRAPPER = "/opt/audiveris/bin/Audiveris"
DEFAULT_AUDIVERIS_JAR = "/opt/audiveris/lib/app/audiveris.jar"


def _get_audiveris_cmd_base() -> list[str]:
    """
    Resolve how to execute Audiveris depending on the environment.

    Priority:
      1) AUDIVERIS_CMD (explicit)
      2) /opt/audiveris/bin/Audiveris (Docker linux .deb install)
      3) AUDIVERIS_JAR (java -jar)
      4) AUDIVERIS_EXE (Windows legacy)
    """
    audiveris_cmd_env = os.environ.get("AUDIVERIS_CMD")
    audiveris_wrapper = os.environ.get("AUDIVERIS_WRAPPER", DEFAULT_AUDIVERIS_WRAPPER)
    audiveris_jar = os.environ.get("AUDIVERIS_JAR", DEFAULT_AUDIVERIS_JAR)
    audiveris_exe = os.environ.get("AUDIVERIS_EXE", r"C:\Program Files\Audiveris\Audiveris.exe")

    if audiveris_cmd_env and audiveris_cmd_env.strip():
        audiveris_cmd_base = audiveris_cmd_env.strip().split()
        logger.info(f"[OMR] Using AUDIVERIS_CMD: {audiveris_cmd_base}")
        return audiveris_cmd_base

    if audiveris_wrapper and os.path.exists(audiveris_wrapper):
        audiveris_cmd_base = [audiveris_wrapper]
        logger.info(f"[OMR] Using AUDIVERIS_WRAPPER: {audiveris_wrapper}")
        return audiveris_cmd_base

    if audiveris_jar and os.path.exists(audiveris_jar):
        java_bin = os.environ.get("JAVA_BIN", "java")
        audiveris_cmd_base = [java_bin, "-Djava.awt.headless=true", "-jar", audiveris_jar]
        logger.info(f"[OMR] Using AUDIVERIS_JAR: {audiveris_jar}")
        return audiveris_cmd_base

    logger.info(f"[OMR] Using AUDIVERIS_EXE: {audiveris_exe}")
    if not os.path.exists(audiveris_exe):
        logger.error(f"[OMR] Audiveris executable not found at {audiveris_exe}")
        raise HTTPException(status_code=500, detail="Audiveris executable not found")

    return [audiveris_exe]


def _analyze_musicxml_path(musicxml_path: str) -> dict:
    """
    Parse and analyze a MusicXML file using music21.

    Note: OMR engines may generate incomplete / invalid MusicXML for low-quality or
    non-musical inputs. We must not crash with 500 in those cases; instead return a
    controlled 422 so the caller can handle it gracefully.
    """
    try:
        score = music21.converter.parse(musicxml_path)
    except ZeroDivisionError as e:
        # Known issue when MusicXML has <divisions>0</divisions>
        raise HTTPException(
            status_code=422,
            detail=f"Invalid MusicXML produced by OMR (divisions=0). Cannot analyze. ({e})",
        )
    except Exception as e:
        raise HTTPException(
            status_code=422,
            detail=f"Invalid MusicXML produced by OMR. Cannot analyze. ({e})",
        )

    # Tonalidad
    key = score.analyze("key")
    tonalidad_str = f"{key.tonic.name} {key.mode}"

    # Compás
    time_sigs = score.getTimeSignatures()
    time_sig = time_sigs[0] if len(time_sigs) > 0 else None
    compas_str = time_sig.ratioString if time_sig else None

    # Número de compases (primer pentagrama)
    measures = score.parts[0].getElementsByClass("Measure") if len(score.parts) > 0 else []
    numero_compases = len(measures)

    # Tempo
    tempos = score.flatten().getElementsByClass("MetronomeMark")
    bpm = tempos[0].getQuarterBPM() if len(tempos) > 0 else 0.0

    # Dificultad estimada
    total_notas = len(score.flatten().notes)
    densidad = total_notas / numero_compases if numero_compases > 0 else 0
    dificultad = min(10.0, max(1.0, densidad / 1.5))

    return {
        "tonalidad": tonalidad_str,
        "compas": compas_str,
        "numeroCompases": numero_compases,
        "tempoDetectado": bpm,
        "dificultadEstimada": round(dificultad, 1),
    }


def _extract_musicxml_from_mxl(mxl_path: str) -> str:
    with zipfile.ZipFile(mxl_path, "r") as zf:
        for name in zf.namelist():
            if name.lower().endswith(".xml") and "meta-inf" not in name.lower():
                with zf.open(name) as f:
                    return f.read().decode("utf-8", errors="replace")
    raise RuntimeError("No XML found inside generated .mxl")


def _find_first_by_suffix(root: str, suffixes: tuple[str, ...]) -> str | None:
    for dirpath, _, filenames in os.walk(root):
        for fn in filenames:
            lower = fn.lower()
            if any(lower.endswith(s) for s in suffixes):
                return str(Path(dirpath) / fn)
    return None


def _upscale_image(input_path: str, multiplier: int = 3) -> str:
    """Upscale small images to improve OMR accuracy."""
    img = Image.open(input_path)
    if img.height < 2000:
        new_size = (img.width * multiplier, img.height * multiplier)
        img = img.resize(new_size, Image.Resampling.LANCZOS)
        img.save(input_path)
        return input_path
    return input_path


def _clean_with_musescore(input_xml: str, tmpdir: str) -> str:
    """Clean MusicXML using MuseScore."""
    musescore_path = os.environ.get("MUSESCORE_PATH", "musescore")
    output_xml = str(Path(tmpdir) / "cleaned.musicxml")

    try:
        cmd = [musescore_path, input_xml, "-o", output_xml]
        proc = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, timeout=60)

        if proc.returncode != 0:
            print(f"MuseScore warning: {proc.stdout}")
            return input_xml

        return output_xml
    except (FileNotFoundError, subprocess.TimeoutExpired) as e:
        print(f"MuseScore not available: {e}. Skipping cleaning.")
        return input_xml


def _save_musicxml_to_disk(musicxml_content: str, sheet_music_id: str | None = None) -> str:
    """Save MusicXML to disk and return file path."""
    os.makedirs(OMR_OUTPUT_DIR, exist_ok=True)

    if sheet_music_id:
        sheet_dir = Path(OMR_OUTPUT_DIR) / str(sheet_music_id)
    else:
        import uuid
        sheet_dir = Path(OMR_OUTPUT_DIR) / str(uuid.uuid4())

    sheet_dir.mkdir(parents=True, exist_ok=True)
    file_path = sheet_dir / "score.musicxml"
    file_path.write_text(musicxml_content, encoding="utf-8")

    return str(file_path)


@router.get("/download/{sheet_music_id}")
async def download_musicxml(sheet_music_id: str):
    """
    Download MusicXML file for a sheet music.
    """
    try:
        sheet_dir = Path(OMR_OUTPUT_DIR) / sheet_music_id
        file_path = sheet_dir / "score.musicxml"
        
        if not file_path.exists():
            raise HTTPException(status_code=404, detail="MusicXML file not found")
        
        return FileResponse(
            path=file_path,
            media_type="application/xml",
            filename="score.musicxml"
        )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/omr")
async def omr_convert_and_analyze(file: UploadFile = File(...)):
    """
    Accepts PDF or image and returns MusicXML + analysis (old backend behavior).
    Conversion is performed by Audiveris (local executable).
    Saves MusicXML to disk to avoid database bloat.
    """
    filename = file.filename or ""
    if not filename.lower().endswith((".pdf", ".png", ".jpg", ".jpeg")):
        raise HTTPException(
            status_code=400,
            detail="Invalid file type. Must be PDF/PNG/JPG/JPEG.",
        )

    # Resolve Audiveris executable (Docker linux .deb install uses /opt/audiveris/bin/Audiveris)
    audiveris_cmd_base = _get_audiveris_cmd_base()

    try:
        with tempfile.TemporaryDirectory(prefix="piania_omr_") as tmpdir:
            input_path = str(Path(tmpdir) / filename)
            with open(input_path, "wb") as f:
                shutil.copyfileobj(file.file, f)

            logger.info(f"[OMR] File saved to: {input_path}")

            # 1. Upscale small images for better OMR accuracy
            if filename.lower().endswith((".png", ".jpg", ".jpeg")):
                _upscale_image(input_path, multiplier=3)
                logger.info(f"[OMR] Image upscaled")

            output_dir = str(Path(tmpdir) / "audiveris_out")
            os.makedirs(output_dir, exist_ok=True)

            # 2. Run Audiveris executable
            cmd = [
                *audiveris_cmd_base,
                "-batch",
                "-export",
                "-output",
                output_dir,
                input_path,
            ]

            logger.info(f"[OMR] Running Audiveris: {' '.join(cmd)}")

            proc = subprocess.run(
                cmd,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                timeout=300,
            )

            logger.info(f"[OMR] Audiveris return code: {proc.returncode}")
            logger.info(f"[OMR] Audiveris output:\n{proc.stdout}")

            if proc.returncode != 0:
                # Usually means "transcription incomplete" or input not recognized as a valid score.
                # Treat as 422 so clients can show a user-friendly error.
                logger.error(f"[OMR] Audiveris FAILED: {proc.stdout}")
                raise HTTPException(
                    status_code=422,
                    detail=f"Audiveris failed (code {proc.returncode}): {proc.stdout[-1500:]}",
                )

            # 3. Prefer MXL, fallback to XML if any
            mxl_path = _find_first_by_suffix(output_dir, (".mxl",))
            xml_path = _find_first_by_suffix(output_dir, (".xml", ".musicxml"))

            logger.info(f"[OMR] Found MXL: {mxl_path}, XML: {xml_path}")

            musicxml_content: str | None = None
            musicxml_for_analysis_path: str | None = None

            if mxl_path:
                musicxml_content = _extract_musicxml_from_mxl(mxl_path)
                # Write extracted xml to tmp for analysis
                extracted_path = str(Path(tmpdir) / "extracted.musicxml")
                Path(extracted_path).write_text(musicxml_content, encoding="utf-8")
                musicxml_for_analysis_path = extracted_path
            elif xml_path:
                musicxml_for_analysis_path = xml_path
                musicxml_content = Path(xml_path).read_text(encoding="utf-8", errors="replace")
            else:
                raise HTTPException(
                    status_code=500,
                    detail="Audiveris finished but no MusicXML/MXL was found in output",
                )

            # 4. Clean MusicXML with MuseScore
            musicxml_for_analysis_path = _clean_with_musescore(musicxml_for_analysis_path, tmpdir)
            if musicxml_for_analysis_path.endswith("cleaned.musicxml"):
                musicxml_content = Path(musicxml_for_analysis_path).read_text(encoding="utf-8", errors="replace")

            # 5. Analyze the MusicXML
            analysis = _analyze_musicxml_path(musicxml_for_analysis_path)

            # 6. Save MusicXML to disk (sheet_music_id will be provided by caller)
            disk_path = _save_musicxml_to_disk(musicxml_content)

            logger.info(f"[OMR] SUCCESS: Analysis completed, saved to {disk_path}")

            return {
                "musicxml": musicxml_content,
                "musicxmlPath": f"/analysis/download",  # Will be used with {sheet_music_id} by caller
                **analysis,
            }

    except subprocess.TimeoutExpired:
        logger.error("[OMR] Audiveris timeout")
        raise HTTPException(status_code=504, detail="Audiveris timed out")
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"[OMR] Exception: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/omr/{sheet_music_id}")
async def omr_convert_with_id(sheet_music_id: str, file: UploadFile = File(...)):
    """
    Same as /omr but accepts sheet_music_id to organize outputs by sheet music ID.
    """
    filename = file.filename or ""
    logger.info(f"[OMR] Received file: {filename}, sheet_music_id: {sheet_music_id}")

    if not filename.lower().endswith((".pdf", ".png", ".jpg", ".jpeg")):
        raise HTTPException(
            status_code=400,
            detail="Invalid file type. Must be PDF/PNG/JPG/JPEG.",
        )

    # Resolve Audiveris executable (Docker linux .deb install uses /opt/audiveris/bin/Audiveris)
    audiveris_cmd_base = _get_audiveris_cmd_base()

    try:
        with tempfile.TemporaryDirectory(prefix="piania_omr_") as tmpdir:
            input_path = str(Path(tmpdir) / filename)
            with open(input_path, "wb") as f:
                shutil.copyfileobj(file.file, f)

            logger.info(f"[OMR] File saved to: {input_path}")

            # 1. Upscale small images for better OMR accuracy
            if filename.lower().endswith((".png", ".jpg", ".jpeg")):
                _upscale_image(input_path, multiplier=3)
                logger.info(f"[OMR] Image upscaled")

            output_dir = str(Path(tmpdir) / "audiveris_out")
            os.makedirs(output_dir, exist_ok=True)

            # 2. Run Audiveris executable
            cmd = [
                *audiveris_cmd_base,
                "-batch",
                "-export",
                "-output",
                output_dir,
                input_path,
            ]

            logger.info(f"[OMR] Running Audiveris: {' '.join(cmd)}")

            proc = subprocess.run(
                cmd,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                timeout=300,
            )

            logger.info(f"[OMR] Audiveris return code: {proc.returncode}")
            logger.info(f"[OMR] Audiveris output:\n{proc.stdout}")

            if proc.returncode != 0:
                # Usually means "transcription incomplete" or input not recognized as a valid score.
                # Treat as 422 so clients can show a user-friendly error.
                logger.error(f"[OMR] Audiveris FAILED: {proc.stdout}")
                raise HTTPException(
                    status_code=422,
                    detail=f"Audiveris failed (code {proc.returncode}): {proc.stdout[-1500:]}",
                )

            # 3. Prefer MXL, fallback to XML if any
            mxl_path = _find_first_by_suffix(output_dir, (".mxl",))
            xml_path = _find_first_by_suffix(output_dir, (".xml", ".musicxml"))

            logger.info(f"[OMR] Found MXL: {mxl_path}, XML: {xml_path}")

            musicxml_content: str | None = None
            musicxml_for_analysis_path: str | None = None

            if mxl_path:
                musicxml_content = _extract_musicxml_from_mxl(mxl_path)
                # Write extracted xml to tmp for analysis
                extracted_path = str(Path(tmpdir) / "extracted.musicxml")
                Path(extracted_path).write_text(musicxml_content, encoding="utf-8")
                musicxml_for_analysis_path = extracted_path
            elif xml_path:
                musicxml_for_analysis_path = xml_path
                musicxml_content = Path(xml_path).read_text(encoding="utf-8", errors="replace")
            else:
                raise HTTPException(
                    status_code=500,
                    detail="Audiveris finished but no MusicXML/MXL was found in output",
                )

            # 4. Clean MusicXML with MuseScore
            musicxml_for_analysis_path = _clean_with_musescore(musicxml_for_analysis_path, tmpdir)
            if musicxml_for_analysis_path.endswith("cleaned.musicxml"):
                musicxml_content = Path(musicxml_for_analysis_path).read_text(encoding="utf-8", errors="replace")

            # 5. Analyze the MusicXML
            analysis = _analyze_musicxml_path(musicxml_for_analysis_path)

            # 6. Save MusicXML to disk with sheet_music_id
            disk_path = _save_musicxml_to_disk(musicxml_content, sheet_music_id)

            logger.info(f"[OMR] SUCCESS: Analysis completed for sheet_music_id={sheet_music_id}, saved to {disk_path}")

            return {
                "musicxml": musicxml_content,
                "musicxmlPath": f"/analysis/download/{sheet_music_id}",  # Downloadable URL
                **analysis,
            }

    except subprocess.TimeoutExpired:
        logger.error("[OMR] Audiveris timeout")
        raise HTTPException(status_code=504, detail="Audiveris timed out")
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"[OMR] Exception: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))
