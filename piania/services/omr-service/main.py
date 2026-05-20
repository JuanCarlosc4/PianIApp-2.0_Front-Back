from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(
    title="PianIA OMR Service",
    version="1.0.0",
    description="Microservicio de análisis MusicXML y transcripción de audio"
)

# CORS (ajustable en producción)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/health")
def health():
    return {"status": "OMR service running"}

# Routers
from app.routers import score_analysis, audio_transcription, omr_conversion

app.include_router(score_analysis.router)
app.include_router(audio_transcription.router)
app.include_router(omr_conversion.router)
