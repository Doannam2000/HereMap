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
import com.here.android.mpa.mapping.Map
import com.here.android.mpa.routing.*
import com.here.android.mpa.search.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import kotlin.collections.ArrayList
import com.here.android.mpa.mapping.MapMarker
import android.widget.Toast
import com.here.android.mpa.search.ReverseGeocodeRequest


class MainActivity : AppCompatActivity() {

    private var myLocation: GeoCoordinate? = null
    private var fusedLocation: FusedLocationProviderClient? = null
    private var mapFragment: AndroidXMapFragment? = null
    private var map: Map? = null
    private var listSearch = ArrayList<DiscoveryResult>()
    lateinit var adapter: RecyclerViewAdapter
    lateinit var mapRoute: MapRoute
    private lateinit var findLocation: GeoCoordinate
    private var allObject = ArrayList<MapObject>()
    private var typeVehicle = 0
    private var isDraw = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragment) as AndroidXMapFragment

        fusedLocation = LocationServices.getFusedLocationProviderClient(this)

        getCurrentLocation()

        initRecyclerView()

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null && query != "") {
                    val searchRequest = SearchRequest(query)
                    searchRequest.setSearchCenter(map!!.center)
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
        })

        btnGuide.setOnClickListener {
            if (this::findLocation.isInitialized) {
                drawRoute()
            }
        }

        btnCar.setOnClickListener {
            typeVehicle = 0
            btnBike.setBackgroundColor(Color.WHITE)
            btnWalk.setBackgroundColor(Color.WHITE)
            btnCar.setBackgroundColor(Color.parseColor("#D5C5C5"))
            btnMotorcycle.setBackgroundColor(Color.WHITE)
            if (isDraw) {
                clearMap()
                drawRoute()
            }
        }
        btnMotorcycle.setOnClickListener {
            typeVehicle = 1
            btnBike.setBackgroundColor(Color.WHITE)
            btnWalk.setBackgroundColor(Color.WHITE)
            btnCar.setBackgroundColor(Color.WHITE)
            btnMotorcycle.setBackgroundColor(Color.parseColor("#D5C5C5"))
            if (isDraw) {
                clearMap()
                drawRoute()
            }
        }
        btnBike.setOnClickListener {
            typeVehicle = 2
            btnBike.setBackgroundColor(Color.parseColor("#D5C5C5"))
            btnWalk.setBackgroundColor(Color.WHITE)
            btnCar.setBackgroundColor(Color.WHITE)
            btnMotorcycle.setBackgroundColor(Color.WHITE)
            if (isDraw) {
                clearMap()
                drawRoute()
            }
        }
        btnWalk.setOnClickListener {
            typeVehicle = 3
            btnBike.setBackgroundColor(Color.WHITE)
            btnWalk.setBackgroundColor(Color.parseColor("#D5C5C5"))
            btnCar.setBackgroundColor(Color.WHITE)
            btnMotorcycle.setBackgroundColor(Color.WHITE)
            if (isDraw) {
                clearMap()
                drawRoute()
            }
        }
        btnReverse.setOnClickListener {
            if (isDraw) {
                clearMap()
                map!!.removeMapObjects(allObject)
                allObject.clear()
                val p = myLocation!!
                myLocation = findLocation
                dropMarker(myLocation!!, true)
                dropMarker(p, false)
                drawRoute()
            }
        }
    }

    private fun drawRoute() {
        createRoute(findLocation, true)
        createRoute(findLocation, false)
        dropMarker(findLocation, false)
        containVehicle.visibility = View.VISIBLE
        containTime.visibility = View.VISIBLE
    }

    private fun dropMarker(position: GeoCoordinate, isMyLocation: Boolean) {
        val img = Image()
        try {
            if (isMyLocation)
                img.setImageResource(R.drawable.marker_blue)
            else {
                img.setImageResource(R.drawable.marker)
                findLocation = position
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val marker = MapMarker()
        marker.icon = img
        marker.coordinate = position
        map!!.addMapObject(marker)
        map!!.zoomLevel = 14.0
        allObject.add(marker)
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
            clearMap()
            isDraw = false
            val placeLink = listSearch[it] as PlaceLink
            dropMarker(GeoCoordinate(placeLink.position!!), false)
            map!!.setCenter(GeoCoordinate(placeLink.position!!), Map.Animation.NONE)
            findLocation = GeoCoordinate(placeLink.position!!)
            listLocation.visibility = View.GONE
            listSearch.clear()
            adapter.notifyDataSetChanged()
        }
        listLocation.adapter = adapter
        listLocation.layoutManager = LinearLayoutManager(applicationContext)
        listLocation.addItemDecoration(DividerItemDecoration(applicationContext,
            DividerItemDecoration.VERTICAL))
    }

    private fun getCurrentLocation() {
        checkPermission()
        fusedLocation!!.lastLocation.addOnSuccessListener {
            if (it != null) {
                mapFragment!!.init { error ->
                    myLocation = GeoCoordinate(it.latitude, it.longitude)
                    if (error == OnEngineInitListener.Error.NONE) {
                        map = mapFragment!!.map!!
                        map!!.setCenter(myLocation!!,
                            Map.Animation.NONE)
                        dropMarker(myLocation!!, true)
                        mapFragment!!.mapGesture!!.addOnGestureListener(gestureListener, 100, true)
                    }
                }
            }
        }
    }

    private val gestureListener = object :
        MapGesture.OnGestureListener.OnGestureListenerAdapter() {
        override fun onTapEvent(p: PointF): Boolean {
            val viewObjectList = map!!.getSelectedObjects(p) as ArrayList<ViewObject>
            for (viewObject in viewObjectList) {
                if (viewObject.baseType == ViewObject.Type.USER_OBJECT) {
                    val mapObject = viewObject as MapObject
                    if (mapObject.type == MapObject.Type.MARKER) {
                        val selectedMarker = viewObject as MapMarker
                        ReverseGeocodeRequest(selectedMarker.coordinate).execute { p0, _ ->
                            Toast.makeText(applicationContext,
                                p0!!.address!!.text,
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            return false
        }

        override fun onLongPressEvent(p: PointF): Boolean {
            val position = map!!.pixelToGeo(p)
            clearMap()
            dropMarker(position!!, false)
            map!!.setCenter(position,Map.Animation.NONE)
            return false
        }
    }

    private fun createRoute(
        findLocation: GeoCoordinate,
        type: Boolean,
    ) {
        val coreRouter = CoreRouter()
        val routePlan = RoutePlan()
        val routeOptions = RouteOptions()
        when (typeVehicle) {
            0 -> routeOptions.transportMode = RouteOptions.TransportMode.CAR
            1 -> routeOptions.transportMode = RouteOptions.TransportMode.SCOOTER
            2 -> routeOptions.transportMode = RouteOptions.TransportMode.BICYCLE
            3 -> routeOptions.transportMode = RouteOptions.TransportMode.PEDESTRIAN
        }
        routeOptions.setHighwaysAllowed(true)
        routeOptions.routeCount = 1
        if (type)
            routeOptions.routeType = RouteOptions.Type.SHORTEST
        else
            routeOptions.routeType = RouteOptions.Type.FASTEST

        routePlan.routeOptions = routeOptions
        val startPoint =
            RouteWaypoint(myLocation!!)
        val destination = RouteWaypoint(findLocation)
        routePlan.addWaypoint(startPoint)
        routePlan.addWaypoint(destination)
        coreRouter.calculateRoute(
            routePlan,
            object : Router.Listener<List<RouteResult>, RoutingError> {
                override fun onProgress(i: Int) {
                }

                @SuppressLint("SetTextI18n")
                override fun onCalculateRouteFinished(
                    routeResults: List<RouteResult>,
                    routingError: RoutingError,
                ) {
                    if (routingError == RoutingError.NONE) {
                        val route = routeResults[0].route
                        mapRoute = MapRoute(route)
                        val duration = route.getTtaExcludingTraffic(Route.WHOLE_ROUTE)!!.duration
                        if (type) {
                            mapRoute.color = Color.GREEN
                            txtTimeGreen.text =
                                "${route.length / 1000} km, " +
                                        "${duration / 3600} giờ " +
                                        "${(duration % 3600) / 60} phút"
                        } else {
                            txtTimeBlue.text =
                                "${route.length / 1000} km, " +
                                        "${duration / 3600} giờ " +
                                        "${(duration % 3600) / 60} phút"
                        }
                        map!!.addMapObject(mapRoute)
                        map!!.zoomTo(route.boundingBox!!, Map.Animation.NONE,
                            Map.MOVE_PRESERVE_ORIENTATION)
                        allObject.add(mapRoute)
                        isDraw = true
                    }
                }
            })
    }

    private fun clearMap() {
        allObject.removeAt(0)
        map!!.removeMapObjects(allObject)
        allObject.clear()
        allObject.addAll(map!!.allMapObjects)
    }

}


