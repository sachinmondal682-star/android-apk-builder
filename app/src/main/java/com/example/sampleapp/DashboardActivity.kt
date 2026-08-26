package com.example.sampleapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val fabRefer = findViewById<FloatingActionButton>(R.id.fab_refer)

        replaceFragment(HomeFragment())

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { replaceFragment(HomeFragment()); true }
                R.id.nav_offers -> { replaceFragment(OffersFragment()); true }
                R.id.nav_my_offers -> { replaceFragment(MyOffersFragment()); true }
                R.id.nav_profile -> { replaceFragment(ProfileFragment()); true }
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

    fun showWithdrawDialog(token: String) {
        val input = EditText(this).apply {
            hint = "Enter UPI ID (e.g. mobile@upi)"
            setPadding(40, 30, 40, 30)
        }

        AlertDialog.Builder(this)
            .setTitle("Redeem Coins (UPI)")
            .setMessage("100 Coins = ₹10.00 Direct Payout\nMinimum: 100 Coins")
            .setView(input)
            .setPositiveButton("Redeem") { _, _ ->
                val upi = input.text.toString().trim()
                if (upi.isNotEmpty()) {
                    processWithdraw(token, 10.0, upi)
                } else {
                    Toast.makeText(this, "Enter valid UPI ID", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun processWithdraw(token: String, amount: Double, upiId: String) {
        val json = JSONObject().apply {
            put("token", token)
            put("amount", amount)
            put("upi_id", upiId)
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url("https://paisaloots.site/api/withdraw.php").post(body).build()

        OkHttpClient().newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(applicationContext, "Network error", Toast.LENGTH_SHORT).show() }
            }
            override fun onResponse(call: Call, response: Response) {
                val res = JSONObject(response.body?.string() ?: "{}")
                runOnUiThread {
                    Toast.makeText(applicationContext, res.optString("message", "Done"), Toast.LENGTH_LONG).show()
                }
            }
        })
    }
}

// 1. Home Fragment (Live Balance + Dynamic Offers)
class HomeFragment : Fragment() {
    private lateinit var sessionManager: SessionManager
    private val httpClient = OkHttpClient()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        sessionManager = SessionManager(requireContext())

        val tvWelcome = view.findViewById<TextView>(R.id.tv_welcome_name)
        val tvBalance = view.findViewById<TextView>(R.id.tv_wallet_coins)
        val tvTotalEarned = view.findViewById<TextView>(R.id.tv_total_earned_badge)
        val offersContainer = view.findViewById<LinearLayout>(R.id.offers_list_container)
        val btnWithdraw = view.findViewById<Button>(R.id.btn_withdraw_quick)

        tvWelcome.text = "Hey, ${sessionManager.getUserName()} 👋"

        btnWithdraw.setOnClickListener {
            sessionManager.getAuthToken()?.let { (activity as? DashboardActivity)?.showWithdrawDialog(it) }
        }

        fetchDashboardData(tvBalance, tvTotalEarned)
        fetchTrendingOffers(inflater, offersContainer)

        return view
    }

    private fun fetchDashboardData(tvBalance: TextView, tvTotalEarned: TextView) {
        val token = sessionManager.getAuthToken() ?: return
        val body = JSONObject().apply { put("token", token) }.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url("https://paisaloots.site/api/get_dashboard.php").post(body).build()

        httpClient.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                try {
                    val res = JSONObject(response.body?.string() ?: "{}")
                    if (res.optBoolean("success")) {
                        val user = res.getJSONObject("user")
                        activity?.runOnUiThread {
                            tvBalance.text = user.optString("balance", "0.00")
                            tvTotalEarned.text = "Total Earned: ₹${user.optString("total_earned", "0.00")}"
                        }
                    }
                } catch (e: Exception) {}
            }
        })
    }

    private fun fetchTrendingOffers(inflater: LayoutInflater, container: LinearLayout) {
        val req = Request.Builder().url("https://paisaloots.site/api/get_offers.php").get().build()
        httpClient.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                try {
                    val res = JSONObject(response.body?.string() ?: "{}")
                    if (res.optBoolean("success")) {
                        val offers = res.getJSONArray("offers")
                        activity?.runOnUiThread {
                            container.removeAllViews()
                            for (i in 0 until offers.length()) {
                                val item = offers.getJSONObject(i)
                                val card = inflater.inflate(R.layout.item_offer_card, container, false)
                                card.findViewById<TextView>(R.id.tv_offer_title).text = item.optString("title")
                                card.findViewById<TextView>(R.id.tv_offer_desc).text = item.optString("tagline")
                                card.findViewById<TextView>(R.id.tv_offer_reward).text = "+ ${item.optString("payout_coins")}"
                                card.setOnClickListener {
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.optString("offer_url"))))
                                }
                                container.addView(card)
                            }
                        }
                    }
                } catch (e: Exception) {}
            }
        })
    }
}

// 2. All Offers Screen
class OffersFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val offersContainer = view.findViewById<LinearLayout>(R.id.offers_list_container)

        val req = Request.Builder().url("https://paisaloots.site/api/get_offers.php").get().build()
        OkHttpClient().newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                try {
                    val res = JSONObject(response.body?.string() ?: "{}")
                    val offers = res.optJSONArray("offers") ?: JSONArray()
                    activity?.runOnUiThread {
                        offersContainer.removeAllViews()
                        for (i in 0 until offers.length()) {
                            val item = offers.getJSONObject(i)
                            val card = inflater.inflate(R.layout.item_offer_card, offersContainer, false)
                            card.findViewById<TextView>(R.id.tv_offer_title).text = item.optString("title")
                            card.findViewById<TextView>(R.id.tv_offer_desc).text = item.optString("tagline")
                            card.findViewById<TextView>(R.id.tv_offer_reward).text = "+ ${item.optString("payout_coins")}"
                            card.setOnClickListener {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.optString("offer_url"))))
                            }
                            offersContainer.addView(card)
                        }
                    }
                } catch (e: Exception) {}
            }
        })
        return view
    }
}

// 3. My Tracker History
class MyOffersFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_tracker, container, false)
        val itemsContainer = view.findViewById<LinearLayout>(R.id.tracker_items_container)
        val session = SessionManager(requireContext())
        val token = session.getAuthToken() ?: return view

        val body = JSONObject().apply { put("token", token) }.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url("https://paisaloots.site/api/get_user_history.php").post(body).build()

        OkHttpClient().newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                try {
                    val res = JSONObject(response.body?.string() ?: "{}")
                    val withdraws = res.optJSONArray("withdrawals") ?: JSONArray()
                    activity?.runOnUiThread {
                        itemsContainer.removeAllViews()
                        if (withdraws.length() == 0) {
                            val tv = TextView(context).apply { 
                                text = "No withdrawal history yet."
                                setPadding(20, 40, 20, 20)
                            }
                            itemsContainer.addView(tv)
                        }
                        for (i in 0 until withdraws.length()) {
                            val w = withdraws.getJSONObject(i)
                            val tv = TextView(context).apply {
                                text = "₹${w.optString("amount")} via ${w.optString("payout_details")} | Status: ${w.optString("status").uppercase()}"
                                setPadding(20, 20, 20, 20)
                                textSize = 14f
                            }
                            itemsContainer.addView(tv)
                        }
                    }
                } catch (e: Exception) {}
            }
        })
        return view
    }
}

// 4. Refer & Earn
class ReferFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_refer, container, false)
        val tvCode = view.findViewById<TextView>(R.id.tv_refer_code)
        val btnCopy = view.findViewById<TextView>(R.id.btn_copy_code)
        val btnShare = view.findViewById<Button>(R.id.btn_share_whatsapp)

        val code = "LOOT" + (1000..9999).random()
        tvCode.text = code

        btnCopy.setOnClickListener {
            val clip = context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clip?.setPrimaryClip(ClipData.newPlainText("Referral Code", code))
            Toast.makeText(context, "Copied: $code", Toast.LENGTH_SHORT).show()
        }

        btnShare.setOnClickListener {
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Download Paisa Loots & enter my code $code to earn real cash! https://paisaloots.site")
            }
            startActivity(Intent.createChooser(share, "Share Code"))
        }

        return view
    }
}

// 5. Profile & Settings
class ProfileFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        val session = SessionManager(requireContext())

        view.findViewById<TextView>(R.id.tv_prof_name).text = session.getUserName()
        view.findViewById<TextView>(R.id.tv_prof_role).text = "Account: USER"

        view.findViewById<Button>(R.id.btn_withdraw_profile).setOnClickListener {
            session.getAuthToken()?.let { (activity as? DashboardActivity)?.showWithdrawDialog(it) }
        }

        view.findViewById<Button>(R.id.btn_logout).setOnClickListener {
            session.clearSession()
            val intent = Intent(activity, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            activity?.finish()
        }

        return view
    }
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
