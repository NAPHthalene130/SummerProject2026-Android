package com.trafficmanagement.android.data.remote

import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import java.net.URI
import java.util.concurrent.Executors

object RemoteImageLoader {
  private val executor = Executors.newFixedThreadPool(3)
  private val mainHandler = Handler(Looper.getMainLooper())

  fun load(imageView: ImageView, source: String) {
    val url = if (source.startsWith("/")) ApiEndpointManager.baseUrl() + source else source
    imageView.tag = url
    executor.execute {
      val bitmap = runCatching {
        URI.create(url).toURL().openStream().use(BitmapFactory::decodeStream)
      }.getOrNull()
      mainHandler.post {
        if (imageView.tag == url && bitmap != null) imageView.setImageBitmap(bitmap)
      }
    }
  }
}
