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
import android.widget.ProgressBar
import android.content.res.ColorStateList


class HomeFragment : Fragment() {

    //UI references for components
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

        // 1. Inicialize the components
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
            loadProfile()

        } else {
                Toast.makeText(context, "Sessão expirada", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun loadSummary() {
        database.child("summary").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() && isAdded) {
                    val consumido = snapshot.child("today_kWh").getValue(Float::class.java) ?: 0f
                    val custo = snapshot.child("today_cost").getValue(Float::class.java) ?: 0f

                    // LER A META DA BASE DE DADOS (Se não existir, usa 20 como padrão)
                    val meta = snapshot.child("daily_goal").getValue(Float::class.java) ?: 20f

                    tvTodayUsage.text = String.format("%.2f kWh", consumido)
                    tvTodayCost.text = String.format("%.2f € hoje", custo)

                    // Barra de Progresso
                    val pb = view?.findViewById<ProgressBar>(R.id.progressGoal)
                    val tvPb = view?.findViewById<TextView>(R.id.tvGoalStatus)

                    if (meta > 0) {
                        val progresso = ((consumido / meta) * 100).toInt()
                        pb?.progress = if (progresso > 100) 100 else progresso
                        tvPb?.text = String.format("%.2f / %.0f kWh hoje", consumido, meta)

                        // Lógica de COR: Vermelho se ultrapassar o limite, Verde se estiver OK
                        val cor = if (consumido >= meta) "#EB5757" else "#27AE60"
                        pb?.progressTintList = ColorStateList.valueOf(Color.parseColor(cor))
                    } else {
                        pb?.progress = 0
                        tvPb?.text = "Defina uma meta nas definições"
                        // Garante que a barra começa verde se não houver meta
                        pb?.progressTintList = ColorStateList.valueOf(Color.parseColor("#27AE60"))
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }


    private fun loadLineChart() {
        database.child("day_history")
            .addValueEventListener(object : ValueEventListener {                override fun onDataChange(snapshot: DataSnapshot) {
                val entries = ArrayList<Entry>()
                val labels = ArrayList<String>()
                var index = 0f

                for (point in snapshot.children) {
                    val timeLabel = point.key ?: ""
                    val value = point.getValue(Float::class.java) ?: 0f

                    entries.add(Entry(index, value))
                    labels.add(timeLabel)
                    index++
                }

                if (entries.isNotEmpty()) {
                    val dataSet = LineDataSet(entries, "Consumo Total (W)")
                    dataSet.color = Color.parseColor("#27AE60")
                    dataSet.setCircleColor(Color.parseColor("#27AE60"))
                    dataSet.lineWidth = 2f
                    dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
                    dataSet.setDrawFilled(true)
                    dataSet.fillColor = Color.parseColor("#27AE60")

                    lineChart.data = LineData(dataSet)

                    // Formatar o Eixo X para mostrar as horas do Python
                    val xAxis = lineChart.xAxis
                    xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val i = value.toInt()
                            return if (i >= 0 && i < labels.size) labels[i] else ""
                        }
                    }
                    xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                    xAxis.granularity = 1f

                    lineChart.description.isEnabled = false
                    lineChart.animateX(500)
                    lineChart.invalidate()
                }
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