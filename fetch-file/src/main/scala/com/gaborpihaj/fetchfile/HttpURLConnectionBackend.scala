package com.gaborpihaj.fetchfile

import cats.effect._
import java.io.InputStream
import java.net.{HttpURLConnection, URL}

object HttpURLConnectionBackend {
  def apply[F[_]: Sync](): URL => Resource[F, InputStream] = { url =>
    Resource.make {
      Sync[F].delay {
        val connection = url.openConnection().asInstanceOf[HttpURLConnection]
        connection.setRequestProperty(
          "User-Agent",
          "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11"
        )
        connection.connect()
        connection.getInputStream()
      }
    } { inputStream =>
      Sync[F].delay(inputStream.close())
    }

  }
}
