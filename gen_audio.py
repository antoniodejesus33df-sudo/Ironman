#!/usr/bin/env python3
"""Genera las voces (robotica, estilo asistente) con espeak-ng + ffmpeg."""
import json, os, subprocess

OUT = "src/main/resources/assets/copiloto"
os.makedirs(OUT + "/sounds", exist_ok=True)

clips = {"pots_0": "Sin pociones", "pots_1": "Queda una poción"}
for n in range(2, 37):
    clips[f"pots_{n}"] = f"Quedan {n} pociones"
pieces = {"helmet": "Casco", "chest": "Peto", "legs": "Pantalones", "boots": "Botas"}
for k, v in pieces.items():
    for t in (50, 25, 10):
        clips[f"armor_{k}_{t}"] = f"{v} al {t} por ciento"
clips["hp_low"] = "Vida baja"
clips["hello"] = "Sistemas en línea"

for key, text in clips.items():
    wav = f"/tmp/{key}.wav"
    subprocess.check_call(["espeak-ng", "-v", "es", "-s", "150", "-p", "30", "-w", wav, text])
    subprocess.check_call([
        "ffmpeg", "-y", "-loglevel", "error", "-i", wav,
        "-af", "highpass=f=120,aecho=0.8:0.8:25:0.35",
        "-ac", "1", "-ar", "22050", "-c:a", "libvorbis",
        f"{OUT}/sounds/{key}.ogg",
    ])

sounds = {k: {"sounds": [f"copiloto:{k}"]} for k in clips}
with open(OUT + "/sounds.json", "w") as f:
    json.dump(sounds, f, indent=1)
print("clips:", len(clips))
