package net.veldor.flibustaloader.http

import java.io.InputStream

class WebResponse(
    val statusCode: Int,
    val inputStream: InputStream?,
    val contentType: String?,
    val headers: HashMap<String, String>?,
    val contentLength: Int = 0
)