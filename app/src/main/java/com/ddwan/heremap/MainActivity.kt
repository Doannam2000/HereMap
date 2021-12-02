package com.ddwan.heremap

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PointF
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.ddwan.heremap.adapter.RecyclerViewAdapter
import com.ddwan.heremap.config.Constants.Companion.LOCATION_REQUEST_CODE
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.here.android.mpa.common.*
import com.here.android.mpa.mapping.*
import com.here.android.mpa.search.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.collections.ArrayList
import com.here.android.mpa.mapping.MapMarker
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.ddwan.heremap.viewmodel.MapViewModel
import com.here.android.mpa.search.ReverseGeocodeRequest


class MainActivity : AppCompatActivity() {

    private val mapViewModel by lazy {
        ViewModelProvider(this).get(MapViewModel::class.java)
    }
    lateinit var adapter: RecyclerViewAdapter
    private var fusedLocation: FusedLocationProviderClient? = null
    private var mapFragment: AndroidXMapFragment? = null
    private var listSearch = ArrayList<DiscoveryResult>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragment) as AndroidXMapFragment

        // lắng nghe text khoảng cách - thời gian
        observe()

        // lấy vị trí ban đầu
        fusedLocation = LocationServices.getFusedLocationProviderClient(this)
        getCurrentLocation()

        // khởi tạo recyclerView
        initRecyclerView()

        searchView.setOnQueryTextListener(onQueryTextListener)
        btnGuide.setOnClickListener {
            mapViewModel.drawRoute()
            containVehicle.visibility = View.VISIBLE
            containTime.visibility = View.VISIBLE
        }
        btnCar.setOnClickListener {
            setBackGroundVehicle(0)
        }
        btnMotorcycle.setOnClickListener {
            setBackGroundVehicle(1)
        }
        btnBike.setOnClickListener {
            setBackGroundVehicle(2)
        }
        btnWalk.setOnClickListener {
            setBackGroundVehicle(3)
        }
        btnReverse.setOnClickListener {
            mapViewModel.reverse()
            mapViewModel.drawRoute()
        }
    }

    private fun observe() {
        mapViewModel.textBlue.observe(this, {
            txtTimeBlue.text = it
        })
        mapViewModel.textGreen.observe(this, {
            txtTimeGreen.text = it
        })
    }

    private fun setBackGroundVehicle(type: Int) {
        mapViewModel.typeVehicle = type
        when (type) {
            0 -> {
                btnBike.setBackgroundColor(Color.WHITE)
                btnWalk.setBackgroundColor(Color.WHITE)
                btnCar.setBackgroundColor(Color.parseColor("#D5C5C5"))
                btnMotorcycle.setBackgroundColor(Color.WHITE)
            }
            1 -> {
                btnBike.setBackgroundColor(Color.WHITE)
                btnWalk.setBackgroundColor(Color.WHITE)
                btnCar.setBackgroundColor(Color.WHITE)
                btnMotorcycle.setBackgroundColor(Color.parseColor("#D5C5C5"))
            }
            2 -> {
                btnBike.setBackgroundColor(Color.parseColor("#D5C5C5"))
                btnWalk.setBackgroundColor(Color.WHITE)
                btnCar.setBackgroundColor(Color.WHITE)
                btnMotorcycle.setBackgroundColor(Color.WHITE)
            }
            3 -> {
                btnBike.setBackgroundColor(Color.WHITE)
                btnWalk.setBackgroundColor(Color.parseColor("#D5C5C5"))
                btnCar.setBackgroundColor(Color.WHITE)
                btnMotorcycle.setBackgroundColor(Color.WHITE)
            }
        }
        if (mapViewModel.isDraw) {
            mapViewModel.clearMap()
            mapViewModel.drawRoute()
        }
    }

    private fun getCurrentLocation() {
        checkPermission()
        fusedLocation!!.lastLocation.addOnSuccessListener {
            if (it != null) {
                mapFragment!!.init { error ->
                    if (error == OnEngineInitListener.Error.NONE) {
                        val location = GeoCoordinate(it.latitude, it.longitude)
                        mapViewModel.map = mapFragment!!.map
                        mapViewModel.myLocation = location
                        mapViewModel.setCenterLocation(location, true)
                        mapFragment!!.mapGesture!!.addOnGestureListener(gestureListener, 100, true)
                    }
                }
            }
        }
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

    @SuppressLint("NotifyDataSetChanged")
    private fun initRecyclerView() {
        adapter = RecyclerViewAdapter(listSearch)
        adapter.setCallback {
            mapViewModel.clearMap()
            val placeLink = listSearch[it] as PlaceLink
            mapViewModel.findLocation = GeoCoordinate(placeLink.position!!)
            mapViewModel.setCenterLocation(placeLink.position!!, false)
            listLocation.visibility = View.GONE
            listSearch.clear()
            adapter.notifyDataSetChanged()
        }
        listLocation.adapter = adapter
        listLocation.layoutManager = LinearLayoutManager(applicationContext)
        listLocation.addItemDecoration(DividerItemDecoration(applicationContext,
            DividerItemDecoration.VERTICAL))
    }

    private val gestureListener = object :
        MapGesture.OnGestureListener.OnGestureListenerAdapter() {
        override fun onTapEvent(p: PointF): Boolean {
            mapViewModel.clickEvent(p)
            return false
        }

        override fun onLongPressEvent(p: PointF): Boolean {
            mapViewModel.clickEvent(p)
            showDialog()
            return false
        }
    }

    private fun showDialog() {
        val selectedMarker = mapViewModel.allObject[mapViewModel.allObject.size - 1] as MapMarker
        ReverseGeocodeRequest(selectedMarker.coordinate).execute { p0, _ ->
            AlertDialog.Builder(this)
                .setTitle("Địa chỉ")
                .setMessage(p0?.address!!.text)
                .setNegativeButton("OK") { _, _ -> }
                .show()
        }
    }

    private val onQueryTextListener = object : SearchView.OnQueryTextListener {
        @SuppressLint("NotifyDataSetChanged")
        override fun onQueryTextSubmit(query: String?): Boolean {
            if (query != null && query != "") {
                val searchRequest = SearchRequest(query)
                searchRequest.setSearchCenter(mapViewModel.map!!.center)
                searchRequest.execute { discoveryResultPage, errorCode ->
                    if (errorCode == ErrorCode.NONE) {
                        listSearch.clear()
                        listSearch.addAll(discoveryResultPage!!.items)
                        adapter.notifyDataSetChanged()
                    }
                }
            }
            return false
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            if (newText.equals("")) {
                listLocation.visibility = View.GONE
            } else {
                listLocation.visibility = View.VISIBLE
                containVehicle.visibility = View.GONE
                containTime.visibility = View.GONE
            }
            return false
        }
    }

}


