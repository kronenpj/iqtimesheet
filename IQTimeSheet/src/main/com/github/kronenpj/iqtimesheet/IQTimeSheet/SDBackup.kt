package com.github.kronenpj.iqtimesheet.IQTimeSheet

/**
 * Class to export the current database to the SDcard.

 * @author Paul Kronenwetter <kronenpj></kronenpj>@gmail.com>
 * *
 * @author Neil http://stackoverflow.com/users/319625/user319625
 */

import android.os.Environment
import android.util.Log

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SDBackup {
    private val TAG = "SDBackup"

    fun doSDBackup(databaseName: String, packageName: String): Boolean {
        try {
            val sd = Environment.getExternalStorageDirectory()
            // File extFilesDir =
            val data = Environment.getDataDirectory()
            Log.d(TAG, "SDBackup: databaseName: $databaseName")
            Log.d(TAG, "SDBackup: packageName: $packageName")

            if (sd.canWrite()) {
                val currentDBPath = "/data/$packageName/databases/$databaseName"
                val backupDBPath = packageName
                        .substring(packageName.lastIndexOf('.') + 1)
                val currentDB = File(data, currentDBPath)
                val backupDir = File(sd, backupDBPath)
                val backupDB = File(sd, "$backupDBPath/$databaseName")

                val currentPrefPath = "/data/$packageName/shared_prefs/${packageName}_preferences.xml"
                val currentPref = File(data, currentPrefPath)
                val backupPref = File(sd, "$backupDBPath/preferences.xml")
                Log.d(TAG, "Current pref: " + currentPrefPath)
                Log.d(TAG, "Backup pref : $backupDBPath/preferences.xml")

                val formatter = SimpleDateFormat("yyyyMMdd", Locale.US)
                val currentTime_1 = Date()
                val dateString = formatter.format(currentTime_1)
                val backupDBDate = File(sd, "$backupDBPath/$databaseName-$dateString")
                Log.d(TAG, "Backup DB Date: $backupDBPath/$databaseName-$dateString")

                Log.d(TAG, "SDBackup: currentDBPath: " + currentDBPath)
                Log.d(TAG, "SDBackup: backupDBPath: " + backupDBPath)
                Log.d(TAG, "SDBackup: backupDBDatePath: $backupDBPath-$dateString")

                if (currentDB.exists()) {
                    Log.d(TAG, "SDBackup: Checking directory: $backupDBPath")
                    if (!backupDir.exists()) {
                        Log.d(TAG, "SDBackup: Creating directory: $backupDBPath")
                        if (!backupDir.mkdir()) {
                            Log.d(TAG, "SDBackup: Directory creation failed.")
                        }
                    } else {
                        Log.d(TAG, "SDBackup: $backupDBPath exists.")
                    }
                    var src = FileInputStream(currentDB).channel
                    var dst = FileOutputStream(backupDB).channel
                    dst.transferFrom(src, 0, src.size())
                    dst.close()
                    src.close()
                    // Make a second backup with today's date.
                    src = FileInputStream(currentDB).channel
                    dst = FileOutputStream(backupDBDate).channel
                    dst.transferFrom(src, 0, src.size())
                    dst.close()
                    src.close()

                    // Make a backup of the preferences
                    try {
                        src = FileInputStream(currentPref).channel
                        dst = FileOutputStream(backupPref).channel
                        dst.transferFrom(src, 0, src.size())
                        dst.close()
                        src.close()
                    } catch (e: FileNotFoundException) {
                        Log.i(TAG, "FileNotFoundException: $e")
                    }

                    return true
                } else {
                    Log.d(TAG, "SDBackup: $currentDBPath doesn't exist.")
                }
            } else {
                Log.d(TAG, "Backups aren't allowed by the OS. Permission must be granted.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "SDBackup threw exception: $e")
        }

        return false
    }

    fun doSDRestore(databaseName: String, packageName: String): Boolean {
        try {
            val sd = Environment.getExternalStorageDirectory()
            val data = Environment.getDataDirectory()
            Log.d(TAG, "SDRestore: databaseName: $databaseName")
            Log.d(TAG, "SDRestore: packageName: $packageName")

            if (sd.canWrite()) {
                val currentDBPath = "/data/$packageName/databases/$databaseName"
                val backupDBPath = packageName
                        .substring(packageName.lastIndexOf('.') + 1)
                val currentDB = File(data, currentDBPath)
                val currentDBbak = File(data, "$currentDBPath.bak")
                val backupDB = File(sd, "$backupDBPath/$databaseName")
                Log.d(TAG, "SDRestore: currentDBPath: $currentDBPath")
                Log.d(TAG, "SDRestore: backupDBPath: $backupDBPath")

                val currentPrefPath = "/data/$packageName/shared_prefs/${packageName}_preferences.xml"
                val currentPref = File(data, currentPrefPath)
                val backupPref = File(sd, "$backupDBPath/preferences.xml")

                if (currentDBbak.exists()) currentDBbak.delete()

                val src: FileChannel
                val dst: FileChannel
                if (backupDB.exists()) {
                    currentDB.renameTo(currentDBbak)
                    src = FileInputStream(backupDB).channel
                    dst = FileOutputStream(currentDB).channel
                    dst.transferFrom(src, 0, src.size())
                    src.close()
                    dst.close()
                    return true
                } else {
                    Log.d(TAG, "SDRestore: $currentDBPath doesn't exist.")
                }

                // Make a backup of the preferences
                try {
                    src = FileInputStream(backupPref).channel
                    dst = FileOutputStream(currentPref).channel
                    dst.transferFrom(src, 0, src.size())
                    dst.close()
                    src.close()
                } catch (e: FileNotFoundException) {
                    Log.i(TAG, "FileNotFoundException: $e")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "SDRestore threw exception: $e")
            Log.e(TAG, e.localizedMessage)
        }

        return false
    }
}