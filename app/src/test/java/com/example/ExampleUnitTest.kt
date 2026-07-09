package com.example

import org.junit.Assert.*
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun createJsonBin() {
    try {
      val url = URL("https://api.extendsclass.com/bin")
      val connection = url.openConnection() as HttpURLConnection
      connection.requestMethod = "POST"
      connection.doOutput = true
      connection.setRequestProperty("Content-Type", "application/json")
      
      val initialJson = """
        [
          {
            "countryName": "United States",
            "countryCode": "US",
            "city": "New York",
            "ipAddress": "104.244.42.1",
            "type": "Free",
            "latency": 45,
            "loadPercent": 15,
            "protocol": "WireGuard"
          },
          {
            "countryName": "Singapore",
            "countryCode": "SG",
            "city": "Singapore City",
            "ipAddress": "139.99.120.5",
            "type": "Premium",
            "latency": 80,
            "loadPercent": 35,
            "protocol": "OpenVPN"
          }
        ]
      """.trimIndent()
      
      connection.outputStream.use { os ->
        os.write(initialJson.toByteArray())
      }
      
      val responseCode = connection.responseCode
      if (responseCode == 201 || responseCode == 200) {
        val response = connection.inputStream.bufferedReader().use { it.readText() }
        println("--- EXTENDSCLASS BIN CREATION SUCCESS ---")
        println("Response: $response")
        println("-----------------------------------------")
      } else {
        println("Error response code: $responseCode")
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}

