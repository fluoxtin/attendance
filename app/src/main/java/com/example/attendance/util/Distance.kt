package com.example.attendance.util

import com.example.attendance.model.Location
import kotlin.math.*

object Distance {

    private const val EARTH_RADIUS = 6378137.0

    fun getDistance(p1 : Location, p2 : Location) :Double {
        val a = radian(p1.latitude) - radian(p2.latitude)
        val b = radian(p1.longitude) - radian(p2.longitude)
        var s =  2 * asin(
            sqrt(
                sin(a / 2).pow(2.0)
                + cos(radian(p1.latitude)) *
                        cos(radian(p2.latitude)) *
                        sin(b / 2).pow(2.0)
            )
        )
        s *= EARTH_RADIUS
        s = ((s * 10000).roundToInt() / 10000).toDouble()
        return s
    }

    /**
     * @param d 经度/维度
     * @return 弧度
     */
    private fun radian(d : Double) : Double {
        return d * Math.PI  / 180.0
    }

}