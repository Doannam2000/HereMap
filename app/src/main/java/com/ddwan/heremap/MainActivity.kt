package com.ddwan.heremap

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.ddwan.heremap.config.Constants.Companion.LOCATION_REQUEST_CODE
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.here.android.mpa.common.GeoCoordinate
import com.here.android.mpa.common.Image
import com.here.android.mpa.common.OnEngineInitListener


import com.here.android.mpa.mapping.AndroidXMapFragment
import com.here.android.mpa.mapping.Map


class MainActivity : AppCompatActivity() {

    private var myLocation: Location? = null
    private var fusedLocation: FusedLocationProviderClient? = null

    private var mapFragment: AndroidXMapFragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragment) as AndroidXMapFragment
        fusedLocation = LocationServices.getFusedLocationProviderClient(this)
        getCurrentLocation()
    }

    private fun checkPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE)
        }
    }


    private fun getCurrentLocation() {
        checkPermission()
        fusedLocation!!.lastLocation.addOnSuccessListener {
            if (it != null) {
                myLocation = it
                mapFragment!!.init { error ->
                    if (error == OnEngineInitListener.Error.NONE) {
                        val map: Map = mapFragment!!.map!!
                        map.setCenter(GeoCoordinate(it.latitude, it.longitude), Map.Animation.NONE)
                        map.zoomLevel = map.maxZoomLevel
                        val image = Image()
                        image.setImageResource(R.drawable.marker_blue)
                        mapFragment!!.positionIndicator?.setMarker(image)
                    }
                }
            }
        }
    }
}
