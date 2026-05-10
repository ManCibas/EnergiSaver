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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.*


class DeviceDetailsFragment(private val device: Device) : BottomSheetDialogFragment() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var firebaseListener: ValueEventListener? = null


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

        firebaseListener = dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val watts = snapshot.child("consumption").value?.toString() ?: "0"
                    tvCons.text = "Consumo: $watts W"
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })


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


    override fun onDestroy() {
        super.onDestroy()
        //Cancels any pending coroutines
        scope.cancel()

        // 2. Remove o listener do Firebase para evitar que a App
        // continue a gastar dados com a janela fechada
        firebaseListener?.let { listener ->
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                FirebaseDatabase.getInstance("https://energisaver-project-default-rtdb.europe-west1.firebasedatabase.app")
                    .getReference("users")
                    .child(uid)
                    .child("energy_data/devices")
                    .child(device.id)
                    .removeEventListener(listener)
            }
        }
    }
}