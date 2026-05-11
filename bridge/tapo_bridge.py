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
    print(f"🚀 Hub Multi-Utilizador Inteligente Iniciado!")
    client = ApiClient(EMAIL_TAPO, PASS_TAPO)
    
    while True:
        try:
            # 1. Search for ALL users in the firebase
            users_ref = db.reference('users')
            users_snapshot = users_ref.get()

            if users_snapshot:
                for uid, user_data in users_snapshot.items():
                    devices = user_data.get('energy_data', {}).get('devices', {})
                    profile = user_data.get('profile', {})
                    summary_ref = db.reference(f'users/{uid}/energy_data/summary')
                    
                    now = datetime.now()
                    

                    e_hora_do_reset = (
                        now.hour == 0 and
                        now.minute == 0 and
                        now.second < 15
                    )
                    
                    pode_gravar_ponto = (now.minute % 5 == 0)
                    
                    # Lê preço da App (profile/energy_price) ou usa 0.22 padrão
                    try:
                        preco_kwh = float(profile.get('energy_price', 0.22))
                    except:
                        preco_kwh = 0.22

                    total_usage_watts = 0.0

                    if not devices:
                        continue

                    # 3. Look for devices
                    for dev_id, dev_data in devices.items():
                        ip = dev_data.get('ipAddress')
                        name = dev_data.get('name', 'Desconhecido')
                        status_nuvem = dev_data.get('status', 'Ativo')

                        if not ip: continue

                        watts = 0.0
                        try:
                            # 3.1 Try Tapo
                            device = await client.p110(ip)
                            if status_nuvem == "Desligado":
                                await device.off()
                                watts = 0.0
                            else:
                                await device.on()
                                usage = await device.get_energy_usage()
                                watts = usage.current_power
                                if watts > 500: watts /= 1000 # Convert mW to W
                        except:
                            try:
                                # 3.2 Try Shelly
                                if status_nuvem == "Ativo":
                                    response = requests.get(f"http://{ip}/status", timeout=3)
                                    watts = response.json()['meters'][0]['power']
                            except:
                                continue
                        
                        watts = round(watts, 2)
                        # Atualiza tempo real do dispositivo
                        db.reference(f'users/{uid}/energy_data/devices/{dev_id}').update({'consumption': watts})

                        # Histórico Individual (12 leituras/hora)
                        if e_hora_do_reset:
                            db.reference(f'users/{uid}/energy_data/devices/{dev_id}/history').delete()
                        if pode_gravar_ponto:
                            db.reference(f'users/{uid}/energy_data/devices/{dev_id}/history').update({now.strftime("%H:%M"): watts})

                        total_usage_watts += watts

                    # --- 4. ATUALIZAR RESUMO (CÁLCULOS TÉCNICOS) ---
                    summary_snapshot = summary_ref.get() or {}
                    kwh_anterior = summary_snapshot.get('today_kWh', 0.0)

                    # Cálculo acumulado: (Watts * 15 segundos) / (3600 segundos * 1000)
                    ganho_kwh = (total_usage_watts * 15) / 3600000
                    novo_total_kwh = 0.0 if e_hora_do_reset else (kwh_anterior + ganho_kwh)
                    
                    # Gravar no Firebase (Caminho que o HomeFragment lê)
                    summary_ref.update({
                        'current_usage': round(total_usage_watts, 2),
                        'today_kWh': round(novo_total_kwh, 6),
                        'today_cost': round(novo_total_kwh * preco_kwh, 2)
                    })

                    # 5. History logic Global
                    if e_hora_do_reset:
                        db.reference(f'users/{uid}/energy_data/day_history').delete()
                    if pode_gravar_ponto:
                        db.reference(f'users/{uid}/energy_data/day_history').update({now.strftime("%H:%M"): round(total_usage_watts, 2)})

        except Exception as e:
            print(f"💥 Erro: {e}")

        await asyncio.sleep(15) # Intervalo de atualização

if __name__ == "__main__":
    asyncio.run(main())