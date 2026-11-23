# Copiloto - edicion Iron Man (Fabric 1.21.4)
- HUD: compas, mira de objetivo, panel de VITALES (izquierda) y panel de TRAJE con la figura Iron Man y la durabilidad de cada pieza (derecha).
- Panel del rival: nombre, distancia y vida del objetivo que miras o golpeas.
- Voz: cuando la vida del rival baja de la mitad dice "Rival al X por ciento" (50, 30 y 15) y "Objetivo neutralizado" al matarlo.
- Menus y barra de acceso rapido con estilo Iron Man (rojo, dorado, cian).
- V: estado por voz. H: marcar monstruos en rojo. J: HUD on/off.
- Config: config/copiloto.json (ironHud, compass, vitalsPanel, armorPanel, rivalHud, rivalAlerts, menuOverlay).
Las voces ya vienen generadas; gen_audio.py las regenera con espeak-ng + ffmpeg.
