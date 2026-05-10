package com.example.energisaver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.database.*
import android.widget.Toast
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import android.graphics.Color
import com.google.firebase.auth.FirebaseAuth


class HomeFragment : Fragment() {

    // Referências para os componentes da UI
    private lateinit var database: DatabaseReference
    private lateinit var tvCurrentUsage: TextView
    private lateinit var tvTodayUsage: TextView
    private lateinit var tvTodayCost: TextView
    private lateinit var lineChart: LineChart
    private lateinit var pieChart: PieChart


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // 1. Inicializar os componentes
        tvCurrentUsage = view.findViewById(R.id.tvCurrentUsageValue)
        tvTodayUsage = view.findViewById(R.id.tvTodayUsageValue)
        tvTodayCost = view.findViewById(R.id.tvTodayCostValue)
        lineChart = view.findViewById(R.id.lineChart)
        pieChart = view.findViewById(R.id.pieChart)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            // Dynamic reference to the user's data
            database = FirebaseDatabase.getInstance("https://energisaver-project-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("users")
                .child(uid)
                .child("energy_data")

            loadSummary()
            loadLineChart()
            loadPieChart()
        } else {
                Toast.makeText(context, "Sessão expirada", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun loadSummary(){
        //Implementing summary
        database.parent?.child("profile")?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("username").value?.toString() ?: "Utilizador"
                view?.findViewById<TextView>(R.id.tvWelcome)?.text = "Bem-vindo, $name"
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        database.child("summary").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(dataSnapshot.exists()) {
                    val currentUsage = dataSnapshot.child("current_usage").value.toString()
                    val todayUsage = dataSnapshot.child("today_kWh").value.toString()
                    val todayCost = dataSnapshot.child("today_cost").value.toString()

                    tvCurrentUsage.text = "$currentUsage kwh"
                    tvTodayUsage.text = "$todayUsage kWh"
                    tvTodayCost.text = "$todayCost € hoje"
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(context, "Database Error: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadLineChart() {
        database.child("day_history")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    val entries = ArrayList<Entry>()

                    for (point in snapshot.children) {
                        val time = point.key?.toFloatOrNull() ?: continue
                        val value = point.getValue(Float::class.java) ?: 0f

                        entries.add(Entry(time, value))
                    }

                    val dataSet = LineDataSet(entries, "Consumo")
                    dataSet.color = Color.parseColor("#27AE60")
                    dataSet.setCircleColor(Color.parseColor("#27AE60"))
                    dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
                    dataSet.setDrawFilled(true) //Fill the area under the line

                    lineChart.data = LineData(dataSet)
                    lineChart.description.isEnabled = false
                    lineChart.invalidate()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }


    private fun loadPieChart() {
        database.child("devices")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    val entries = ArrayList<PieEntry>()

                    for (device in snapshot.children) {
                        val status = device.child("status").value.toString() ?: "Offline"
                        if(status == "Ativo") {
                            val name = device.child("name").value.toString()
                            val consumption = device.child("consumption")
                                .getValue(Float::class.java) ?: 0f

                            entries.add(PieEntry(consumption, name))
                        }
                    }

                    val dataSet = PieDataSet(entries, "")
                    dataSet.colors = listOf(
                        Color.parseColor("#27AE60"),
                        Color.parseColor("#2D9CDB"),
                        Color.parseColor("#F2994A"),
                        Color.parseColor("#9B51E0")
                    )

                    val data = PieData(dataSet)
                    pieChart.data = data
                    pieChart.description.isEnabled = false
                    pieChart.centerText = "Dispositivos"
                    pieChart.invalidate()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadProfile() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            //Point users/UID/profile
            val profileRef = FirebaseDatabase.getInstance("https://energisaver-project-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("users").child(uid).child("profile")

            profileRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("username").value?.toString() ?: "Utilizador"
                    // Atualiza o TextView tvWelcome
                    view?.findViewById<TextView>(R.id.tvWelcome)?.text = "Bem-vindo, $name"
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

}