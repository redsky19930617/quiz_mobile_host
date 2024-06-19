package com.quiz.hostapp.network

import android.content.Context
import com.quiz.hostapp.MyApplication
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

//http://167.71.194.90/
const val BASE_URL = "https://api.quizmobb.com/"; //
//const val BASE_URL = "http://192.168.104.82:4000/";
object ApiClient {

    fun getApiClient(context: Context): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(getOkHttpClient(context))
            .build()
    }

//    private val okHttpClient: OkHttpClient
//        get() {
//            try {
//                val httpLoggingInterceptor = HttpLoggingInterceptor()
//                httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
//                val httpClient = OkHttpClient.Builder()
//                httpClient.interceptors().add(httpLoggingInterceptor)
//                httpClient.interceptors().add(MoxieInterceptor())
//                httpClient.readTimeout(180, TimeUnit.SECONDS)
//                httpClient.connectTimeout(180, TimeUnit.SECONDS)
//                return httpClient.build()
//            } catch (e: Exception) {
//                throw RuntimeException(e)
//            }
//        }

    private fun getOkHttpClient(context:Context):OkHttpClient{
        val httpLoggingInterceptor = HttpLoggingInterceptor()
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        val httpClient = OkHttpClient.Builder()
        httpClient.interceptors().add(httpLoggingInterceptor)
        httpClient.interceptors().add(MoxieInterceptor(context))
        httpClient.readTimeout(180, TimeUnit.SECONDS)
        httpClient.connectTimeout(180, TimeUnit.SECONDS)
        return httpClient.build()
    }
}