package com.ddwan.heremap.viewmodel

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PointF
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ddwan.heremap.R
import com.here.android.mpa.common.GeoCoordinate
import com.here.android.mpa.common.Image
import com.here.android.mpa.mapping.*
import com.here.android.mpa.mapping.Map
import com.here.android.mpa.routing.*
import java.io.IOException

class MapViewModel : ViewModel() {

    var myLocation: GeoCoordinate? = null
    var findLocation: GeoCoordinate? = null
    var typeVehicle = MutableLiveData<Int>()
    var textBlue = MutableLiveData<String>()
    var textGreen = MutableLiveData<String>()
    var map: Map? = null
    lateinit var mapRoute: MapRoute
    var allObject = ArrayList<MapObject>()
    var isDraw = false

    fun setCenterLocation(location: GeoCoordinate, isMyLocation: Boolean) {
        map!!.setCenter(location, Map.Animation.NONE)
        map!!.zoomLevel = 13.0
        dropMarker(location, isMyLocation)
    }

    fun formatText(length: Int, duration: Int): String {
        return "${length / 1000} km, " +
                "${duration / 3600} giờ " +
                "${(duration % 3600) / 60} phút"
    }

    private fun getTypeVehicle(): RouteOptions.TransportMode {
        return when (typeVehicle.value) {
            0 -> RouteOptions.TransportMode.CAR
            1 -> RouteOptions.TransportMode.SCOOTER
            2 -> RouteOptions.TransportMode.BICYCLE
            3 -> RouteOptions.TransportMode.PEDESTRIAN
            else -> RouteOptions.TransportMode.CAR
        }
    }

    private fun getRouteType(type: Boolean): RouteOptions.Type {
        return if (type)
            RouteOptions.Type.SHORTEST
        else
            RouteOptions.Type.FASTEST
    }

    private fun createRoute(
        myLocation: GeoCoordinate,
        findLocation: GeoCoordinate,
        type: Boolean,
    ) {
        val coreRouter = CoreRouter()
        val routePlan = RoutePlan()
        val routeOptions = RouteOptions()
        routeOptions.transportMode = getTypeVehicle()
        routeOptions.setHighwaysAllowed(true)
        routeOptions.routeCount = 1
        routeOptions.routeType = getRouteType(type)
        routePlan.routeOptions = routeOptions
        val startPoint = RouteWaypoint(myLocation)
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
                            textGreen.value = formatText(route.length, duration)
                        } else {
                            textBlue.value = formatText(route.length, duration)
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

    fun clearMap() {
        map!!.removeMapObjects(allObject)
        allObject.clear()
        dropMarker(myLocation!!, true)
    }

    fun drawRoute() {
        createRoute(myLocation!!, findLocation!!, true)
        createRoute(myLocation!!, findLocation!!, false)
        dropMarker(findLocation!!, false)
    }

    private fun dropMarker(position: GeoCoordinate, isMyLocation: Boolean) {
        val img = Image()
        try {
            if (isMyLocation)
                img.setImageResource(R.drawable.marker_blue)
            else {
                img.setImageResource(R.drawable.marker)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val marker = MapMarker()
        marker.icon = img
        marker.coordinate = position
        map!!.addMapObject(marker)
        allObject.add(marker)
    }

    fun reverse() {
        if (isDraw) {
            map!!.removeMapObjects(allObject)
            allObject.clear()
            val p = findLocation
            findLocation = myLocation
            myLocation = p
            dropMarker(findLocation!!, false)
            dropMarker(myLocation!!, true)
        }
    }

    fun clickEvent(p: PointF) {
        val position = map!!.pixelToGeo(p)
        clearMap()
        dropMarker(position!!, false)
        findLocation = position
    }

}