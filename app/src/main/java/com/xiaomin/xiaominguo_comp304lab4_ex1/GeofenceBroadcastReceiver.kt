package com.xiaomin.xiaominguo_comp304lab4_ex1

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        GeofencingEvent.fromIntent(intent)?.let { geofencingEvent ->
            if (geofencingEvent.hasError()) {
                Log.e("GeofenceReceiver", "Error: ${geofencingEvent.errorCode}")
                return
            }

            when (geofencingEvent.geofenceTransition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    Toast.makeText(context, "Entered geofence!", Toast.LENGTH_LONG).show()
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    Toast.makeText(context, "Exited geofence!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
