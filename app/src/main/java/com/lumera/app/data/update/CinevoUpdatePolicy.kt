package com.lumera.app.data.update

/** Test builds are delivered through the signed Shield workflow until a channel is approved. */
object CinevoUpdatePolicy {
    const val inAppUpdatesEnabled = false
    const val deliveryMessage = "Cinevo Test updates are supplied as signed APKs. In-app updates are disabled."
}
