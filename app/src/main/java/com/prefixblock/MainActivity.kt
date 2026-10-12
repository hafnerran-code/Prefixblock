
package com.prefixblock

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var prefixes: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 40, 32, 24)
        }

        layout.addView(TextView(this).apply {
            text = "PrefixBlock"
            textSize = 28f
        })

        layout.addView(TextView(this).apply {
            text = "הזן קידומות לחסימה, מופרדות בפסיקים"
            textSize = 16f
        })

        prefixes = EditText(this).apply {
            hint = "לדוגמה: 073214, 072555"
            setText(
                getSharedPreferences("settings", MODE_PRIVATE)
                    .getString("prefixes", "073214") ?: "073214"
            )
        }

        layout.addView(prefixes)

        layout.addView(Button(this).apply {
            text = "שמור קידומות"
            setOnClickListener {
                val value = prefixes.text.toString()
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString(",")

                getSharedPreferences("settings", MODE_PRIVATE)
                    .edit()
                    .putString("prefixes", value)
                    .apply()

                Toast.makeText(
                    this@MainActivity,
                    "הקידומות נשמרו",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        layout.addView(Button(this).apply {
            text = "הפעל סינון שיחות"
            setOnClickListener {
                requestContactsPermission()
                requestScreeningRole()
            }
        })

        layout.addView(TextView(this).apply {
            text = "יש להגדיר את PrefixBlock כאפליקציית סינון שיחות."
        })

        setContentView(layout)
    }

    private fun requestContactsPermission() {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_CONTACTS), 101
            )
        }
    }

    private fun requestScreeningRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(
                Context.ROLE_SERVICE
            ) as RoleManager

            if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
            ) {
                startActivityForResult(
                    roleManager.createRequestRoleIntent(
                        RoleManager.ROLE_CALL_SCREENING
                    ),
                    102
                )
            } else {
                Toast.makeText(
                    this,
                    "בדוק שהרשאת סינון השיחות פעילה",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(
                this,
                "סינון שיחות אוטומטי דורש Android 10 ומעלה בגרסה זו",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
