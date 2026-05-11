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

            val builder = AlertDialog.Builder(requireContext())

            builder.setTitle("Alterar Nome")

            val input = EditText(requireContext())

            input.setText(tvName.text.toString())

            builder.setView(input)

            builder.setPositiveButton("Guardar") { _, _ ->

                val novoNome = input.text.toString().trim()

                if (novoNome.isNotEmpty()) {

                    val uid = auth.currentUser?.uid
                        ?: return@setPositiveButton

                    val dbProfile = FirebaseDatabase
                        .getInstance("https://energisaver-project-default-rtdb.europe-west1.firebasedatabase.app")
                        .getReference("users")
                        .child(uid)
                        .child("profile")

                    dbProfile.child("username")
                        .setValue(novoNome)
                        .addOnSuccessListener {

                            tvName.text = novoNome

                            Toast.makeText(
                                context,
                                "Nome atualizado!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener {

                            Toast.makeText(
                                context,
                                "Erro ao atualizar nome",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }

            builder.setNegativeButton("Cancelar", null)

            builder.show()
        }

        btnLogout.setOnClickListener {
            //Remove listeners before logging out
            profileListener?.let {
                dbRef?.child("profile")?.removeEventListener(it)
            }
            devicesListener?.let {
                dbRef?.child("energy_data/devices")
                    ?.removeEventListener(it)
            }

            // Clear references so OnDestroyView doesn't try to remove them again
            profileListener = null
            devicesListener = null

            //Logout
            auth.signOut()

            // Go to login screen but clear the backstack
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()

        profileListener?.let {
            dbRef?.child("profile")?.removeEventListener(it)
        }

        devicesListener?.let {
            dbRef?.child("energy_data/devices")
                ?.removeEventListener(it)
        }
    }
}