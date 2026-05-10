package com.example.energisaver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.*
import com.github.mikephil.charting.charts.LineChart
import com.google.firebase.database.DatabaseReference
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.database.*
import com.google.android.material.switchmaterial.SwitchMaterial



class DeviceDetailsFragment(private val device: Device) : BottomSheetDialogFragment() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var firebaseListener: ValueEventListener? = null
    private var historyListener: ValueEventListener? = null
    private lateinit var lineChart: LineChart



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.layout_device_details, container, false)

        val etName = view.findViewById<EditText>(R.id.etEditDeviceName)
        val tvCons = view.findViewById<TextView>(R.id.tvDetailConsumption)
        val btnUpdate = view.findViewById<Button>(R.id.btnUpdateDevice)
        val btnDelete = view.findViewById<Button>(R.id.btnDeleteDeviceDetail)
        lineChart = view.findViewById(R.id.detailLineChart)

        etName.setText(device.name)


        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val dbRef = FirebaseDatabase.getInstance("https://energisaver-project-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("users").child(uid!!).child("energy_data").child("devices").child(device.id)

        firebaseListener = dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val watts = snapshot.child("consumption").value?.toString() ?: "0"
                    tvCons.text = "Consumo: $watts W"
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        val swStatus = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchDeviceStatus)

        // 1.Listen to changes in firebase and update the Switch (ex: if someone manually turns it off)
        dbRef.child("status").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentStatus = snapshot.value?.toString() ?: "Offline"
                swStatus.isChecked = (currentStatus == "Ativo")
                swStatus.text = if (swStatus.isChecked) "Ligado (Ativo)" else "Desligado (Off)"
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 2. When user clicks the Switch, update the Firebase
        swStatus.setOnCheckedChangeListener { _, isChecked ->
            val newStatus = if (isChecked) "Ativo" else "Desligado"
            dbRef.child("status").setValue(newStatus)

            // If off manually, set consumption to 0
            if (!isChecked) {
                dbRef.child("consumption").setValue(0)
            }
        }

        loadDeviceHistory(dbRef.child("history"))

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
            Toast.makeText(context, "Dispositivo eliminado!", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        return view
    }


    private fun loadDeviceHistory(historyRef: DatabaseReference) {
        historyListener = historyRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val entries = ArrayList<Entry>()
                var index = 0f

                for (point in snapshot.children) {
                    val value = point.value.toString().toFloatOrNull() ?: 0f
                    entries.add(Entry(index, value))
                    index++
                }

                if (entries.isNotEmpty()) {
                    val dataSet = LineDataSet(entries, "Consumo (W)")
                    dataSet.color = Color.parseColor("#27AE60")
                    dataSet.setCircleColor(Color.parseColor("#27AE60"))
                    dataSet.lineWidth = 2f
                    dataSet.setDrawFilled(true)
                    dataSet.fillColor = Color.parseColor("#27AE60")
                    dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

                    lineChart.data = LineData(dataSet)
                    lineChart.description.isEnabled = false
                    lineChart.invalidate() // Refresh
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        //Cancels any pending coroutines
        scope.cancel()

        firebaseListener?.let { listener ->
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val deviceRef = FirebaseDatabase.getInstance("https://energisaver-project-default-rtdb.europe-west1.firebasedatabase.app")
                    .getReference("users")
                    .child(uid)
                    .child("energy_data")
                    .child("devices")
                    .child(device.id)

                firebaseListener?.let { deviceRef.removeEventListener(it) }

                historyListener?.let { deviceRef.child("history").removeEventListener(it) }
            }
        }
    }
}