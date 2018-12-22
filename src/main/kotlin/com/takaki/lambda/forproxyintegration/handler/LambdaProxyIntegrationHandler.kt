package com.takaki.lambda.forproxyintegration.handler

import java.io.*

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler

@Suppress("DEPRECATION")
class LambdaProxyIntegrationHandler :RequestStreamHandler {

    @Throws(IOException::class)
    override fun handleRequest(inputStream: InputStream, outputStream: OutputStream, context: Context)
    {
        val gson = Gson()
        val headerJson = JsonObject()
        val responseJson = JsonObject()
        val responseBody = JsonObject()
        val logger = context.logger

        try {

            val reader = BufferedReader(InputStreamReader(inputStream))
            val parser = JsonParser()
            val event = parser.parse(reader) as JsonObject

            logger.log("event-- " + event.toString())

            //　when header existing in event json object
            if (event.has("headers")) responseBody.addProperty("eventHeader", event["headers"].toString())
            //　when body existing in event json object
            if (
                event.has("body") &&
                !event["body"].isJsonNull
            ) {
                responseBody.addProperty("eventBody", event["body"].toString())
            }

            // the following response body info is required
            responseJson.addProperty("statusCode", "200")
            // body property is required to change string
            responseJson.addProperty("body", responseBody.toString())

        } catch (ex: Exception) {
            responseJson.addProperty("statusCode", "500")
            responseJson.addProperty("exception", ex.toString())
            responseJson.addProperty("body", responseBody.toString())
            logger.log("Error Exception(500,　" + ex.message + "　)")

        }

        // the following response header info is required
        headerJson.addProperty("Access-Control-Allow-Origin", "*")
        responseJson.add("headers", headerJson)
        // this's also required.  if include binary data, true
        responseJson.addProperty("isBase64Encoded", false)

        logger.log("responseJson--  $responseJson")

        val result = gson.toJson(responseJson)

        // send response
        OutputStreamWriter(outputStream, "UTF-8").apply {
            write(result)
            close()
        }

    }
}