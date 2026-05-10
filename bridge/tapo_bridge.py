import asyncio
import firebase_admin
from firebase_admin import credentials, db
from tapo import ApiClient
from datetime import datetime
import requests # Movido para o topo para melhor performance

# 1. Firebase config
cred_path = r"C:\Users\ciago\Downloads\energisaver-project-firebase-adminsdk-fbsvc-cf009acb7a.json"
cred = credentials.Certificate(cred_path)

if not firebase_admin._apps:
    firebase_admin.initialize_app(cred, {
        'databaseURL': 'https://energisaver-project-default-rtdb.europe-west1.firebasedatabase.app'
    })

# 2. Global CONFIG (Conta Tapo)
EMAIL_TAPO = "ciagoaragao@gmail.com"
PASS_TAPO = "ccii@Tapo5202"

# 3. User config (Removido o UID fixo para ser Multi-Utilizador)

async def main():
    print(f"🚀 Super Hub Multi-Utilizador Iniciado!")
    client = ApiClient(EMAIL_TAPO, PASS_TAPO)
    
    while True:
        try:
            # 1. Search for ALL users in the firebase
            users_ref = db.reference('users')
            users_snapshot = users_ref.get()

            if users_snapshot:
                # 2. Look for each user found in the system
                for uid, user_data in users_snapshot.items():
                    
                    # 2.1 Get devices and summary path for this specific user
                    devices = user_data.get('energy_data', {}).get('devices', {})
                    total_usage_watts = 0.0
                    
                    if not devices:
                        continue

                    # 3. Look for devices of this specific user
                    for dev_id, dev_data in devices.items():
                        ip = dev_data.get('ipAddress')
                        name = dev_data.get('name', 'Dispositivo Desconhecido')
                        if not ip:
                            continue

                        watts = 0.0

                        try:
                            # 3.1 Try Tapo
                            device = await client.p110(ip)
                            usage = await device.get_energy_usage()
                            watts = usage.current_power
                            if watts > 500: watts /= 1000 # Convert mW to W
                            
                        except:
                            try:
                                # 3.2 If Tapo not found or connected try Shelly (with simple HTTP)
                                response = requests.get(f"http://{ip}/status", timeout=3)
                                watts = response.json()['meters'][0]['power']
                            except:
                                watts = 0
                                print(f"❌ User [{uid[:5]}] -> {name} em {ip} offline")
                                # Atualiza status para offline no Firebase
                                db.reference(f'users/{uid}/energy_data/devices/{dev_id}').update({'status': 'Offline'})
                                continue
                        
                        # Individual update in Firebase for this user's device
                        watts = round(watts, 2)
                        db.reference(f'users/{uid}/energy_data/devices/{dev_id}').update({
                            'consumption': watts, 
                            'status': 'Ativo'
                        })
                        total_usage_watts += watts
                        print(f"✅ User [{uid[:5]}...] -> {name}: {watts} W")

                    # 4. Update general resume (summary) for this specific user
                    summary_ref = db.reference(f'users/{uid}/energy_data/summary')
                    summary_ref.update({
                        'current_usage': round(total_usage_watts, 2)
                    })

                    # 5. History logic (for line graph) for this specific user
                    # Stores total consumption in current time (ex: "14h" -> 25.5W)
                    hora_atual = datetime.now().strftime("%H")
                    db.reference(f'users/{uid}/energy_data/day_history').update({
                        hora_atual: round(total_usage_watts, 2)
                    })

        except Exception as e:
            print(f"💥 Erro no loop global: {e}")

        # Wait 15s before verify all users again (aumentado para evitar sobrecarga)
        print("--- Ciclo Completo (Todos os utilizadores verificados) ---")
        await asyncio.sleep(15)

if __name__ == "__main__":
    asyncio.run(main())