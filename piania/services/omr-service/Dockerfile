FROM python:3.10

WORKDIR /app

# System deps:
# - openjdk: to run Audiveris (Java)
# - curl + unzip: to download / inspect artifacts if needed
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        openjdk-21-jre-headless \
        curl \
        unzip \
        tesseract-ocr \
        tesseract-ocr-eng \
        tesseract-ocr-spa \
        ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# Make OCR languages visible for Audiveris (it looks at ~/.config/AudiverisLtd/audiveris/tessdata by default)
# We copy the system tessdata there so Audiveris can list languages (eng/spa/osd, etc.).
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/5/tessdata
RUN mkdir -p /root/.config/AudiverisLtd/audiveris/tessdata \
    && if [ -d /usr/share/tesseract-ocr/5/tessdata ]; then cp -n /usr/share/tesseract-ocr/5/tessdata/*.traineddata /root/.config/AudiverisLtd/audiveris/tessdata/ || true; fi \
    && if [ -d /usr/share/tessdata ]; then cp -n /usr/share/tessdata/*.traineddata /root/.config/AudiverisLtd/audiveris/tessdata/ || true; fi

# Audiveris (PDF/IMG -> MusicXML)
# Audiveris no publica .jar runnable en releases recientes; publica instaladores.
# En Linux instalamos el .deb (Ubuntu) y usamos el binario `audiveris` dentro del contenedor.
#
# Puedes override en build: --build-arg AUDIVERIS_VERSION=5.10.2 --build-arg AUDIVERIS_DEB_FLAVOR=ubuntu22.04
ARG AUDIVERIS_VERSION=5.10.2
ARG AUDIVERIS_DEB_FLAVOR=ubuntu22.04
ENV AUDIVERIS_DEB_URL=https://github.com/Audiveris/audiveris/releases/download/${AUDIVERIS_VERSION}/Audiveris-${AUDIVERIS_VERSION}-${AUDIVERIS_DEB_FLAVOR}-x86_64.deb
# En contenedores headless el postinst de Audiveris intenta registrar entradas de escritorio
# vía xdg-desktop-menu y falla. Lo neutralizamos sustituyendo /usr/bin/xdg-desktop-menu por un stub.
RUN curl -fL -o /tmp/audiveris.deb "${AUDIVERIS_DEB_URL}" \
    && apt-get update \
    && apt-get install -y --no-install-recommends xdg-utils \
    && if [ -x /usr/bin/xdg-desktop-menu ]; then mv /usr/bin/xdg-desktop-menu /usr/bin/xdg-desktop-menu.real; fi \
    && printf '#!/bin/sh\nexit 0\n' > /usr/bin/xdg-desktop-menu \
    && chmod +x /usr/bin/xdg-desktop-menu \
    && dpkg -i /tmp/audiveris.deb || true \
    && apt-get -f install -y --no-install-recommends \
    && ln -sf /opt/audiveris/bin/Audiveris /usr/local/bin/audiveris \
    && rm -f /tmp/audiveris.deb \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install -r requirements.txt

COPY . .

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
