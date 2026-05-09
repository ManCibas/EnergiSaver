EnergiSaver - Monitorização de Energia IoT

Funcionalidades
• Autenticação Segura: Sistema de Login e Registo via Firebase Auth com isolamento de dados por utilizador (UID).
• Dashboard em Tempo Real: Visualização do consumo atual (Watts), consumo diário (kWh) e custos estimados.
• Gráficos Dinâmicos:
◦ Gráfico de pizza para distribuição de energia por dispositivo.
◦ Gráfico de linha para histórico de consumo (via biblioteca MPAndroidChart).
• Gestão de Dispositivos: Listagem, edição e remoção de aparelhos inteligentes.
• Integração IoT: Leitura real de hardware através de uma ponte Python que comunica com o protocolo encriptado da TP-Link.

Arquitetura do Sistema
1. Hardware (Tapo P110): Mede o consumo elétrico real.
2. Bridge (Python): Script assíncrono que desencripta os dados da tomada e os injeta no Firebase.
3. Cloud (Firebase): Base de dados NoSQL (Realtime Database) que sincroniza os dados instantaneamente.
4. App (Android): Interface nativa que "escuta" as mudanças na nuvem e atualiza a UI sem necessidade de refresh.


Tecnologias Utilizadas
• Android: Kotlin, XML, Fragments, RecyclerView, Coroutines.
• Gráficos: MPAndroidChart.
• Backend: Firebase (Authentication & Realtime Database).
• IoT Bridge: Python 3.12, tapo-python library, firebase-admin.

Como Executar
1. Script Python (Ponte)
    cd bridge
    pip install tapo firebase-admin
    python tapo_bridge.py
2. Aplicação Android
• Abrir o projeto no Android Studio.
• Certificar-se de que o dispositivo (ou emulador) tem ligação à internet.
• Compilar e correr o módulo app.

    Nota: Por motivos de segurança, os ficheiros de configuração de credenciais (google-services.json e chaves privadas do Firebase Admin SDK) foram omitidos através do .gitignore.







