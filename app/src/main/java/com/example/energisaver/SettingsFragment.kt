package com.example.energisaver

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*


class SettingsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val tvName = view.findViewById<TextView>(R.id.tvSettingsUsername)
        val tvEmail = view.findViewById<TextView>(R.id.tvSettingsEmail)
        val tvDeviceCount = view.findViewById<TextView>(R.id.tvDeviceCount)
        val btnLogout = view.findViewById<LinearLayout>(R.id.btnSettingsLogout)

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user != null) {
            tvEmail.text = user.email
            val db = FirebaseDatabase.getInstance("https://energisaver-project-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("users").child(user.uid)

            // Load username
            db.child("profile").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    tvName.text = snapshot.child("username").value?.toString() ?: "Utilizador"
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            // Count devices
            db.child("energy_data/devices").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    tvDeviceCount.text = "${snapshot.childrenCount} dispositivos ligados"
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }

        tvName.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Alterar Nome")

            val input = EditText(requireContext())
            input.setText(tvName.text.toString())
            builder.setView(input)

            builder.setPositiveButton("Guardar") { _, _ ->
                val novoNome = input.text.toString()
                if (novoNome.isNotEmpty()) {
                    val uid = auth.currentUser?.uid
                    val dbProfile = FirebaseDatabase.getInstance("https://energisaver-project-default-rtdb.europe-west1.firebasedatabase.app")
                        .getReference("users").child(uid!!).child("profile")

                    dbProfile.child("username").setValue(novoNome).addOnSuccessListener {
                        Toast.makeText(context, "Nome atualizado!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            builder.setNegativeButton("Cancelar", null)
            builder.show()
        }


        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(activity, LoginActivity::class.java))
            activity?.finish()
        }


        return view
    }
}