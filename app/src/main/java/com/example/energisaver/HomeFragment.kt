package com.example.energisaver

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeFragment : Fragment() {

    // UI references for components
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

        // 1. Initialize the components
        tvCurrentUsage = view.findViewById(R.id.tvCurrentUsageValue)
        tvTodayUsage = view.findViewById(R.id.tvTodayUsageValue)
        tvTodayCost = view.findViewById(R.id.tvTodayCostValue)

        lineChart = view.findViewById(R.id.lineChart)
        pieChart = view.findViewById(R.id.pieChart)

        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {

            val uid = currentUser.uid

            // Dynamic reference to the user's data
            database = FirebaseDatabase
                .getInstance("https://energisaver-project-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("users")
                .child(uid)
                .child("energy_data")

            // Load all dashboard information
            loadSummary(view)
            loadLineChart()
            loadPieChart()
            loadProfile()

        } else {

            Toast.makeText(
                context,
                "Sessão expirada",
                Toast.LENGTH_SHORT
            ).show()
        }

        return view
    }

    private fun loadSummary(view: View) {

        // References to dashboard cards
        val tvWatts = view.findViewById<TextView>(R.id.tvCurrentUsageValue)
        val tvKwh = view.findViewById<TextView>(R.id.tvTodayUsageValue)
        val tvCusto = view.findViewById<TextView>(R.id.tvTodayCostValue)

        // IMPORTANT:
        // database already points to:
        // users/{uid}/energy_data
        //
        // So we only need:
        // summary
        database.child("summary")
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    if (!isAdded || !snapshot.exists()) return

                    // Read values from Firebase
                    val watts = snapshot.child("current_usage")
                        .getValue(Double::class.java) ?: 0.0

                    val kwh = snapshot.child("today_kWh")
                        .getValue(Double::class.java) ?: 0.0

                    val custo = snapshot.child("today_cost")
                        .getValue(Double::class.java) ?: 0.0

                    // Update cards
                    tvWatts.text =
                        "${String.format("%.1f", watts)} W"

                    tvKwh.text =
                        "${String.format("%.4f", kwh)} kWh"

                    tvCusto.text =
                        "${String.format("%.2f", custo)} €"
                }

                override fun onCancelled(error: DatabaseError) {

                    Toast.makeText(
                        context,
                        "Erro ao carregar resumo",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun loadLineChart() {

        database.child("day_history")
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    val entries = ArrayList<Entry>()
                    val labels = ArrayList<String>()

                    var index = 0f

                    for (point in snapshot.children) {

                        val timeLabel = point.key ?: ""

                        val value =
                            point.getValue(Double::class.java)?.toFloat() ?: 0f

                        entries.add(Entry(index, value))

                        labels.add(timeLabel)

                        index++
                    }

                    if (entries.isNotEmpty()) {

                        val dataSet =
                            LineDataSet(entries, "Consumo Total (W)")

                        dataSet.color =
                            Color.parseColor("#27AE60")

                        dataSet.setCircleColor(
                            Color.parseColor("#27AE60")
                        )

                        dataSet.lineWidth = 2f

                        dataSet.mode =
                            LineDataSet.Mode.CUBIC_BEZIER

                        dataSet.setDrawFilled(true)

                        dataSet.fillColor =
                            Color.parseColor("#27AE60")

                        lineChart.data = LineData(dataSet)

                        // Format X axis with hour labels
                        val xAxis = lineChart.xAxis

                        xAxis.valueFormatter =
                            object : com.github.mikephil.charting.formatter.ValueFormatter() {

                                override fun getFormattedValue(value: Float): String {

                                    val i = value.toInt()

                                    return if (i >= 0 && i < labels.size)
                                        labels[i]
                                    else
                                        ""
                                }
                            }

                        xAxis.position = XAxis.XAxisPosition.BOTTOM
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

                        val status =
                            device.child("status")
                                .value
                                .toString()

                        if (status == "Ativo") {

                            val name =
                                device.child("name")
                                    .value
                                    .toString()

                            val consumption =
                                device.child("consumption")
                                    .getValue(Double::class.java)
                                    ?.toFloat() ?: 0f

                            entries.add(
                                PieEntry(consumption, name)
                            )
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

        val uid = FirebaseAuth
            .getInstance()
            .currentUser
            ?.uid

        if (uid != null) {

            // Point users/UID/profile
            val profileRef = FirebaseDatabase
                .getInstance("https://energisaver-project-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("users")
                .child(uid)
                .child("profile")

            profileRef.addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    val name =
                        snapshot.child("username")
                            .value
                            ?.toString()
                            ?: "Utilizador"

                    // Update welcome text
                    view?.findViewById<TextView>(R.id.tvWelcome)
                        ?.text = "Bem-vindo, $name"
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }
}