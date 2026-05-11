package com.example.energisaver

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*


class SettingsFragment : Fragment() {

    private var profileListener: ValueEventListener? = null
    private var devicesListener: ValueEventListener? = null
    private var dbRef: DatabaseReference? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val tvName = view.findViewById<TextView>(R.id.tvSettingsUsername)
        val tvEmail = view.findViewById<TextView>(R.id.tvSettingsEmail)
        val tvDeviceCount = view.findViewById<TextView>(R.id.tvDeviceCount)
        val btnLogout = view.findViewById<LinearLayout>(R.id.btnSettingsLogout)
        val ivEditUsername = view.findViewById<ImageView>(R.id.ivEditUsername)
        val cardGoal = view.findViewById<CardView>(R.id.card_goal)
        val btnTariff = view.findViewById<LinearLayout>(R.id.btnSetTariff)


        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user != null) {

            tvEmail.text = user.email

            dbRef = FirebaseDatabase
                .getInstance("https://energisaver-project-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("users")
                .child(user.uid)

            // Load username
            profileListener = object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    tvName.text =
                        snapshot.child("username").value?.toString()
                            ?: "Utilizador"
                }

                override fun onCancelled(error: DatabaseError) {}
            }
            dbRef!!
                .child("profile")
                .addListenerForSingleValueEvent(profileListener!!)

            // Count devices
            devicesListener = object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    tvDeviceCount.text =
                        "${snapshot.childrenCount} dispositivos ligados"
                }

                override fun onCancelled(error: DatabaseError) {}
            }
            dbRef!!
                .child("energy_data/devices")
                .addListenerForSingleValueEvent(devicesListener!!)
        }

        ivEditUsername.setOnClickListener {
            showInputDialog("Alterar Nome", "profile/username", InputType.TYPE_CLASS_TEXT)
        }

        // Definir Meta
        cardGoal.setOnClickListener {
            showInputDialog("Definir Meta Diária (kWh)", "energy_data/summary/daily_goal",
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        }

        // Definir Tarifa
        btnTariff.setOnClickListener {
            showInputDialog("Preço por kWh (€)", "profile/energy_price",
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        }

        btnLogout.setOnClickListener {
            removeListeners()
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }


        return view
    }

    private fun showInputDialog(title: String, dbPath: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(title)
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        builder.setView(input)
        builder.setPositiveButton("Ok") { _, _ ->
            val valor = input.text.toString().toFloatOrNull() ?: 0f
            dbRef?.child(dbPath)?.setValue(valor)
        }
        builder.show()
    }

    private fun showInputDialog(title: String, dbPath: String, inputType: Int) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(title)
        val input = EditText(requireContext())
        input.inputType = inputType
        builder.setView(input)
        builder.setPositiveButton("Guardar") { _, _ ->
            val valorStr = input.text.toString()
            if (valorStr.isNotEmpty()) {
                val valor: Any = if (inputType == InputType.TYPE_CLASS_TEXT) valorStr else (valorStr.toFloatOrNull() ?: 0f)
                dbRef?.child(dbPath)?.setValue(valor)
            }
        }
        builder.setNegativeButton("Cancelar", null).show()
    }

    private fun removeListeners() {
        profileListener?.let { dbRef?.child("profile")?.removeEventListener(it) }
        devicesListener?.let { dbRef?.child("energy_data/devices")?.removeEventListener(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeListeners()
    }
}
