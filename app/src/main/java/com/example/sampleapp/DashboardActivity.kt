package com.example.sampleapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val fabRefer = findViewById<FloatingActionButton>(R.id.fab_refer)

        // Default Screen
        replaceFragment(HomeFragment())

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.nav_offers -> {
                    replaceFragment(OffersFragment())
                    true
                }
                R.id.nav_my_offers -> {
                    replaceFragment(MyOffersFragment())
                    true
                }
                R.id.nav_profile -> {
                    replaceFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }

        fabRefer.setOnClickListener {
            replaceFragment(ReferFragment())
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}

// 1. Home Fragment (Fintech & Live Data)
class HomeFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private val httpClient = OkHttpClient()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        sessionManager = SessionManager(requireContext())

        val tvWelcome = view.findViewById<TextView>(R.id.tv_welcome_name)
        val tvBalance = view.findViewById<TextView>(R.id.tv_wallet_coins)
        val tvTotalEarned = view.findViewById<TextView>(R.id.tv_total_earned_badge)

        tvWelcome.text = "Hey, ${sessionManager.getUserName()} 👋"

        fetchDashboardData(tvBalance, tvTotalEarned)

        return view
    }

    private fun fetchDashboardData(tvBalance: TextView, tvTotalEarned: TextView) {
        val token = sessionManager.getAuthToken() ?: return
        val json = JSONObject().apply { put("token", token) }
        val body = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://paisaloots.site/api/get_dashboard.php")
            .post(body)
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                val resStr = response.body?.string() ?: return
                try {
                    val resJson = JSONObject(resStr)
                    if (resJson.optBoolean("success")) {
                        val userData = resJson.getJSONObject("user")
                        val balance = userData.optString("balance", "0.00")
                        val earned = userData.optString("total_earned", "0.00")
                        activity?.runOnUiThread {
                            tvBalance.text = balance
                            tvTotalEarned.text = "Total Earned: ₹$earned"
                        }
                    }
                } catch (e: Exception) {}
            }
        })
    }
}

// 2. Offers Fragment
class OffersFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val tv = TextView(context)
        tv.text = "All Offers List"
        tv.textSize = 20f
        return tv
    }
}

// 3. My Offers Tracker
class MyOffersFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val tv = TextView(context)
        tv.text = "My Offers Tracker"
        tv.textSize = 20f
        return tv
    }
}

// 4. Profile & Settings
class ProfileFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val tv = TextView(context)
        tv.text = "Profile & Settings"
        tv.textSize = 20f
        return tv
    }
}

// 5. Refer & Earn
class ReferFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val tv = TextView(context)
        tv.text = "Refer & Earn"
        tv.textSize = 20f
        return tv
    }
}
