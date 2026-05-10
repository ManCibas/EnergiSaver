package com.example.energisaver

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*


class DevicesFragment : Fragment() {

    private lateinit var adapter: DeviceAdapter
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_devices, container, false)

        val rv = view.findViewById<RecyclerView>(R.id.rvDevices)
        rv.layoutManager = LinearLayoutManager(context)
        adapter = DeviceAdapter(
            deviceList = emptyList(),
            onItemClick = { device ->
                //Open a new BottomSheet
                val detailsSheet = DeviceDetailsFragment(device)
                detailsSheet.show(parentFragmentManager, "DeviceDetails")
            },
            onItemLongClick = { device ->
                // Chamar a função para eliminar
                showDeleteConfirmDialog(device)
            }
        )
        rv.adapter = adapter

        val btnAdd = view.findViewById<Button>(R.id.btnAddNewDevice)
        //Get the current user
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            val uid = currentUser.uid

            database = FirebaseDatabase.getInstance("https://energisaver-project-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("users")
                .child(uid)
                .child("energy_data")
                .child("devices")


            database.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<Device>()
                    for (child in snapshot.children) {
                        val device = child.getValue(Device::class.java)
                        if (device != null) {
                            device.id = child.key ?: "" //Store nod key in case of deletion
                            list.add(device)
                        }
                    }
                    adapter.updateList(list)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Erro: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })

            //Click listener for the button
            btnAdd.setOnClickListener {
                showAddDeviceDialog()
            }
        } else {
            Toast.makeText(context, "Utilizador não autenticado", Toast.LENGTH_SHORT).show()
        }



        return view
    }

    private fun showAddDeviceDialog() {
        val builder =  AlertDialog.Builder(requireContext())
        builder.setTitle("Novo Dispositivo")

        // Create a layout for the input fields
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val inputName = EditText(requireContext())
        inputName.hint = "Nome (ex: Frigorífico)"
        layout.addView(inputName)

        val inputIP = EditText(requireContext())
        inputIP.hint = "Endereço IP (ex: 192.168.1.50)"
        layout.addView(inputIP)

        builder.setView(layout)

        builder.setPositiveButton("Adicionar") { _, _ ->
            val name = inputName.text.toString()
            val ip = inputIP.text.toString()

            if (name.isNotEmpty()) {
                val deviceId = database.push().key // Generate a unique ID for the new device

                val newDevice = Device(
                    id = deviceId ?: "",
                    name = name,
                    consumption = 0f,
                    status = "Ativo",
                    ipAddress = ip
                )

                if (deviceId != null) {
                    database.child(deviceId).setValue(newDevice)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Adicionado com sucesso!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(context, "Erro ao gravar: ${exception.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()

    }

    private fun showDeleteConfirmDialog(device: Device) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Dispositivo")
            .setMessage("Tem a certeza que deseja eliminar o ${device.name}?")
            .setPositiveButton("Sim") { _, _ ->
                // Eliminate from Firebase using device's ID
                database.child(device.id).removeValue().addOnSuccessListener {
                    Toast.makeText(context, "Removido com sucesso", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Não", null)
            .show()
    }
}