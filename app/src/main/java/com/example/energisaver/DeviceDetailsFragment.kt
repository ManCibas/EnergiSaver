package com.example.energisaver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException


class DeviceDetailsFragment(private val device: Device) : BottomSheetDialogFragment() {

    private val client = OkHttpClient()
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.layout_device_details, container, false)

        val etName = view.findViewById<EditText>(R.id.etEditDeviceName)
        val tvCons = view.findViewById<TextView>(R.id.tvDetailConsumption)
        val btnUpdate = view.findViewById<Button>(R.id.btnUpdateDevice)
        val btnDelete = view.findViewById<Button>(R.id.btnDeleteDeviceDetail)

        etName.setText(device.name)
        tvCons.text = "Consumo Atual: ${device.consumption} kWh"

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val dbRef = FirebaseDatabase.getInstance("https://energisaver-project-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("users").child(uid!!).child("energy_data").child("devices").child(device.id)

        if (device.ipAddress.isNotEmpty()) {
            fetchShellyData(device.ipAddress, tvCons, dbRef)
        } else {
            tvCons.text = "IP não configurado. Consumo: ${device.consumption} kWh"
        }

        btnUpdate.setOnClickListener {
            val newName = etName.text.toString()
            if (newName.isNotEmpty()) {
                dbRef.child("name").setValue(newName)
                Toast.makeText(context, "Atualizado!", Toast.LENGTH_SHORT).show()
                dismiss() // Fecha a janela
            }
        }

        btnDelete.setOnClickListener {
            dbRef.removeValue()
            Toast.makeText(context, "Eliminado!", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        return view
    }
    private fun fetchShellyData(ip: String, textView: TextView, dbRef: com.google.firebase.database.DatabaseReference) {
        val request = Request.Builder()
            .url("http://$ip/status") //Shelly uses HTTP
            .build()

        scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw IOException("Erro: $response")

                        val jsonData = JSONObject(response.body!!.string())
                        val meters = jsonData.getJSONArray("meters")
                        val powerWatts = meters.getJSONObject(0).getDouble("power").toFloat()

                        withContext(Dispatchers.Main) {
                            textView.text = "Consumo Real: $powerWatts W"
                            // Update Firebase with the read values from the plug
                            dbRef.child("consumption").setValue(powerWatts)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        textView.text = "Erro ao ligar à Shelly: ${e.message}"
                    }
                }
                delay(5000) // Wait for 5 seconds before fetching data again
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel() // Limpa os processos de rede ao fechar a janela
    }
}