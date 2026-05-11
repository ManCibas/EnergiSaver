import asyncio
import firebase_admin
from firebase_admin import credentials, db
from tapo import ApiClient
from datetime import datetime
import requests
import os

# 1. Firebase config
folder = os.path.dirname(os.path.abspath(__file__))
cred_path = os.path.join(folder, "energisaver-project-firebase-adminsdk-fbsvc-cf009acb7a.json")

if not firebase_admin._apps:
    cred = credentials.Certificate(cred_path)
    firebase_admin.initialize_app(cred, {
        'databaseURL': 'https://energisaver-project-default-rtdb.europe-west1.firebasedatabase.app'
    })

# 2. Global CONFIG
EMAIL_TAPO = "ciagoaragao@gmail.com"
PASS_TAPO = "ccii@Tapo5202"

async def main():
    print(f"🚀 Hub IoT Global Iniciado!")
    client = ApiClient(EMAIL_TAPO, PASS_TAPO)

    while True:
        try:
            users_ref = db.reference('users')
            users_snapshot = users_ref.get()

            if users_snapshot:
                for uid, user_data in users_snapshot.items():
                    devices = user_data.get('energy_data', {}).get('devices', {})
                    profile = user_data.get('profile', {})
                    summary_ref = db.reference(f'users/{uid}/energy_data/summary')
                    
                    now = datetime.now()
                    e_hora_do_reset = (now.hour == 9 and now.minute == 0)
                    pode_gravar_ponto = (now.minute % 5 == 0)
                    
                    # Preço da App ou 0.22€ padrão
                    preco_kwh = profile.get('energy_price', 0.22)
                    total_watts = 0.0

                    for dev_id, dev_data in devices.items():
                        ip = dev_data.get('ipAddress')
                        status = dev_data.get('status', 'Ativo')
                        if not ip: continue

                        try:
                            device = await client.p110(ip)
                            if status == "Desligado":
                                await device.off()
                                watts = 0.0
                            else:
                                await device.on()
                                usage = await device.get_energy_usage()
                                watts = usage.current_power / 1000 if usage.current_power > 500 else usage.current_power
                            
                            watts = round(watts, 2)
                            db.reference(f'users/{uid}/energy_data/devices/{dev_id}').update({'consumption': watts})
                            total_watts += watts

                            # Histórico Individual
                            if pode_gravar_ponto:
                                db.reference(f'users/{uid}/energy_data/devices/{dev_id}/history').update({now.strftime("%H:%M"): watts})
                        except:
                            continue

                    # --- CÁLCULO DE ACUMULAÇÃO (Hoje e Custo) ---
                    summary_data = summary_ref.get() or {}
                    kwh_anterior = summary_data.get('today_kWh', 0.0)
                    
                    # 15 segundos entre loops: (Watts * 15s) / (3600s * 1000)
                    ganho = (total_watts * 15) / 3600000
                    novo_kwh = 0.0 if e_hora_do_reset else (kwh_anterior + ganho)
                    
                    summary_ref.update({
                        'current_usage': round(total_watts, 2),
                        'today_kWh': round(novo_kwh, 4),
                        'today_cost': round(novo_kwh * preco_kwh, 2)
                    })

                    if pode_gravar_ponto:
                        db.reference(f'users/{uid}/energy_data/day_history').update({now.strftime("%H:%M"): round(total_watts, 2)})

        except Exception as e:
            print(f"💥 Erro: {e}")

        await asyncio.sleep(15)

if __name__ == "__main__":
    asyncio.run(main())