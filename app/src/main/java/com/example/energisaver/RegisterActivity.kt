package com.example.energisaver

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val auth = FirebaseAuth.getInstance()
        val btnReg = findViewById<Button>(R.id.btnDoRegister)
        val btnBack = findViewById<TextView>(R.id.tvBackToLogin)

        btnReg.setOnClickListener {
            val name = findViewById<EditText>(R.id.etRegName).text.toString()
            val email = findViewById<EditText>(R.id.etRegEmail).text.toString()
            val pass = findViewById<EditText>(R.id.etRegPassword).text.toString()

            if (name.isNotEmpty() && email.isNotEmpty() && pass.isNotEmpty()) {
                // 1. Criar utilizador no Firebase Auth
                auth.createUserWithEmailAndPassword(email, pass)
                    .addOnSuccessListener {
                        val uid = auth.currentUser?.uid
                        if (uid != null) {
                            // 2. Referência para a base de dados (Europa)
                            val db = FirebaseDatabase.getInstance("https://energisaver-project-default-rtdb.europe-west1.firebasedatabase.app")
                                .getReference("users").child(uid)

                            val initialData = mapOf(
                                "profile/username" to name,
                                "energy_data/summary/current_usage" to 0,
                                "energy_data/summary/today_kwh" to 0,
                                "energy_data/summary/today_cost" to 0
                            )

                            // 3. Criar a estrutura inicial de dados
                            db.updateChildren(initialData).addOnSuccessListener {
                                Toast.makeText(this, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show()

                                // 4. Navegar para o Dashboard
                                val intent = Intent(this, DashboardActivity::class.java)
                                startActivity(intent)
                                finish()
                            }.addOnFailureListener {
                                Toast.makeText(this, "Erro ao criar dados: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .addOnFailureListener {
                        // Caso o email já exista ou a senha seja fraca
                        Toast.makeText(this, "Erro no registo: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            }
        }

        btnBack.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}