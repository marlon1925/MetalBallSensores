package com.projects.enzoftware.metalball

import android.app.Service
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Point
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.view.Display
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


class MetalBall : AppCompatActivity(), SensorEventListener {

    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null
    var ground: GroundView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        // get reference of the service
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        // focus in accelerometer
        mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        // setup the window
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_IMMERSIVE
        }

        // set the view
        ground = GroundView(this)
        setContentView(ground)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            val x = event.values[1]
            val y = event.values[0]
            ground?.updateMe(x, y)
        }
    }

    override fun onResume() {
        super.onResume()
        mSensorManager!!.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager!!.unregisterListener(this)
    }
}

class GroundView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    var myRef: DatabaseReference? = null

    var cx: Float = 10f
    var cy: Float = 10f

    var lastGx: Float = 0f
    var lastGy: Float = 0f

    var picHeight: Int = 0
    var picWidth: Int = 0

    var icon: Bitmap? = null

    var Windowwidth: Int = 0
    var Windowheight: Int = 0

    var noBorderX = false
    var noBorderY = false

    var vibratorService: Vibrator? = null
    var thread: DrawThread? = null

    init {
        holder.addCallback(this)
        thread = DrawThread(holder, this)
        val display: Display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val size: Point = Point()
        display.getSize(size)
        Windowwidth = size.x
        Windowheight = size.y
        icon = BitmapFactory.decodeResource(resources, R.drawable.ball)
        picHeight = icon!!.height
        picWidth = icon!!.width
        vibratorService = (context.getSystemService(Service.VIBRATOR_SERVICE)) as Vibrator
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Implementación de surfaceChanged
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Implementación de surfaceDestroyed
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        thread!!.setRunning(true)
        thread!!.start()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        canvas.drawColor(Color.BLACK)
        icon?.let { canvas.drawBitmap(it, cx, cy, null) }
        myRef?.child("ball_coordinates")?.setValue(mapOf("x" to cx, "y" to cy))
    }

    fun updateMe(inx: Float, iny: Float) {
        lastGx += inx
        lastGy += iny

        cx += lastGx
        cy += lastGy

        if (cx > (Windowwidth - picWidth)) {
            cx = (Windowwidth - picWidth).toFloat()
            lastGx = 0f
            if (noBorderX) {
                vibratorService!!.vibrate(100)
                noBorderX = false
            }
        } else if (cx < 0) {
            cx = 0f
            lastGx = 0f
            if (noBorderX) {
                vibratorService!!.vibrate(100)
                noBorderX = false
            }
        } else {
            noBorderX = true
        }

        if (cy > (Windowheight - picHeight)) {
            cy = (Windowheight - picHeight).toFloat()
            lastGy = 0f
            if (noBorderY) {
                vibratorService!!.vibrate(100)
                noBorderY = false
            }
        } else if (cy < 0) {
            cy = 0f
            lastGy = 0f
            if (noBorderY) {
                vibratorService!!.vibrate(100)
                noBorderY = false
            }
        } else {
            noBorderY = true
        }

        // Actualiza las coordenadas en Firebase
        val database = FirebaseDatabase.getInstance()
        myRef = database.reference.child("metalball")
        myRef!!.child("ball_coordinates").setValue(mapOf("x" to cx, "y" to cy))

        invalidate()
    }

    inner class DrawThread(private val surfaceHolder: SurfaceHolder, private val groundView: GroundView) : Thread() {
        private var running: Boolean = false

        fun setRunning(running: Boolean) {
            this.running = running
        }

        override fun run() {
            var canvas: Canvas?
            while (running) {
                canvas = null
                try {
                    canvas = surfaceHolder.lockCanvas(null)
                    synchronized(surfaceHolder) {
                        groundView.draw(canvas!!)
                    }
                } finally {
                    if (canvas != null) {
                        surfaceHolder.unlockCanvasAndPost(canvas)
                    }
                }
            }
        }
    }
}
