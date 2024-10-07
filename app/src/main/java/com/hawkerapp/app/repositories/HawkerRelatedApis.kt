package com.hawkerapp.app.repositories

import android.content.Context
import android.widget.Toast
import com.hawkerapp.app.models.HawkerInfo
import com.hawkerapp.app.models.UserData
import com.hawkerapp.app.network.RetrofitHelper
import com.hawkerapp.app.utils.LocationProvider


class HawkerRelatedApis {
    companion object {
        fun getHawkersWithItem(context: Context, appContext: Context, item: String, onSuccess: (List<HawkerInfo>) -> Unit) {
            LocationProvider.init(context)
            LocationProvider.getLocation(context,
                { location ->
                    RetrofitHelper.getHawkersWithItem(item, location) {
                        Toast.makeText(appContext, "Searching Hawkers Selling: ${item.capitalize()}", Toast.LENGTH_SHORT).show()
                        onSuccess(it)
                    }
                },
                { error ->
                    Toast.makeText(appContext, error, Toast.LENGTH_SHORT).show()

                }
            )
        }


        fun senUserRequestToHawker(context: Context, hawkerInfo: HawkerInfo, name: String, note: String) {
            Toast.makeText(context, "Calling ${hawkerInfo.name}", Toast.LENGTH_SHORT).show()
            LocationProvider.init(context)
            LocationProvider.getLocation(context,
                { location ->
                    val userData = UserData(
                        hawkerInfo.id,
                        name,
                        "1234567890",
                        location,
                        note,
                    )
                    RetrofitHelper.sendUserRequest(userData) {
                        Toast.makeText(
                            context,
                            "Request sent to ${hawkerInfo.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                },
                { error ->
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}