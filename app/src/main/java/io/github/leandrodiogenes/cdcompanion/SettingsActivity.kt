package io.github.leandrodiogenes.cdcompanion

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val prefs = getSharedPreferences("cdcompanion", MODE_PRIVATE)
        val etHost = findViewById<EditText>(R.id.et_host)
        val etPort = findViewById<EditText>(R.id.et_port)
        val btnSave = findViewById<Button>(R.id.btn_save)

        etHost.setText(prefs.getString("ws_host", "10.0.0.9"))
        etPort.setText(prefs.getInt("ws_port", 7891).toString())

        btnSave.setOnClickListener {
            val host = etHost.text.toString().trim()
            val port = etPort.text.toString().toIntOrNull() ?: 7891
            if (host.isEmpty()) {
                Toast.makeText(this, "IP inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.edit()
                .putString("ws_host", host)
                .putInt("ws_port", port)
                .apply()
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        @Suppress("DEPRECATION")
        onBackPressed()
        return true
    }
}
