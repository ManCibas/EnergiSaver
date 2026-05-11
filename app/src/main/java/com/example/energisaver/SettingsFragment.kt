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

        // 1. Clicar no Card da Meta para mudar o limite
        val cardGoal = view.findViewById<CardView>(R.id.card_goal)
        cardGoal.setOnClickListener {
            showInputDialog("Definir Meta Diária (kWh)", "energy_data/summary/daily_goal")
        }

        // 2. Clicar na Tarifa para mudar o preço
        val btnTariff = view.findViewById<LinearLayout>(R.id.btnSetTariff)
        btnTariff.setOnClickListener {
            showInputDialog("Preço por kWh (€)", "profile/energy_price")
        }


        loadDailyGoal(view)


        return view
    }

    private fun showInputDialog(title: String, dbPath: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(title)
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        builder.setView(input)
        builder.setPositiveButton("Guardar") { _, _ ->
            val valor = input.text.toString().toFloatOrNull()

            if (valor == null) {

                Toast.makeText(
                    context,
                    "Valor inválido",
                    Toast.LENGTH_SHORT
                ).show()

                return@setPositiveButton
            }
            // Isto envia o número real para o Firebase
            dbRef?.child(dbPath)?.setValue(valor)
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun loadDailyGoal(view: View) {

        val progressBar =
            view.findViewById<ProgressBar>(R.id.progressGoal)

        val tvGoalStatus =
            view.findViewById<TextView>(R.id.tvGoalStatus)

        val uid = FirebaseAuth.getInstance()
            .currentUser
            ?.uid ?: return

        // Reference to summary
        val summaryRef = FirebaseDatabase
            .getInstance("https://energisaver-project-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("users")
            .child(uid)
            .child("energy_data")
            .child("summary")

        summaryRef.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                if (!snapshot.exists()) return

                // Current consumption
                val todayKwh =
                    snapshot.child("today_kWh")
                        .getValue(Double::class.java) ?: 0.0

                // Daily limit
                val dailyGoal =
                    snapshot.child("daily_goal")
                        .getValue(Double::class.java) ?: 0.0

                // If there is no limit configured
                if (dailyGoal <= 0) {

                    progressBar.progress = 0
                    progressBar.invalidate()

                    tvGoalStatus.text =
                        "Clique para definir meta"

                    return
                }

                // Calculate progress
                val progress =
                    ((todayKwh / dailyGoal) * 100).toInt()

                progressBar.progress =
                    if (progress > 100) 100
                    else progress

                // Update text
                tvGoalStatus.text =
                    "${String.format("%.2f", todayKwh)} / ${String.format("%.2f", dailyGoal)} kWh"

                // Change color if limit exceeded
                val color =
                    if (progress >= 100)
                        "#EB5757"
                    else
                        "#27AE60"

                progressBar.progressDrawable.setTint(
                    android.graphics.Color.parseColor(color)
                )
            }

            override fun onCancelled(error: DatabaseError) {}
        })
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