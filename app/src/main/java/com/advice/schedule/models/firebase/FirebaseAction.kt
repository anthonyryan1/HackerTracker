package com.advice.schedule.models.firebase

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class FirebaseAction(val label: String = "", val url: String = "") : Parcelable