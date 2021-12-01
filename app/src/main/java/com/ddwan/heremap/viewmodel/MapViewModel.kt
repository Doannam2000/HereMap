package com.ddwan.heremap.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.here.android.mpa.common.GeoCoordinate

class MapViewModel : ViewModel() {

    var myLocation = MutableLiveData<GeoCoordinate>()
    var typeVehicle = MutableLiveData<Int>()
    var findLocation = MutableLiveData<GeoCoordinate>()

    fun setMyLocation(location: GeoCoordinate) {
        myLocation.value = location
    }

    fun setFindLocation(location: GeoCoordinate) {
        findLocation.value = location
    }

    fun setTypeVehicle(type: Int) {
        typeVehicle.value = type
    }


}