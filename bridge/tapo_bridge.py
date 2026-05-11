async def main():
    print(f"🚀 Hub Multi-Utilizador Iniciado!")
    client = ApiClient(EMAIL_TAPO, PASS_TAPO)

    while True:
        try:
            # 1. Search for ALL users in the firebase
            users_ref = db.reference('users')
            users_snapshot = users_ref.get()

            if users_snapshot:
                # 2. Look for each user found in the system
                for uid, user_data in users_snapshot.items():

                    devices = user_data.get('energy_data', {}).get('devices', {})
                    total_usage_watts = 0.0

                    if not devices:
                        continue

                    # Pegamos a hora e minuto atual para as validações
                    now = datetime.now()
                    pode_gravar_historico = (now.minute % 5 == 0) # True de 5 em 5 minutos
                    e_hora_do_reset = (now.hour == 9 and now.minute == 0)

                    # 3. Look for devices of this specific user
                    for dev_id, dev_data in devices.items():
                        ip = dev_data.get('ipAddress')
                        name = dev_data.get('name', 'Dispositivo Desconhecido')
                        # Pega o status da nuvem (Ordem vinda da App)
                        status_nuvem = dev_data.get('status', 'Ativo')

                        if not ip:
                            continue

                        watts = 0.0

                        try:
                            # 3.1 Try Tapo
                            device = await client.p110(ip)

                            # LOGICA DE COMANDO REMOTO:
                            if status_nuvem == "Desligado":
                                await device.off() # Desliga a tomada física
                                watts = 0.0
                            else:
                                await device.on() # Garante que está ligada para ler
                                usage = await device.get_energy_usage()
                                watts = usage.current_power
                                if watts > 500: watts /= 1000 # Convert mW to W

                        except:
                            try:
                                # 3.2 If Tapo not found try Shelly
                                if status_nuvem == "Ativo":
                                    response = requests.get(f"http://{ip}/status", timeout=3)
                                    watts = response.json()['meters'][0]['power']
                                else:
                                    watts = 0.0
                            except:
                                print(f"❌ User [{uid[:5]}] -> {name} em {ip} offline")
                                db.reference(f'users/{uid}/energy_data/devices/{dev_id}').update({'status': 'Offline'})
                                continue

                        # Individual update in Firebase (Sempre atualiza o tempo real)
                        watts = round(watts, 2)
                        db.reference(f'users/{uid}/energy_data/devices/{dev_id}').update({
                            'consumption': watts
                        })

                        # --- LÓGICA DE HISTÓRICO INDIVIDUAL (REQ: 12 leituras/hora + Reset 09h) ---
                        if e_hora_do_reset:
                            # Apaga o histórico para começar o dia novo às 09:00
                            db.reference(f'users/{uid}/energy_data/devices/{dev_id}/history').delete()

                        if pode_gravar_historico:
                            hora_minuto = now.strftime("%H:%M")
                            db.reference(f'users/{uid}/energy_data/devices/{dev_id}/history').update({
                                hora_minuto: watts
                            })

                        total_usage_watts += watts
                        # print(f"✅ User [{uid[:5]}...] -> {name}: {watts} W ({status_nuvem})")

                    # 4. Update general resume (summary)
                    summary_ref = db.reference(f'users/{uid}/energy_data/summary')
                    summary_ref.update({
                        'current_usage': round(total_usage_watts, 2)
                    })

                    # 5. History logic (for line graph global)
                    if e_hora_do_reset:
                        db.reference(f'users/{uid}/energy_data/day_history').delete()

                    if pode_gravar_historico:
                        hora_atual = now.strftime("%H:%M") # Mudamos para H:M para o gráfico ser detalhado
                        db.reference(f'users/{uid}/energy_data/day_history').update({
                            hora_atual: round(total_usage_watts, 2)
                        })

        except Exception as e:
            print(f"💥 Erro no loop global: {e}")

        # Wait 15s before verify all users again
        await asyncio.sleep(15)