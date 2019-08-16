package com.eacevedod78.consultadesensores

import android.graphics.Color
import android.graphics.DashPathEffect
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

import kotlinx.android.synthetic.main.activity_datos_oxigeno.*
import kotlinx.android.synthetic.main.content_datos_oxigeno.*
import org.jetbrains.anko.textColor
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.scheduleAtFixedRate

class DatosOxigenoActivity : AppCompatActivity() {


    lateinit var lineChart: LineChart
    var mediciones = ArrayList<Entry>()
    val timer = Timer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_datos_oxigeno)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        lineChart = chartOxigeno
        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(true)

        //temporizador para ejecutar la tarea cada 6 segundos
        timer.scheduleAtFixedRate(0,6000){
            consultaMediciones()
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        timer.cancel()
        NavUtils.navigateUpFromSameTask(this)
        overridePendingTransition(R.anim.slide_from_left,R.anim.slide_to_right)
    }

    fun consultaMediciones(){
        val db = FirebaseFirestore.getInstance()
        //1. Obtener datos de la nube
        //2. Guardar datos
        //3. actualizar datos en la grafica

        db.collection("oxigeno")
            .orderBy("fecha", Query.Direction.DESCENDING).limit(1)
            .get()
            .addOnSuccessListener {result ->
                for(document in result){
                    val sfd = SimpleDateFormat("dd/MMMM/yyyy HH:mm:ss", Locale("es","MX"))
                    var valor = document.getDouble("valor") as Double
                    val medicion = "${valor.toString()} mg/l"
                    fechaOxigenoTextView.text = sfd.format(document.getTimestamp("fecha")?.toDate())
                    medicionOxigenoTextView.text = medicion

                    if(valor.compareTo(25)>0 || valor.compareTo(21)<0){
                        medicionOxigenoTextView.textColor = Color.RED
                    }else{
                        medicionOxigenoTextView.textColor = Color.DKGRAY
                    }
                }
            }
            .addOnFailureListener{exception ->
                Log.w("oxigeno","Error getting documents.", exception)
                Toast.makeText(this,"Error al leer las mediciones",Toast.LENGTH_SHORT).show()
            }


        db.collection("oxigeno")
            .orderBy("fecha",Query.Direction.DESCENDING).limit(10)
            .get()
            .addOnSuccessListener {result ->
                var i = 0
                mediciones = ArrayList<Entry>()
                for(document in result){
                    val sfd = SimpleDateFormat("dd/MMMM/yyyy HH:mm:ss", Locale("es-MX"))
                    val fecha = sfd.format(document.getTimestamp("fecha")?.toDate())
                    val valor = document.getDouble("valor")
                    mediciones.add(Entry(i.toFloat(),valor!!.toFloat()))
                    i=i+1
                    Log.d("oxigeno","${document.id} => ${fecha} , valor ${valor}")
                }
                renderData()
            }
            .addOnFailureListener{exception ->
                Log.w("oxigeno","Error getting documents.", exception)
                Toast.makeText(this,"Error al leer las mediciones",Toast.LENGTH_SHORT).show()
            }

    }

    fun renderData() {
        var llXAxis = LimitLine(10f,"Index 10")
        llXAxis.lineWidth = 4f
        llXAxis.labelPosition = LimitLine.LimitLabelPosition.RIGHT_BOTTOM
        llXAxis.enableDashedLine(10f,10f,0f)
        llXAxis.textSize = 10f

        var xAxis = lineChart.xAxis
        xAxis.enableAxisLineDashedLine(10f,10f,0f)
        xAxis.axisMaximum = 9f
        xAxis.axisMinimum = 0f
        xAxis.setDrawGridLinesBehindData(true)

        var ll1 = LimitLine(40f,"Límite máximo aceptable")
        ll1.lineWidth = 4f
        ll1.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
        ll1.enableDashedLine(10f,10f,0f)
        ll1.textSize = 10f

        var ll2 = LimitLine(30f,"Límite mínimo aceptable")
        ll2.lineWidth = 4f
        ll2.labelPosition = LimitLine.LimitLabelPosition.RIGHT_BOTTOM
        ll2.enableDashedLine(10f,10f,0f)
        ll2.textSize = 10f

        var leftAxis = lineChart.axisLeft
        leftAxis.removeAllLimitLines()
        leftAxis.addLimitLine(ll1)
        leftAxis.addLimitLine(ll2)
        leftAxis.axisMaximum = 60f
        leftAxis.axisMinimum = 0f
        leftAxis.enableGridDashedLine(10f,10f,0f)
        leftAxis.setDrawZeroLine(false)
        leftAxis.setDrawGridLinesBehindData(false)

        lineChart.axisRight.isEnabled = false

        setData()

    }

    fun setData() {

       // if(lineChart.data != null && lineChart.data.dataSetCount >0){
       //     var set1 = lineChart.data.getDataSetByIndex(0) as LineDataSet
       //     set1.values = mediciones
       //     lineChart.data.notifyDataChanged()
       //     lineChart.notifyDataSetChanged()
       // }else{
            var set1 = LineDataSet(mediciones,"Oxígeno")
            set1.setDrawIcons(false)
            set1.enableDashedLine(10f, 5f, 0f)
            set1.enableDashedHighlightLine(10f, 5f, 0f)
            set1.color = Color.DKGRAY
            set1.setCircleColor(Color.DKGRAY)
            set1.lineWidth = 1f
            set1.circleRadius = 3f
            set1.setDrawCircleHole(false)
            set1.valueTextSize = 9f
            set1.setDrawFilled(true)
            set1.formLineWidth = 1f
            set1.formLineDashEffect = DashPathEffect(floatArrayOf(10f,5f),0f)
            set1.formSize= 15f


            set1.fillDrawable = ContextCompat.getDrawable(this,R.drawable.fade_blue)

            var lineData = LineData(set1)
            lineChart.data = lineData
            lineChart.invalidate()

      //  }
    }


}
