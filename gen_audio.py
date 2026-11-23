#!/usr/bin/env python3
"""Genera las voces del asistente (estilo J.A.R.V.I.S.) con espeak-ng + ffmpeg."""
import json, os, subprocess

OUT = "src/main/resources/assets/copiloto"
os.makedirs(OUT + "/sounds", exist_ok=True)

clips = {"pots_0": "Sin pociones", "pots_1": "Queda una poción"}
for n in range(2, 37):
    clips[f"pots_{n}"] = f"Quedan {n} pociones"
pieces = {"helmet": "Casco", "chest": "Peto", "legs": "Pantalones", "boots": "Botas"}
for k, v in pieces.items():
    for t in (50, 25, 10):
        clips[f"armor_{k}_{t}"] = (f"Alerta. {v} al {t} por ciento" if t == 10
                                   else f"{v} al {t} por ciento")
clips["hp_low"] = "Advertencia. Integridad estructural baja"
clips["hello"] = "Iniciando sistemas. Bienvenido, señor. Todos los sistemas en línea"
for r in range(5, 50, 5):
    clips[f"rival_{r}"] = f"Rival al {r} por ciento"
clips["rival_dead"] = "Objetivo neutralizado"

# clips que llevan pitido de alerta delante; "hello" lleva sonido de arranque
ALERT = {"hp_low"} | {f"armor_{k}_10" for k in pieces} | {f"rival_{r}" for r in (5, 10, 15, 20)}

FX = ("aformat=sample_rates=22050:channel_layouts=mono,highpass=f=140,lowpass=f=8000,"
      "equalizer=f=2800:t=q:w=1.2:g=5,aecho=0.8:0.6:35|70:0.3|0.2,volume=1.6")
PRE = "aformat=sample_rates=22050:channel_layouts=mono"


def ff(*args):
    subprocess.check_call(["ffmpeg", "-y", "-loglevel", "error", *args])


# efectos de sonido
ff("-f", "lavfi", "-i", "sine=frequency=1400:duration=0.07",
   "-f", "lavfi", "-i", "sine=frequency=1900:duration=0.07",
   "-filter_complex", f"[0][1]concat=n=2:v=0:a=1,volume=0.5,{PRE}", "/tmp/beep.wav")
ff("-f", "lavfi", "-i", "aevalsrc=0.4*sin(2*PI*(500*t+900*t*t)):d=0.45:s=22050",
   "-af", f"afade=t=out:st=0.35:d=0.1,{PRE}", "/tmp/boot.wav")

for key, text in clips.items():
    wav = f"/tmp/{key}.wav"
    subprocess.check_call(["espeak-ng", "-v", "es+m3", "-s", "155", "-p", "35", "-w", wav, text])
    out = f"{OUT}/sounds/{key}.ogg"
    pre = "/tmp/beep.wav" if key in ALERT else ("/tmp/boot.wav" if key == "hello" else None)
    if pre:
        ff("-i", pre, "-i", wav, "-filter_complex",
           f"[0:a]{PRE}[b];[1:a]{FX}[v];[b][v]concat=n=2:v=0:a=1[o]",
           "-map", "[o]", "-ac", "1", "-c:a", "libvorbis", out)
    else:
        ff("-i", wav, "-af", FX, "-ac", "1", "-c:a", "libvorbis", out)

sounds = {k: {"sounds": [f"copiloto:{k}"]} for k in clips}
with open(OUT + "/sounds.json", "w") as f:
    json.dump(sounds, f, indent=1)
print("clips:", len(clips))
