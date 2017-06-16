/*
 * Copyright 2010 TimeSheet authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.kronenpj.iqtimesheet.IQTimeSheet

import java.util.Calendar
import java.util.GregorianCalendar

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.CursorIndexOutOfBoundsException
import android.database.SQLException
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import android.widget.Toast

import com.github.kronenpj.iqtimesheet.TimeHelpers

/**
 * Simple time sheet database helper class. Defines the basic CRUD operations
 * for the time sheet application, and gives the ability to list all entries as
 * well as retrieve or modify a specific entry.
 *
 * Graciously stolen from the Android Notepad example.

 * @author Paul Kronenwetter <kronenpj></kronenpj>@gmail.com>
 */
class TimeSheetDbAdapter
/**
 * Constructor - takes the context to allow the database to be
 * opened/created
 */
(private val mCtx: Context) {

    private class DatabaseHelper internal constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            Log.w(TAG, "In TimeSheetDbAdapter.onCreate.")
            db.execSQL(TASK_TABLE_CREATE)
            db.execSQL(CLOCK_TABLE_CREATE)
            db.execSQL(METADATA_CREATE)
            db.execSQL(TASKSPLIT_TABLE_CREATE)
            db.execSQL(ENTRYITEMS_VIEW_CREATE)
            db.execSQL(ENTRYREPORT_VIEW_CREATE)
            db.execSQL(TASKSPLITREPORT_VIEW_CREATE)
            db.execSQL(TASKS_INDEX)
            db.execSQL(CHARGENO_INDEX)
            db.execSQL(TIMEIN_INDEX)
            db.execSQL(TIMEOUT_INDEX)
            db.execSQL(SPLIT_INDEX)

            var initialValues = ContentValues()
            initialValues.put(KEY_VERSION, DATABASE_VERSION)
            db.insert(DATABASE_METADATA, null, initialValues)

            initialValues = ContentValues()
            initialValues.put(KEY_TASK, "Example task entry")
            initialValues.put(KEY_LASTUSED, System.currentTimeMillis())
            db.insert(TASKS_DATABASE_TABLE, null, initialValues)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ".")
            // db.execSQL("ALTER TABLE TimeSheet ADD...");
            // db.execSQL("UPDATE TimeSheet SET ");
            when (oldVersion) {
                1 -> {
                    Log.d(TAG, "Old DB version <= 1")
                    Log.d(TAG, "Running: $CHARGENO_INDEX")
                    db.execSQL(CHARGENO_INDEX)
                    Log.d(TAG, "Running: $TIMEIN_INDEX")
                    db.execSQL(TIMEIN_INDEX)
                    Log.d(TAG, "Running: $TIMEOUT_INDEX")
                    db.execSQL(TIMEOUT_INDEX)
                    Log.d(TAG, "Old DB version <= 2")
                    Log.d(TAG, "Running: $TASK_TABLE_ALTER3")
                    db.execSQL(TASK_TABLE_ALTER3)
                    Log.d(TAG, "Running: $TASKSPLIT_TABLE_CREATE")
                    db.execSQL(TASKSPLIT_TABLE_CREATE)
                    Log.d(TAG, "Running: $TASKSPLITREPORT_VIEW_CREATE")
                    db.execSQL(TASKSPLITREPORT_VIEW_CREATE)
                    Log.d(TAG, "Running: $SPLIT_INDEX")
                    db.execSQL(SPLIT_INDEX)
                    if (newVersion != oldVersion)
                        db.execSQL("UPDATE $DATABASE_METADATA SET $KEY_VERSION = newVersion")
                }
                2 -> {
                    Log.d(TAG, "Old DB version <= 2")
                    Log.d(TAG, "Running: $TASK_TABLE_ALTER3")
                    db.execSQL(TASK_TABLE_ALTER3)
                    Log.d(TAG, "Running: $TASKSPLIT_TABLE_CREATE")
                    db.execSQL(TASKSPLIT_TABLE_CREATE)
                    Log.d(TAG, "Running: $TASKSPLITREPORT_VIEW_CREATE")
                    db.execSQL(TASKSPLITREPORT_VIEW_CREATE)
                    Log.d(TAG, "Running: $SPLIT_INDEX")
                    db.execSQL(SPLIT_INDEX)
                    if (newVersion != oldVersion)
                        db.execSQL("UPDATE $DATABASE_METADATA SET $KEY_VERSION=$newVersion")
                }
                else -> if (newVersion != oldVersion)
                    db.execSQL("UPDATE $DATABASE_METADATA SET $KEY_VERSION=$newVersion")
            }
        }
    }

    /**
     * Open the time sheet database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure

     * @return this (self reference, allowing this to be chained in an
     * * initialization call)
     * *
     * @throws SQLException if the database could be neither opened or created
     */
    @Throws(SQLException::class)
    fun open(): TimeSheetDbAdapter {
        if (mDb != null) {
            return this
        }
        mDbHelper = DatabaseHelper(mCtx)
        try {
            mDb = mDbHelper!!.writableDatabase
        } catch (e: NullPointerException) {
            mDb = mCtx.openOrCreateDatabase(DATABASE_NAME, 0, null)
        }

        return this
    }

    /**
     * Close the database.
     */
    fun close() {
        mDbHelper!!.close()
        mDb = null
    }

    /**
     * Create a new time entry using the charge number provided. If the entry is
     * successfully created return the new rowId for that note, otherwise return
     * a -1 to indicate failure.

     * @param task the charge number for the entry
     * *
     * @return rowId or -1 if failed
     */
    fun createEntry(task: String): Long {
        val chargeno = getTaskIDByName(task)
        return createEntry(chargeno)
    }

    /**
     * Create a new time entry using the charge number provided. If the entry is
     * successfully created return the new rowId for that note, otherwise return
     * a -1 to indicate failure.

     * @param task   the charge number for the entry
     * *
     * @param timeIn the time in milliseconds of the clock-in
     * *
     * @return rowId or -1 if failed
     */
    fun createEntry(task: String, timeIn: Long): Long {
        val chargeno = getTaskIDByName(task)
        return createEntry(chargeno, timeIn)
    }

    /**
     * Create a new time entry using the charge number provided. If the entry is
     * successfully created return the new rowId for that note, otherwise return
     * a -1 to indicate failure.

     * @param chargeno the charge number for the entry
     * *
     * @param timeIn   the time in milliseconds of the clock-in
     * *
     * @return rowId or -1 if failed
     */
    @JvmOverloads fun createEntry(chargeno: Long, timeInP: Long = System.currentTimeMillis()): Long {
        var timeIn = timeInP
        // if (TimeSheetActivity.prefs.getAlignMinutesAuto()) {
        // timeIn = TimeHelpers.millisToAlignMinutes(timeIn,
        // TimeSheetActivity.prefs.getAlignMinutes());
        // }
        if (TimeSheetActivity.prefs.alignMinutesAuto) {
            timeIn = TimeHelpers.millisToAlignMinutes(timeIn,
                    TimeSheetActivity.prefs.alignMinutes)
        }

        incrementTaskUsage(chargeno)

        val initialValues = ContentValues()
        initialValues.put(KEY_CHARGENO, chargeno)
        initialValues.put(KEY_TIMEIN, timeIn)

        Log.d(TAG, "createEntry: $chargeno at $timeIn (${TimeHelpers.millisToTimeDate(timeIn)})")
        if (mDb == null)
            open()
        return mDb!!.insert(CLOCK_DATABASE_TABLE, null, initialValues)
    }

    /**
     * Close last time entry. If the entry is successfully closed, return the
     * rowId for that entry, otherwise return a -1 to indicate failure.

     * @return rowId or -1 if failed
     */
    fun closeEntry(): Long {
        val rowID = lastClockEntry()
        val c = fetchEntry(rowID, KEY_CHARGENO)

        c.moveToFirst()

        val chargeno = c.getLong(0)
        try {
            c.close()
        } catch (e: IllegalStateException) {
            Log.d(TAG, "closeEntry $e")
        }

        return closeEntry(chargeno)
    }

    /**
     * Close supplied time entry. If the entry is successfully closed, return
     * the rowId for that entry, otherwise return a -1 to indicate failure.

     * @param task the charge number for the entry
     * *
     * @return rowId or -1 if failed
     */
    fun closeEntry(task: String): Long {
        val chargeno = getTaskIDByName(task)
        return closeEntry(chargeno)
    }

    /**
     * Close supplied time entry with the supplied time. If the entry is
     * successfully closed, return the rowId for that entry, otherwise return a
     * -1 to indicate failure.

     * @param task    the charge number for the entry
     * *
     * @param timeOut the time in milliseconds of the clock-out
     * *
     * @return rowId or -1 if failed
     */
    fun closeEntry(task: String, timeOut: Long): Long {
        val chargeno = getTaskIDByName(task)
        return closeEntry(chargeno, timeOut)
    }

    /**
     * Close an existing time entry using the charge number provided. If the
     * entry is successfully created return the new rowId for that note,
     * otherwise return a -1 to indicate failure.

     * @param chargeno the charge number for the entry
     * *
     * @param timeOut  the time in milliseconds of the clock-out
     * *
     * @return rowId or -1 if failed
     */
    @JvmOverloads fun closeEntry(chargeno: Long, timeOutP: Long = System.currentTimeMillis()): Long {
        var timeOut = timeOutP
        // final long origTimeOut = timeOut;
        // if (TimeSheetActivity.prefs.getAlignMinutesAuto()) {
        // timeOut = TimeHelpers.millisToAlignMinutes(timeOut,
        // TimeSheetActivity.prefs.getAlignMinutes());
        if (TimeSheetActivity.prefs.alignMinutesAuto) {
            timeOut = TimeHelpers.millisToAlignMinutes(timeOut,
                    TimeSheetActivity.prefs.alignMinutes)
            // TODO: Fix in a more sensible way.
            // Hack to account for a cross-day automatic clock out.
            // if (timeOut - origTimeOut == 1)
            // timeOut = origTimeOut;
        }

        // Stop the time closed from landing on a day boundary.
        val taskCursor = fetchEntry(lastClockEntry())
        taskCursor.moveToFirst()
        val timeIn = taskCursor.getLong(taskCursor
                .getColumnIndex(TimeSheetDbAdapter.KEY_TIMEIN))
        // TODO: Need a boolean preference to enable / disable this...
        // long boundary = TimeHelpers.millisToEoDBoundary(timeIn,
        // TimeSheetActivity.prefs.getTimeZone());
        val boundary = TimeHelpers.millisToEoDBoundary(timeIn,
                TimeSheetActivity.prefs.timeZone)
        Log.d(TAG, "Boundary: $boundary")
        if (timeOut > boundary)
            timeOut = boundary - 1000

        val updateValues = ContentValues()
        updateValues.put(KEY_TIMEOUT, timeOut)

        Log.d(TAG, "closeEntry: $chargeno at $timeOut (${TimeHelpers.millisToTimeDate(timeOut)})")
        if (mDb == null)
            open()
        return mDb!!.update(
                CLOCK_DATABASE_TABLE,
                updateValues,
                "$KEY_ROWID= ? and $KEY_CHARGENO = ?",
                arrayOf(java.lang.Long.toString(lastClockEntry()),
                        java.lang.Long.toString(chargeno))).toLong()
    }

    /**
     * Delete the entry with the given rowId

     * @param rowId code id of note to delete
     * *
     * @return true if deleted, false otherwise
     */
    fun deleteEntry(rowId: Long): Boolean {
        Log.i("Delete called", "value__ $rowId")
        if (mDb == null)
            open()
        return mDb!!.delete(CLOCK_DATABASE_TABLE, "$KEY_ROWID=$rowId", null) > 0
    }

    /**
     * Return a Cursor over the list of all entries in the database

     * @return Cursor over all database entries
     */
    fun fetchAllTimeEntries(): Cursor {
        if (mDb == null)
            open()
        return mDb!!.query(CLOCK_DATABASE_TABLE,
                arrayOf(KEY_ROWID, KEY_CHARGENO, KEY_TIMEIN, KEY_TIMEOUT),
                null, null, null, null, null)
    }

    /**
     * Return a Cursor positioned at the entry that matches the given rowId

     * @param rowId id of entry to retrieve
     * *
     * @return Cursor positioned to matching entry, if found
     * *
     * @throws SQLException if entry could not be found/retrieved
     */
    @Throws(SQLException::class)
    fun fetchEntry(rowId: Long): Cursor {
        if (mDb == null)
            open()
        val mCursor = mDb!!.query(true, CLOCK_DATABASE_TABLE,
                arrayOf(KEY_ROWID, KEY_CHARGENO, KEY_TIMEIN, KEY_TIMEOUT),
                "$KEY_ROWID=$rowId", null, null, null, null, null)
        mCursor?.moveToFirst()
        return mCursor
    }

    /**
     * Return a Cursor positioned at the entry that matches the given rowId

     * @param rowId id of entry to retrieve
     * *
     * @return Cursor positioned to matching entry, if found
     * *
     * @throws SQLException if entry could not be found/retrieved
     */
    @Throws(SQLException::class)
    fun fetchEntry(rowId: Long, column: String): Cursor {
        if (mDb == null)
            open()
        val mCursor = mDb!!.query(true, CLOCK_DATABASE_TABLE,
                arrayOf(column), "$KEY_ROWID=$rowId", null, null, null, null, null)
        mCursor?.moveToFirst()
        return mCursor
    }

    /**
     * Update the note using the details provided. The entry to be updated is
     * specified using the rowId, and it is altered to use the date and time
     * values passed in

     * @param rowId    id of entry to update
     * *
     * @param chargeno change number to update
     * *
     * @param date     the date of the entry
     * *
     * @param timein   the time work started on the task
     * *
     * @param timeout  the time work stopped on the task
     * *
     * @return true if the entry was successfully updated, false otherwise
     */
    fun updateEntry(rowIdP: Long, chargeno: Long, date: String?,
                    timein: Long, timeout: Long): Boolean {
        var rowId = rowIdP
        val args = ContentValues()
        // Only change items that aren't null or -1.
        if (timein != -1L)
            args.put(KEY_TIMEIN, timein)
        if (chargeno != -1L)
            args.put(KEY_CHARGENO, chargeno)
        if (timeout != -1L)
            args.put(KEY_TIMEOUT, timeout)

        if (rowId == -1L)
            rowId = lastClockEntry()

        if (mDb == null)
            open()
        return mDb!!.update(CLOCK_DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0
    }

    /**
     * Retrieve the taskID of the last entry in the clock table.

     * @return rowId or -1 if failed
     */
    fun taskIDForLastClockEntry(): Long {
        val lastClockID = lastClockEntry()

        if (mDb == null)
            open()
        val mCursor = mDb!!.query(true, CLOCK_DATABASE_TABLE,
                arrayOf(KEY_CHARGENO), "$KEY_ROWID = $lastClockID",
                null, null, null, null, null) ?: return -1

        mCursor.moveToFirst()

        if (mCursor.isAfterLast)
            return -1

        val response = mCursor.getLong(0)
        try {
            mCursor.close()
        } catch (e: IllegalStateException) {
            Log.d(TAG, "taskIDForLastClockEntry " + e.toString())
        }

        return response
    }

    /**
     * in *

     * @return time in in milliseconds or -1 if failed
     */
    fun timeInForLastClockEntry(): Long {
        val lastClockID = lastClockEntry()

        if (mDb == null)
            open()
        val mCursor = mDb!!.query(true, CLOCK_DATABASE_TABLE,
                arrayOf(KEY_TIMEIN), "$KEY_ROWID = lastClockID",
                null, null, null, null, null) ?: return -1

        mCursor.moveToFirst()

        if (mCursor.isAfterLast)
            return -1

        val response = mCursor.getLong(0)
        try {
            mCursor.close()
        } catch (e: IllegalStateException) {
            Log.d(TAG, "timeOutForLastClockEntry " + e.toString())
        }

        return response
    }

    /**
     * Retrieve the time out of the last entry in the clock table.

     * @return time out in milliseconds or -1 if failed
     */
    fun timeOutForLastClockEntry(): Long {
        val lastClockID = lastClockEntry()

        if (mDb == null)
            open()
        val mCursor = mDb!!.query(true, CLOCK_DATABASE_TABLE,
                arrayOf(KEY_TIMEOUT), "$KEY_ROWID = $lastClockID",
                null, null, null, null, null) ?: return -1

        mCursor.moveToFirst()

        if (mCursor.isAfterLast)
            return -1

        val response = mCursor.getLong(0)
        try {
            mCursor.close()
        } catch (e: IllegalStateException) {
            Log.d(TAG, "timeOutForLastClockEntry " + e.toString())
        }

        return response
    }

    /**
     * Retrieve the row of the last entry in the tasks table.

     * @return rowId or -1 if failed
     */
    fun lastTaskEntry(): Long {
        if (mDb == null)
            open()
        val mCursor = mDb!!.query(true, TASKS_DATABASE_TABLE,
                arrayOf(MAX_ROW), null, null, null, null, null, null)
        mCursor.moveToFirst()
        val response = mCursor.getLong(0)
        try {
            mCursor.close()
        } catch (e: IllegalStateException) {
            Log.d(TAG, "lastTaskEntry " + e.toString())
        }

        return response
    }

    /**
     * Return a Cursor positioned at the entry that matches the given rowId

     * @param rowId id of entry to retrieve
     * *
     * @return Cursor positioned to matching entry, if found
     * *
     * @throws SQLException if entry could not be found/retrieved
     */
    @Throws(SQLException::class)
    fun getTimeEntryTuple(rowId: Long): Cursor {
        if (mDb == null)
            open()
        val mCursor = mDb!!.query(true, ENTRYITEMS_VIEW,
                arrayOf(KEY_ROWID, KEY_TASK, KEY_TIMEIN, KEY_TIMEOUT),
                "$KEY_ROWID = $rowId", null, null, null, null, null)
        mCursor?.moveToFirst()
        return mCursor
    }

    /**
     * Retrieve the entry in the timesheet table immediately prior to the
     * supplied entry.

     * @return rowId or -1 if failed
     */
    fun getPreviousClocking(rowID: Long): Long {
        var thisTimeIn: Long = -1
        var prevTimeOut: Long = -1

        Log.d(TAG, "getPreviousClocking for row: " + rowID)

        // Get the tuple from the provided row
        var mCurrent: Cursor? = getTimeEntryTuple(rowID)

        // KEY_ROWID, KEY_TASK, KEY_TIMEIN, KEY_TIMEOUT
        if (mCurrent != null) {
            try {
                mCurrent.moveToFirst()
                val response = mCurrent.getString(mCurrent.getColumnIndex(KEY_TIMEIN))
                thisTimeIn = java.lang.Long.parseLong(response)
                Log.d(TAG, "timeIn for current: " + thisTimeIn)
            } catch (e: IllegalStateException) {
            }

            try {
                mCurrent.close()
            } catch (e: IllegalStateException) {
                Log.d(TAG, "getPreviousClocking " + e.toString())
            }

        }

        // Query to discover the immediately previous row ID.
        if (mDb == null)
            open()
        val mCursor = mDb!!.query(true, CLOCK_DATABASE_TABLE,
                arrayOf(KEY_ROWID), "$KEY_ROWID < '$rowID'",
                null, null, null, KEY_ROWID + " desc", "1") ?: return -1

        mCursor.moveToFirst()

        val response = mCursor.getString(0)
        var prevRowID = java.lang.Long.parseLong(response)
        try {
            mCursor.close()
        } catch (e: IllegalStateException) {
            Log.d(TAG, "getPreviousClocking " + e.toString())
        }

        Log.d(TAG, "rowID for previous: " + prevRowID)

        // Get the tuple from the just-retrieved row
        mCurrent = getTimeEntryTuple(prevRowID)
        // KEY_ROWID, KEY_TASK, KEY_TIMEIN, KEY_TIMEOUT
        try {
            mCurrent.moveToFirst()
            val response1 = mCurrent.getString(3)
            prevTimeOut = java.lang.Long.parseLong(response1)
            Log.d(TAG, "timeOut for previous: " + prevTimeOut)
        } catch (e: IllegalStateException) {
        } finally {
            mCurrent.close()
        }
        // If the two tasks don't flow from one to another, don't allow the
        // entry to be adjusted.
        if (thisTimeIn != prevTimeOut)
            prevRowID = -1

        return prevRowID
    }

    /**
     * Retrieve the entry in the timesheet table immediately following the
     * supplied entry.

     * @return rowId or -1 if failed
     */
    // TODO: Should this be chronological or ordered by _id? as it is now?
    // And, if it should be chronological by time in or time out or both... :(
    fun getNextClocking(rowID: Long): Long {
        var thisTimeOut: Long = -1
        var nextTimeIn: Long = -1

        Log.d(TAG, "getNextClocking for row: " + rowID)

        // Get the tuple from the provided row
        var mCurrent: Cursor? = getTimeEntryTuple(rowID)
        // KEY_ROWID, KEY_TASK, KEY_TIMEIN, KEY_TIMEOUT
        if (mCurrent != null) {
            try {
                mCurrent.moveToFirst()
                val response = mCurrent.getString(mCurrent.getColumnIndex(KEY_TIMEOUT))
                thisTimeOut = java.lang.Long.parseLong(response)
                Log.d(TAG, "timeOut for current: " + thisTimeOut)
            } catch (e: IllegalStateException) {
            }

            try {
                mCurrent.close()
            } catch (e: IllegalStateException) {
                Log.d(TAG, "getNextClocking " + e.toString())
            }

        }

        // Query to discover the immediately following row ID.
        val mCursor: Cursor?
        if (mDb == null)
            open()
        try {
            mCursor = mDb!!.query(true, CLOCK_DATABASE_TABLE,
                    arrayOf(KEY_ROWID), "$KEY_ROWID > '$rowID'",
                    null, null, null, KEY_ROWID, "1")
        } catch (e: RuntimeException) {
            Log.i(TAG, "Caught exception finding next clocking.")
            Log.i(TAG, e.toString())
            return -1
        }

        if (mCursor == null)
            return -1

        mCursor.moveToFirst()

        var nextRowID: Long
        try {
            val response = mCursor.getString(0)
            nextRowID = java.lang.Long.parseLong(response)
        } catch (e: CursorIndexOutOfBoundsException) {
            Log.i(TAG, "Caught exception retrieving row.")
            Log.i(TAG, e.toString())
            return -1
        } catch (e: RuntimeException) {
            Log.i(TAG, "Caught exception retrieving row.")
            Log.i(TAG, e.toString())
            return -1
        }

        try {
            mCursor.close()
        } catch (e: IllegalStateException) {
            Log.d(TAG, "getNextClocking " + e.toString())
        }

        Log.d(TAG, "rowID for next: " + nextRowID)

        // Get the tuple from the just-retrieved row
        mCurrent = getTimeEntryTuple(nextRowID)
        // KEY_ROWID, KEY_TASK, KEY_TIMEIN, KEY_TIMEOUT
        try {
            mCurrent.moveToFirst()
            val response1 = mCurrent.getString(mCurrent.getColumnIndex(KEY_TIMEIN))
            nextTimeIn = java.lang.Long.parseLong(response1)
            Log.d(TAG, "timeIn for next: " + nextTimeIn)
        } catch (e: IllegalStateException) {
        }

        try {
            mCurrent.close()
        } catch (e: IllegalStateException) {
            Log.d(TAG, "taskIDForLastClockEntry " + e.toString())
        }

        // If the two tasks don't flow from one to another, don't allow the
        // entry to be adjusted.
        if (thisTimeOut != nextTimeIn)
            nextRowID = -1

        return nextRowID
    }

    /**
     * Retrieve the row of the last entry in the clock table.

     * @return rowId or -1 if failed
     */
    fun lastClockEntry(): Long {
        if (mDb == null)
            open()
        val mCursor = mDb!!.query(true, CLOCK_DATABASE_TABLE,
                arrayOf(MAX_ROW), null, null, null, null, null, null)
        mCursor?.moveToFirst()
        val response = mCursor!!.getLong(0)
        try {
            mCursor.close()
        } catch (e: IllegalStateException) {
            Log.d(TAG, "lastClockEntry " + e.toString())
        }

        return response
    }

    /**
     * Retrieve an array of rows for today's entry in the clock table.

     * @return rowId or -1 if failed
     */
    fun todaysEntries(): LongArray? {
        val now = TimeHelpers.millisNow()
        val todayStart = TimeHelpers.millisToStartOfDay(now)
        val todayEnd = TimeHelpers.millisToEndOfDay(now)
        val rows: LongArray

        // public Cursor query(boolean distinct, String table, String[] columns,
        // String selection, String[] selectionArgs, String groupBy, String
        // having, String orderBy) {

        if (mDb == null)
            open()
        val mCursor = mDb!!.query(false, CLOCK_DATABASE_TABLE, arrayOf(KEY_ROWID),
                "$KEY_TIMEIN >=? and ($KEY_TIMEOUT <=? or $KEY_TIMEOUT = 0)",
                arrayOf(todayStart.toString(), todayEnd.toString()), null, null, null, null)
        mCursor?.moveToLast() ?: Log.e(TAG, "todaysEntried mCursor is null.")

        if (mCursor!!.isAfterLast) {
            Toast.makeText(mCtx, "No entries in the database for today.",
                    Toast.LENGTH_SHORT).show()
            return null
        }

        rows = LongArray(mCursor.count)

        mCursor.moveToFirst()
        while (!mCursor.isAfterLast) {
            rows[mCursor.position] = mCursor.getLong(0)
            mCursor.moveToNext()
        }
        try {
            mCursor.close()
        } catch (e: IllegalStateException) {
            Log.d(TAG, "todaysEntries " + e.toString())
        }

        return rows
    }

    /**
     * Retrieve list of entries for the day surrounding the supplied time.

     * @return rowId or -1 if failed
     */
    fun getEntryReportCursor(distinct: Boolean, columns: Array<String>,
                             start: Long, end: Long): Cursor? {
        return getEntryReportCursor(distinct, columns, null, null, start, end)
    }

    /**
     * Retrieve list of entries for the day surrounding the supplied time.

     * @return rowId or -1 if failed
     */
    protected fun getEntryReportCursor(distinct: Boolean, columns: Array<String>,
                                       groupBy: String?, orderBy: String?, start: Long, end: Long): Cursor? {
        // public Cursor query(boolean distinct, String table, String[] columns,
        // String selection, String[] selectionArgs, String groupBy, String
        // having, String orderBy, String limit) {

        val selection: String
        val endDay = TimeHelpers.millisToDayOfMonth(end - 1000)
        val now = TimeHelpers.millisNow()
        if (TimeHelpers.millisToDayOfMonth(now - 1000) == endDay || TimeHelpers.millisToDayOfMonth(TimeHelpers
                .millisToEndOfWeek(now - 1000, TimeSheetActivity.prefs.weekStartDay,
                        TimeSheetActivity.prefs.weekStartHour)) == endDay) {
            Log.d(TAG, "getEntryReportCursor: Allowing selection of zero-end-hour entries.")
            selection = "$KEY_TIMEIN >=? and $KEY_TIMEOUT <= ? and ($KEY_TIMEOUT >= $KEY_TIMEIN or $KEY_TIMEOUT = 0)"
        } else {
            selection = "$KEY_TIMEIN >=? and $KEY_TIMEOUT <= ? and $KEY_TIMEOUT >= $KEY_TIMEIN"
        }

        Log.d(TAG, "getEntryReportCursor: Selection criteria: $selection")
        Log.d(TAG, "getEntryReportCursor: Selection arguments: $start, $end")
        Log.d(TAG, "getEntryReportCursor: Selection arguments: $(TimeHelpers.millisToTimeDate(start)), $(TimeHelpers.millisToTimeDate(end))")

        val mCursor: Cursor?
        if (mDb == null)
            open()
        try {
            mCursor = mDb!!.query(distinct, ENTRYREPORT_VIEW, columns,
                    selection, arrayOf(start.toString(), end.toString()), groupBy, null,
                    orderBy, null)
        } catch (e: NullPointerException) {
            Log.d(TAG, "getEntryReportCursor: Cursor creation failed: " + e)
            return null
        }

        mCursor?.moveToLast() ?: Log.e(TAG, "getEntryReportCursor: mCursor for range is null.")

        if (mCursor!!.isAfterLast) {
            Log.d(TAG, "getEntryReportCursor: mCursor for range is empty.")
            // Toast.makeText(mCtx,
            // "No entries in the database for supplied range.",
            // Toast.LENGTH_SHORT).show();
            return null
        }

        return mCursor
    }

    /**
     * Retrieve list of entries for the day surrounding the supplied time.

     * @return rowId or -1 if failed
     */
    fun getSummaryCursor(distinct: Boolean, columns: Array<String>,
                         start: Long, end: Long): Cursor? {
        return getSummaryCursor(distinct, columns, null, null, start, end)
    }

    /*
     * Given a task ID, close the task and, if the ID differs, open a new task.
     * Refresh the data displayed.
     *
     * @return true if a new task was started, false if the old task was
     * stopped.
     */
    protected fun processChange(taskID: Long): Boolean {
        Log.d(TAG, "processChange for task ID: " + taskID)

        var lastRowID = lastClockEntry()
        val lastTaskID = taskIDForLastClockEntry()

        Log.d(TAG, "Last Task Entry Row: " + lastRowID)
        val c = fetchEntry(lastRowID, TimeSheetDbAdapter.KEY_TIMEOUT)

        var timeOut: Long = -1
        try {
            if (!c.moveToFirst())
                Log.d(TAG, "Moving cursor to first failed.")
            else {
                timeOut = c.getLong(0)
                Log.d(TAG, "Last clock out at: " + timeOut)
            }
        } catch (e: NullPointerException) {
            Log.d(TAG, "Using cursor: " + e.toString())
        } catch (e: Exception) {
            Log.d(TAG, "Using cursor: " + e.toString())
        }

        try {
            c.close()
        } catch (e: Exception) {
            Log.e(TAG, "Closing fetch entry cursor c: " + e.toString())
        }

        // Determine if the task has already been chosen and is now being
        // closed.
        if (timeOut == 0L && lastTaskID == taskID) {
            closeEntry(taskID)
            Log.d(TAG, "Closed task ID: " + taskID)
            return false
        } else {
            if (timeOut == 0L)
                closeEntry()
            createEntry(taskID)

            lastRowID = lastClockEntry()
            val tempClockCursor = fetchEntry(lastRowID)

            try {
                tempClockCursor.close()
            } catch (e: IllegalStateException) {
                Log.d(TAG, "processChange " + e.toString())
            }

            Log.d(TAG, "processChange ID from $lastTaskID to $taskID")
            return true
        }
    }

    /**
     * Retrieve list of entries for the day surrounding the supplied time.

     * @return rowId or -1 if failed
     */
    protected fun getSummaryCursor(distinct: Boolean, columns: Array<String>,
                                   groupBy: String?, orderBy: String?, start: Long, end: Long): Cursor? {
        // public Cursor query(boolean distinct, String table, String[] columns,
        // String selection, String[] selectionArgs, String groupBy, String
        // having, String orderBy, String limit) {

        // String selection;

        // Log.d(TAG, "getSummaryCursor: Selection criteria: " +
        // selection);
        try {
            Log.d(TAG, "getSummaryCursor: Columns: " + columns[0] + ", "
                    + columns[1] + " and " + columns[2])
        } catch (e: Exception) {
            Log.d(TAG, "getSummaryCursor has fewer than 3 columns.")
        }

        Log.d(TAG, "getSummaryCursor: Selection arguments: " + start
                + ", " + end)
        Log.d(TAG,
                "getSummaryCursor: Selection arguments: "
                        + TimeHelpers.millisToTimeDate(start) + ", "
                        + TimeHelpers.millisToTimeDate(end))
        // Cursor mCursor = mDb.query(distinct, SUMMARY_DATABASE_TABLE, columns,
        // selection, new String[] { String.valueOf(start).toString(),
        // String.valueOf(end).toString() }, groupBy, null,
        // orderBy, null);

        // TODO: Below KEY_STOTAL was KEY_TOTAL
        // Cursor mCursor = mDb.query(distinct, SUMMARY_DATABASE_TABLE,
        // columns,
        // KEY_STOTAL + " > 0",
        // new String[] { String.valueOf(0).toString(),
        // String.valueOf(end).toString() }, groupBy, null,
        // orderBy, null);

        var select = "SELECT "
        if (distinct)
            select = select + "DISTINCT "
        for (c in columns.indices) {
            if (c == 0) {
                select = select + columns[c]
            } else {
                select = select + (", " + columns[c])
            }
        }
        select = select + (" FROM " + SUMMARY_DATABASE_TABLE)
        select = "$select WHERE $KEY_TOTAL > 0"
        select = select + (" GROUP BY " + groupBy!!)
        select = select + (" ORDER BY " + orderBy!!)
        Log.d(TAG, "getSummaryCursor: query: " + select)
        if (mDb == null)
            open()
        val mCursor = mDb!!.rawQuery(select, null)

        mCursor?.moveToLast() ?: Log.e(TAG, "entryReport mCursor for range is null.")

        if (mCursor!!.isAfterLast) {
            Log.d(TAG, "entryReport mCursor for range is empty.")
            // Toast.makeText(mCtx,
            // "No entries in the database for supplied range.",
            // Toast.LENGTH_SHORT).show();
            return null
        }

        mCursor.moveToFirst()
        return mCursor
    }

    /**
     * Retrieve list of entries for the day surrounding the supplied time.

     * @return rowId or -1 if failed
     */
    @JvmOverloads fun dayEntryReport(timeP: Long = TimeHelpers.millisNow()): Cursor? {
        var time = timeP
        if (time <= 0) time = TimeHelpers.millisNow()

        val todayStart = TimeHelpers.millisToStartOfDay(time)
        val todayEnd = TimeHelpers.millisToEndOfDay(time)

        Log.d(TAG, "dayEntryReport start: " + TimeHelpers.millisToTimeDate(todayStart))
        Log.d(TAG, "dayEntryReport   end: " + TimeHelpers.millisToTimeDate(todayEnd))

        val columns = arrayOf(KEY_ROWID, KEY_TASK, KEY_RANGE, KEY_TIMEIN, KEY_TIMEOUT)
        return getEntryReportCursor(false, columns, null, KEY_TIMEIN,
                todayStart, todayEnd)
    }

    /**
     * Method that retrieves the entries for today from the entry view.

     * @param omitOpen Omit an open task in the result
     * *
     * @return Cursor over the results.
     */
    fun daySummary(omitOpen: Boolean): Cursor? {
        return daySummary(TimeHelpers.millisNow(), omitOpen)
    }

    /**
     * Method that populates a temporary table for a single specified day from
     * the entry view.

     * @param time     The time around which to report
     * *
     * @param omitOpen Include an open task in the result
     * *
     * @return Cursor over the results.
     */
    // TODO: Finish and replace the other routines with it.
    @JvmOverloads fun daySummary(timeP: Long = TimeHelpers.millisNow(), omitOpen: Boolean = true): Cursor? {
        var time = timeP
        Log.d(TAG, "In daySummary.")
        if (time <= 0)
            time = TimeHelpers.millisNow()

        var todayStart = TimeHelpers.millisToStartOfDay(time)
        var todayEnd = TimeHelpers.millisToEndOfDay(time)

        // TODO: This is a first attempt and is likely to be convoluted.
        // If today is the split day for a 9/80-style week, adjust the start/end times appropriately.
        if (TimeHelpers.millisToDayOfWeek(todayStart) == TimeSheetActivity.prefs.weekStartDay && TimeSheetActivity.prefs.weekStartHour > 0) {
            val splitMillis = (TimeSheetActivity.prefs.weekStartHour * 3600 * 1000).toLong()
            // This is the partial day where the week splits, only for schedules like a 9/80.
            if (time < todayStart + splitMillis)
            // Move the end of the day to the split time.
                todayEnd = todayStart + splitMillis
            else
                todayStart = todayStart + splitMillis // Move the start of the day to the split time.
        }

        Log.d(TAG, "daySummary start: " + TimeHelpers.millisToTimeDate(todayStart))
        Log.d(TAG, "daySummary   end: " + TimeHelpers.millisToTimeDate(todayEnd))

        populateSummary(todayStart, todayEnd, omitOpen)

        // String[] columns = { KEY_TASK, KEY_HOURS };
        // String groupBy = KEY_TASK;
        // String orderBy = KEY_TASK;
        val columns = arrayOf(KEY_ROWID, KEY_TASK, KEY_TOTAL)
        val groupBy = KEY_TASK
        val orderBy = KEY_TOTAL + " DESC"
        try {
            return getSummaryCursor(true, columns, groupBy, orderBy,
                    todayStart, todayEnd)
        } catch (e: SQLiteException) {
            Log.e(TAG, "getSummaryCursor: " + e.toString())
            return null
        }

    }

    /**
     * Retrieve list of entries for the week surrounding the supplied time.

     * @return Cursor over the entries
     */
    @JvmOverloads fun weekEntryReport(timeP: Long = TimeHelpers.millisNow()): Cursor? {
        var time = timeP
        if (time <= 0)
            time = TimeHelpers.millisNow()

        //	long todayStart = TimeHelpers.millisToStartOfWeek(time);
        //	long todayEnd = TimeHelpers.millisToEndOfWeek(time);
        val todayStart = TimeHelpers.millisToStartOfWeek(time,
                TimeSheetActivity.prefs.weekStartDay,
                TimeSheetActivity.prefs.weekStartHour)
        val todayEnd = TimeHelpers.millisToEndOfWeek(time,
                TimeSheetActivity.prefs.weekStartDay,
                TimeSheetActivity.prefs.weekStartHour)

        // public Cursor query(boolean distinct, String table, String[] columns,
        // String selection, String[] selectionArgs, String groupBy, String
        // having, String orderBy) {

        Log.d(TAG, "weekEntryReport start: " + TimeHelpers.millisToTimeDate(todayStart))
        Log.d(TAG, "weekEntryReport   end: " + TimeHelpers.millisToTimeDate(todayEnd))

        val columns = arrayOf(KEY_ROWID, KEY_TASK, KEY_RANGE, KEY_TIMEIN, KEY_TIMEOUT)
        return getEntryReportCursor(false, columns, todayStart, todayEnd)
    }

    /**
     * Method that populates a temporary table for a single specified day from
     * the entry view.

     * @return Cursor over the results.
     */
    // TODO: Finish and replace the other routines with it.
    fun weekSummary(timeP: Long, omitOpen: Boolean): Cursor? {
        var time = timeP
        Log.d(TAG, "In weekSummary.")
        if (time <= 0)
            time = TimeHelpers.millisNow()
        Log.d(TAG, "weekSummary time arg: " + TimeHelpers.millisToTimeDate(time))

        //	long weekStart = TimeHelpers.millisToStartOfWeek(time);
        //	long weekEnd = TimeHelpers.millisToEndOfWeek(time);
        val weekStart = TimeHelpers.millisToStartOfWeek(time,
                TimeSheetActivity.prefs.weekStartDay,
                TimeSheetActivity.prefs.weekStartHour)
        val weekEnd = TimeHelpers.millisToEndOfWeek(weekStart + 86400000,
                TimeSheetActivity.prefs.weekStartDay,
                TimeSheetActivity.prefs.weekStartHour)

        Log.d(TAG, "weekSummary start: " + TimeHelpers.millisToTimeDate(weekStart))
        Log.d(TAG, "weekSummary   end: " + TimeHelpers.millisToTimeDate(weekEnd))

        populateSummary(weekStart, weekEnd, omitOpen)

        // String[] columns = { KEY_TASK, KEY_HOURS };
        // String groupBy = KEY_TASK;
        // String orderBy = KEY_TASK;
        val columns = arrayOf(KEY_ROWID, KEY_TASK, KEY_TOTAL)
        val groupBy = KEY_TASK
        val orderBy = KEY_TOTAL + " DESC"
        try {
            return getSummaryCursor(true, columns, groupBy, orderBy, weekStart,
                    weekEnd)
        } catch (e: SQLiteException) {
            Log.e(TAG, "getSummaryCursor: " + e.localizedMessage)
            return null
        }

    }

    /**
     * @param summaryStart The start time for the summary
     * *
     * @param summaryEnd   The end time for the summary
     */
    internal fun populateSummary(summaryStart: Long, summaryEnd: Long) {
        populateSummary(summaryStart, summaryEnd, true)
    }

    /**
     * @param summaryStart The start time for the summary
     * *
     * @param summaryEnd   The end time for the summary
     * *
     * @param omitOpen     Whether the summary should omit an open task
     */
    private fun populateSummary(summaryStart: Long, summaryEnd: Long,
                                omitOpen: Boolean) {
        if (mDb == null)
            open()
        try {
            Log.v(TAG, "populateSummary: Creating summary table.")
            mDb!!.execSQL(SUMMARY_TABLE_CREATE)
        } catch (e: SQLException) {
            // This shouldn't occur, but hope for the best.
            Log.i(TAG, "populateSummary: SUMMARY_TABLE_CREATE: " + e.toString())
        }

        Log.v(TAG, "populateSummary: Cleaning summary table.")
        mDb!!.execSQL(SUMMARY_TABLE_CLEAN)
        mDb!!.execSQL(VACUUM)

        var omitOpenQuery = ""
        if (omitOpen) {
            omitOpenQuery = "$CLOCK_DATABASE_TABLE.$KEY_TIMEOUT > 0 AND "
        }

        val populateTemp1 = """INSERT INTO $SUMMARY_DATABASE_TABLE
( $KEY_TASK,$KEY_TOTAL ) SELECT
$TASKS_DATABASE_TABLE.$KEY_TASK,
SUM((CASE WHEN $CLOCK_DATABASE_TABLE.$KEY_TIMEOUT = 0 THEN ${TimeHelpers.millisNow()} ELSE
$CLOCK_DATABASE_TABLE.$KEY_TIMEOUT END -
$CLOCK_DATABASE_TABLE.$KEY_TIMEIN )/3600000.0) AS
$KEY_TOTAL FROM $CLOCK_DATABASE_TABLE,$TASKS_DATABASE_TABLE
WHERE $CLOCK_DATABASE_TABLE.$KEY_TIMEOUT <= $summaryEnd AND $omitOpenQuery
$CLOCK_DATABASE_TABLE.$KEY_TIMEIN >=
$summaryStart AND $CLOCK_DATABASE_TABLE.$KEY_CHARGENO = $TASKS_DATABASE_TABLE.$KEY_ROWID
AND $TASKS_DATABASE_TABLE.$KEY_SPLIT=0 GROUP BY $KEY_TASK"""
        Log.v(TAG, "populateTemp1\n" + populateTemp1)
        mDb!!.execSQL(populateTemp1)

        val populateTemp2 = """INSERT INTO $SUMMARY_DATABASE_TABLE
($KEY_TASK,$KEY_TOTAL) SELECT
$TASKSPLITREPORT_VIEW.taskdesc, (sum((CASE WHEN
$CLOCK_DATABASE_TABLE.$KEY_TIMEOUT = 0 THEN
${TimeHelpers.millisNow()} ELSE $CLOCK_DATABASE_TABLE.$KEY_TIMEOUT END -
$CLOCK_DATABASE_TABLE.$KEY_TIMEIN )/3600000.0) *
($TASKSPLIT_DATABASE_TABLE.$KEY_PERCENTAGE /100.0)) AS $KEY_TOTAL
FROM
$CLOCK_DATABASE_TABLE,$TASKSPLIT_DATABASE_TABLE,
$TASKS_DATABASE_TABLE,$TASKSPLITREPORT_VIEW
WHERE $CLOCK_DATABASE_TABLE.$KEY_TIMEOUT <=
$summaryEnd AND $omitOpenQuery $CLOCK_DATABASE_TABLE.$KEY_TIMEIN >= $summaryStart AND
$CLOCK_DATABASE_TABLE.$KEY_CHARGENO =
$TASKS_DATABASE_TABLE.$KEY_ROWID AND
$TASKS_DATABASE_TABLE.$KEY_ROWID =
$TASKSPLIT_DATABASE_TABLE.$KEY_CHARGENO AND
$TASKS_DATABASE_TABLE.$KEY_ROWID =
$TASKSPLITREPORT_VIEW.$KEY_PARENTTASK AND
$TASKSPLIT_DATABASE_TABLE.$KEY_TASK ||
$TASKSPLIT_DATABASE_TABLE.$KEY_PERCENTAGE = $TASKSPLITREPORT_VIEW.$KEY_ROWID ||
$TASKSPLITREPORT_VIEW.$KEY_PERCENTAGE
GROUP BY $TASKSPLIT_DATABASE_TABLE.$KEY_TASK"""
        Log.v(TAG, "populateTemp2\n$populateTemp2")
        mDb!!.execSQL(populateTemp2)
    }

    /**
     * Create a new time entry using the charge number provided. If the entry is
     * successfully created return the new rowId for that number, otherwise
     * return a -1 to indicate failure.

     * @param task the charge number text for the entry
     * *
     * @return rowId or -1 if failed
     */
    fun createTask(task: String): Long {
        Log.d(TAG, "createTask: " + task)
        val tempDate = System.currentTimeMillis() // Local time...
        val initialValues = ContentValues()
        initialValues.put(KEY_TASK, task)
        initialValues.put(KEY_LASTUSED, tempDate)

        return mDb!!.insert(TASKS_DATABASE_TABLE, null, initialValues)
    }

    /**
     * Create a new split time entry using the information provided. If the
     * entry is successfully created return the new rowId for that number,
     * otherwise return a -1 to indicate failure.

     * @param task       the charge number text for the entry
     * *
     * @param parent     The parent for the split task
     * *
     * @param percentage The amount this task contributes to the task.
     * *
     * @return rowId or -1 if failed
     */
    fun createTask(task: String, parent: String, percentage: Int): Long {
        Log.d(TAG, "createTask: " + task)
        Log.d(TAG, "    parent: " + parent)
        Log.d(TAG, "percentage: " + percentage)
        val tempDate = System.currentTimeMillis() // Local time...
        val parentId = getTaskIDByName(parent)

        var initialValues = ContentValues()
        initialValues.put(KEY_TASK, task)
        initialValues.put(KEY_LASTUSED, tempDate)
        initialValues.put(KEY_SPLIT, 1)
        val newRow = mDb!!.insert(TASKS_DATABASE_TABLE, null, initialValues)

        initialValues = ContentValues()
        initialValues.put(KEY_SPLIT, 2)
        try {
            mDb!!.update(TASKS_DATABASE_TABLE, initialValues, KEY_ROWID + "=?",
                    arrayOf(parentId.toString()))
        } catch (e: SQLiteConstraintException) {
            Log.d(TAG, "createTask: " + e.toString())
        }

        Log.d(TAG, "new row   : " + newRow)
        initialValues = ContentValues()
        initialValues.put(KEY_TASK, newRow)
        initialValues.put(KEY_CHARGENO, parentId)
        initialValues.put(KEY_PERCENTAGE, percentage)

        return mDb!!.insert(TASKSPLIT_DATABASE_TABLE, null, initialValues)
    }

    /**
     * Return a Cursor over the list of all tasks in the database eligible to be
     * split task parents.

     * @return Cursor over all database entries
     */
    fun fetchParentTasks(): Cursor {
        Log.d(TAG, "fetchParentTasks: Issuing DB query.")
        return mDb!!.query(TASKS_DATABASE_TABLE,
                arrayOf(KEY_ROWID, KEY_TASK, KEY_ACTIVE, KEY_SPLIT),
                "$KEY_ACTIVE = '$DB_TRUE' and $KEY_SPLIT !=1", null, null, null, KEY_TASK)
    }

    /**
     * Return a Cursor over the list of all tasks in the database that are children
     * of the supplied split task parent.
     * @return Cursor over all database entries
     */
    /*
    CREATE TABLE TaskSplit (
        _id INTEGER PRIMARY KEY AUTOINCREMENT,
        chargeno INTEGER NOT NULL REFERENCES Tasks(_id),
        task INTEGER NOT NULL REFERENCES Tasks(_id),
        percentage INTEGER NOT NULL DEFAULT 100 CHECK(percentage>=0 AND percentage<=100)
     );
     */
    fun fetchChildTaskCursor(parentID: Long): Cursor? {
        Log.d(TAG, "fetchChildTasks($parentID): Issuing DB query.")
        return mDb!!.query(TASKSPLIT_DATABASE_TABLE,
                arrayOf(KEY_ROWID, KEY_CHARGENO, KEY_TASK),
                "$KEY_CHARGENO = '$parentID'", null, null, null, KEY_TASK)
    }

    /**
     * Return an array over the list of all tasks in the database that are children
     * of the supplied split task parent.
     * @return Array over all matching database entries
     */
    fun fetchChildTasks(parentID: Long): Array<Long> {
        val entryids = MutableList<Long>(size = 5) { -1 }
        val mCursor = fetchChildTaskCursor(parentID)
        mCursor!!.moveToFirst()
        while (!mCursor.isAfterLast) {
            // Index #2 is the task identifier, which is what's needed here.
            val value = mCursor.getLong(mCursor.getColumnIndex(KEY_TASK))
            if (value > 0) entryids.add(value)
            mCursor.moveToNext()
        }
        while (entryids.contains(-1))
            entryids.remove(-1)
        Log.i(TAG, "# child tasks: ${entryids.size}")
        mCursor.close()
        return entryids.toTypedArray()
    }

    /**
     * Return a Cursor over the list of all entries in the database

     * @return Cursor over all database entries
     */
    fun fetchAllTaskEntries(): Cursor {
        Log.d(TAG, "fetchAllTaskEntries: Issuing DB query.")
        return mDb!!.query(TASKS_DATABASE_TABLE,
                arrayOf(KEY_ROWID, KEY_TASK, KEY_ACTIVE, KEY_USAGE, KEY_OLDUSAGE, KEY_LASTUSED),
                "active='$DB_TRUE'", null, null, null,
                "usage + (oldusage / 2) DESC")
    }

    /**
     * Return a Cursor over the list of all entries in the database

     * @return Cursor over all database entries
     */
    fun fetchAllDisabledTasks(): Cursor {
        Log.d(TAG, "fetchAllDisabledTasks: Issuing DB query.")
        return mDb!!.query(TASKS_DATABASE_TABLE,
                arrayOf(KEY_ROWID, KEY_TASK, KEY_ACTIVE, KEY_USAGE, KEY_OLDUSAGE, KEY_LASTUSED),
                "active='$DB_FALSE'", null, null, null, KEY_TASK)
    }

    /**
     * Return a Cursor positioned at the entry that matches the given rowId

     * @param rowId id of entry to retrieve
     * *
     * @return Cursor positioned to matching entry, if found
     * *
     * @throws SQLException if entry could not be found/retrieved
     */
    @Throws(SQLException::class)
    fun fetchTask(rowId: Long): Cursor {
        Log.d(TAG, "fetchTask: Issuing DB query.")
        val mCursor = mDb!!.query(true, TASKS_DATABASE_TABLE,
                arrayOf(KEY_ROWID, KEY_TASK, KEY_ACTIVE, KEY_USAGE, KEY_OLDUSAGE, KEY_LASTUSED),
                "$KEY_ROWID = $rowId", null, null, null, null, null)
        mCursor?.moveToFirst()
        return mCursor
    }

    /**
     * Retrieve the task ID of the supplied task name.

     * @return rowId or -1 if failed
     */
    fun getTaskIDByName(name: String): Long {
        Log.d(TAG, "getTaskIDByName: Issuing DB query.")
        if (mDb == null)
            open()
        val mCursor = mDb!!.query(true, TASKS_DATABASE_TABLE,
                arrayOf(KEY_ROWID), "$KEY_TASK = '$name'", null, null, null, null, null)
        mCursor?.moveToFirst() ?: return -1
        val response = mCursor.getLong(0)
        try {
            mCursor.close()
        } catch (e: IllegalStateException) {
            Log.d(TAG, "getTaskIDByName " + e.toString())
        }

        Log.d(TAG, "getTaskIDByName: " + response)
        return response
    }

    /**
     * Retrieve the task name for the supplied task ID.

     * @param taskID The identifier of the desired task
     * *
     * @return Name of the task identified by the taskID
     */
    fun getTaskNameByID(taskID: Long): String? {
        Log.d(TAG, "getTaskNameByID: Issuing DB query for ID: " + taskID)
        if (mDb == null)
            open()
        val mCursor = mDb!!.query(true, TASKS_DATABASE_TABLE,
                arrayOf(KEY_TASK), "$KEY_ROWID = '$taskID'", null, null, null, null, null)
        mCursor?.moveToFirst() ?: return null

        var response = "" // = new String("");
        if (mCursor.count < 1) {
            Log.d(TAG, "getCount result was < 1")
        } else {
            try {
                response = mCursor.getString(0)
                mCursor.close()
            } catch (e: SQLException) {
                Log.i(TAG, "getTaskNameByID: " + e.toString())
            } catch (e: IllegalStateException) {
                Log.i(TAG, "getTaskNameByID: " + e.toString())
            } catch (e: CursorIndexOutOfBoundsException) {
                Log.i(TAG, "getTaskNameByID: " + e.toString())
            }

            Log.d(TAG, "getTaskNameByID: " + response)
        }
        return response
    }

    /**
     * Return the entry that matches the given rowId

     * @param splitTask id of task to retrieve
     * *
     * @return parent's task ID, if found, 0 if not
     */
    fun getSplitTaskParent(splitTask: String): Long {
        Log.d(TAG, "getSplitTaskParent: " + splitTask)
        return getSplitTaskParent(getTaskIDByName(splitTask))
    }

    /**
     * Return the entry that matches the given rowId

     * @param rowId id of task to retrieve
     * *
     * @return parent's task ID, if found, 0 if not
     */
    fun getSplitTaskParent(rowId: Long): Long {
        Log.d(TAG, "getSplitTaskParent: Issuing DB query.")
        var ret: Long
        var mCursor: Cursor? = null
        val query = "SELECT $KEY_CHARGENO FROM $TASKSPLIT_DATABASE_TABLE WHERE $KEY_TASK = ?"
        Log.d(TAG, "getSplitTaskParent: query: $query, $rowId")
        if (mDb == null)
            open()
        try {
            // mCursor = mDb.query(true, TASKSPLIT_DATABASE_TABLE,
            // new String[] { KEY_CHARGENO }, KEY_TASK + "=" + rowId,
            // null, null, null, null, null);
            mCursor = mDb!!.rawQuery(query, arrayOf(rowId.toString()))
        } catch (e: SQLException) {
            Log.i(TAG, "getSplitTaskParent: " + e.toString())
        }

        try {
            if (mCursor != null) {
                mCursor.moveToFirst()
            }
            ret = mCursor!!.getLong(0)
            mCursor.close()
        } catch (e: CursorIndexOutOfBoundsException) {
            Log.i(TAG, "getSplitTaskParent: " + e.toString())
            ret = 0
        } catch (e: IllegalStateException) {
            Log.i(TAG, "getSplitTaskParent: " + e.toString())
            ret = 0
        }

        Log.d(TAG, "getSplitTaskParent: " + ret + " / " + getTaskNameByID(ret))
        return ret
    }

    /**
     * Return the entry that matches the given rowId

     * @param splitTask id of task to retrieve
     * *
     * @return parent's task ID, if found, 0 if not
     */
    fun getSplitTaskPercentage(splitTask: String): Int {
        Log.d(TAG, "getSplitTaskPercentage: " + splitTask)
        return getSplitTaskPercentage(getTaskIDByName(splitTask))
    }

    /**
     * Return the entry that matches the given rowId

     * @param rowId id of task to retrieve
     * *
     * @return parent's task ID, if found, 0 if not
     */
    fun getSplitTaskPercentage(rowId: Long): Int {
        Log.d(TAG, "getSplitTaskPercentage: Issuing DB query.")
        var ret: Int
        if (mDb == null)
            open()
        try {
            val mCursor = mDb!!.query(true, TASKSPLIT_DATABASE_TABLE,
                    arrayOf(KEY_PERCENTAGE), "$KEY_TASK = $rowId",
                    null, null, null, null, null)
            mCursor?.moveToFirst()
            ret = mCursor!!.getInt(0)
            mCursor.close()
        } catch (e: SQLException) {
            Log.i(TAG, "getSplitTaskPercentage: " + e.toString())
            ret = 0
        } catch (e: IllegalStateException) {
            Log.i(TAG, "getSplitTaskPercentage: " + e.toString())
            ret = 0
        }

        Log.d(TAG, "getSplitTaskPercentage: " + ret)
        return ret
    }

    /**
     * Return the flag whether the task that matches the given rowId is a split
     * task.

     * @param splitTask name of task to retrieve
     * *
     * @return parent's task ID, if found, 0 if not
     */
    fun getSplitTaskFlag(splitTask: String): Int {
        Log.d(TAG, "getSplitTaskFlag: " + splitTask)
        return getSplitTaskFlag(getTaskIDByName(splitTask))
    }

    /**
     * Return the flag whether the task that matches the given rowId is a split
     * task.

     * @param rowId id of task to retrieve
     * *
     * @return parent's task ID, if found, 0 if not
     */
    fun getSplitTaskFlag(rowId: Long): Int {
        Log.d(TAG, "getSplitTaskFlag: Issuing DB query.")
        var ret: Int
        if (mDb == null)
            open()
        try {
            val mCursor = mDb!!.query(true, TASKS_DATABASE_TABLE,
                    arrayOf(KEY_SPLIT), "$KEY_ROWID = $rowId", null, null, null, null, null)
            mCursor?.moveToFirst()
            ret = mCursor!!.getInt(0)
            Log.i(TAG, "getSplitTaskFlag: " + mCursor.getInt(0))
            mCursor.close()
        } catch (e: SQLException) {
            Log.i(TAG, "getSplitTaskPercentage: " + e.toString())
            ret = 0
        } catch (e: IllegalStateException) {
            Log.i(TAG, "getSplitTaskPercentage: " + e.toString())
            ret = 0
        }

        Log.d(TAG, "getSplitTaskFlag: " + ret)
        return ret
    }

    /**
     * Return the number of children whose parent matches the given rowId

     * @param rowId id of task to retrieve
     * *
     * @return Number of "children" of this task, 0 if none.
     */
    fun getQuantityOfSplits(rowId: Long): Long {
        Log.d(TAG, "getQuantityOfSplits: Issuing DB query.")
        var ret: Long
        if (mDb == null)
            open()
        try {
            val mCursor = mDb!!.query(true, TASKSPLIT_DATABASE_TABLE,
                    arrayOf("count($KEY_TASK)"), KEY_CHARGENO
                    + "=" + rowId, null, null, null, null, null)
            mCursor?.moveToFirst()
            ret = mCursor!!.getLong(0)
            mCursor.close()
        } catch (e: SQLException) {
            Log.i(TAG, "getQuantityOfSplits: " + e.toString())
            ret = 0
        } catch (e: IllegalStateException) {
            Log.i(TAG, "getQuantityOfSplits: " + e.toString())
            ret = 0
        }

        Log.d(TAG, "getQuantityOfSplits: " + ret)
        return ret
    }

    /**
     * Rename specified task

     * @param origName Old task name
     * *
     * @param newName  New task name
     */
    fun renameTask(origName: String, newName: String) {
        Log.d(TAG, "renameTask: Issuing DB query.")
        val taskID = getTaskIDByName(origName)
        val newData = ContentValues(1)
        newData.put(KEY_TASK, newName)
        if (mDb == null)
            open()
        try {
            // update(String table, ContentValues values, String whereClause,
            // String[] whereArgs)
            mDb!!.update(TASKS_DATABASE_TABLE, newData, "$KEY_ROWID=?",
                    arrayOf(taskID.toString()))
        } catch (e: RuntimeException) {
            Log.e(TAG, e.localizedMessage)
        }

    }

    /**
     * Alter specified split task

     * @param rowID      Task ID to change
     * *
     * @param parentID   New parent ID
     * *
     * @param percentage New percentage
     * *
     * @param split      New split flag state
     */
    fun alterSplitTask(rowID: Long, parentIDP: Long, percentage: Int,
                       split: Int) {
        var parentID = parentIDP
        Log.d(TAG, "alterSplitTask: Issuing DB query.")
        val currentParent = getSplitTaskParent(rowID)
        val currentSplit = getSplitTaskFlag(rowID)

        if (split == 0 && currentSplit == 1)
            parentID = -1

        if (mDb == null)
            open()
        // If the number of sub-splits under the parent task is 1 (<2) and we
        // are changing the parent, set the split flag to 0.
        if (getQuantityOfSplits(currentParent) < 2 && currentParent != parentID) {
            val initialValues = ContentValues(1)
            initialValues.put(KEY_SPLIT, 0)
            val i = mDb!!.update(TASKS_DATABASE_TABLE, initialValues,
                    "$KEY_ROWID=?", arrayOf(currentParent.toString()))
            Log.d(TAG, "Reverting task $currentParent to standard task returned $i")
        }

        // Set the flag on the new parent
        if (currentParent != parentID && parentID > 0) {
            val initialValues = ContentValues(1)
            initialValues.put(KEY_SPLIT, 2)
            val i = mDb!!.update(TASKS_DATABASE_TABLE, initialValues,
                    "$KEY_ROWID=?", arrayOf(parentID.toString()))
            Log.d(TAG, "Converting task $parentID to parent task returned: $i")
        }

        // If the new split state is 1, a child, set the appropriate values
        if (split == 1) {
            Log.d(TAG, "alterSplitTask: Setting up child")
            var newData = ContentValues(3)
            newData.put(KEY_CHARGENO, parentID)
            newData.put(KEY_PERCENTAGE, percentage)
            try {
                // update(String table, ContentValues values, String
                // whereClause,
                // String[] whereArgs)
                val i = mDb!!.update(TASKSPLIT_DATABASE_TABLE, newData,
                        "$KEY_TASK=?", arrayOf(rowID.toString()))
                Log.d(TAG, "Setting child task $rowID details returned: $i")
                if (i == 0) {
                    newData.put(KEY_TASK, rowID)
                    val j = mDb!!.insert(TASKSPLIT_DATABASE_TABLE, null, newData)
                    Log.d(TAG, "Inserting child task $rowID details returned: $j")
                }
            } catch (e: RuntimeException) {
                Log.e(TAG, e.localizedMessage)
            }

            newData = ContentValues(1)
            newData.put(KEY_SPLIT, 1)
            try {
                val i = mDb!!.update(TASKS_DATABASE_TABLE, newData,
                        "$KEY_ROWID=?", arrayOf(rowID.toString()))
                Log.d(TAG, "Converting task $rowID to child task returned: $i")
            } catch (e: RuntimeException) {
                Log.e(TAG, e.localizedMessage)
            }
        }

        // Delete the record in tasksplit if the new split state is 0
        if (currentSplit == 1 && split == 0) {
            Log.d(TAG, "alterSplitTask: Tearing down child")
            try {
                // update(String table, ContentValues values, String
                // whereClause,
                // String[] whereArgs)
                val i = mDb!!.delete(TASKSPLIT_DATABASE_TABLE, "$KEY_TASK=?",
                        arrayOf(rowID.toString()))
                Log.d(TAG, "Setting child task " + rowID
                        + " details returned: " + i)
            } catch (e: RuntimeException) {
                Log.e(TAG, e.localizedMessage)
            }

            val newData = ContentValues(1)
            newData.put(KEY_SPLIT, 0)
            try {
                val i = mDb!!.update(TASKS_DATABASE_TABLE, newData,
                        "$KEY_ROWID=?", arrayOf(rowID.toString()))
                Log.d(TAG, "Converting child task $rowID to standard task returned: $i")
            } catch (e: RuntimeException) {
                Log.e(TAG, e.localizedMessage)
            }
        }
    }

    /**
     * Deactivate / retire the task supplied.

     * @param taskName The name of the task to be deactivated.
     */
    fun deactivateTask(taskName: String) {
        Log.d(TAG, "deactivateTask: Issuing DB query.")
        val taskID = getTaskIDByName(taskName)
        deactivateTask(taskID)
    }

    /**
     * Deactivate / retire the task supplied.

     * @param taskID The ID of the task to be deactivated.
     */
    fun deactivateTask(taskID: Long) {
        Log.d(TAG, "deactivateTask: Issuing DB query.")
        val newData = ContentValues(1)
        newData.put(KEY_ACTIVE, DB_FALSE)
        if (mDb == null)
            open()
        try {
            mDb!!.update(TASKS_DATABASE_TABLE, newData,
                    "$KEY_ROWID=?", arrayOf(taskID.toString()))
        } catch (e: RuntimeException) {
            Log.e(TAG, e.localizedMessage)
        }
    }

    /**
     * Activate the task supplied.

     * @param taskName The name of the task to be activated.
     */
    fun activateTask(taskName: String) {
        Log.d(TAG, "activateTask: Issuing DB query.")
        val taskID = getTaskIDByName(taskName)
        activateTask(taskID)
    }

    /**
     * Activate the task supplied.

     * @param taskID The ID of the task to be activated.
     */
    fun activateTask(taskID: Long) {
        Log.d(TAG, "activateTask: Issuing DB query.")
        val newData = ContentValues(1)
        newData.put(KEY_ACTIVE, DB_TRUE)
        if (mDb == null)
            open()
        try {
            mDb!!.update(TASKS_DATABASE_TABLE, newData,
                    "$KEY_ROWID=?", arrayOf(taskID.toString()))
        } catch (e: RuntimeException) {
            Log.e(TAG, e.localizedMessage)
        }

    }

    /**
     * Increment the usage counter of the supplied task.

     * @param taskID The ID of the task's usage to be incremented.
     */
    private fun incrementTaskUsage(taskID: Long) {
        Log.d(TAG, "incrementTaskUsage: Issuing DB query.")
        if (mDb == null)
            open()
        val mCursor = mDb!!.query(true, TASKS_DATABASE_TABLE,
                arrayOf(KEY_USAGE, KEY_OLDUSAGE, KEY_LASTUSED),
                "$KEY_ROWID = $taskID", null, null, null, null, null)
        mCursor?.moveToFirst()
        var usage = mCursor!!.getLong(mCursor.getColumnIndex(KEY_USAGE))
        // long oldUsage = mCursor.getLong(mCursor.getColumnIndex(KEY_OLDUSAGE));
        val lastUsed = mCursor.getLong(mCursor.getColumnIndex(KEY_OLDUSAGE))
        val updateValues = ContentValues()

        val now = System.currentTimeMillis()
        val todayCal = GregorianCalendar.getInstance()
        val dateLastUsedCal = GregorianCalendar.getInstance()
        todayCal.timeInMillis = now
        dateLastUsedCal.timeInMillis = lastUsed

        // Roll-over the old usage when transitioning into a new month.
        if (todayCal.get(Calendar.MONTH) != dateLastUsedCal.get(Calendar.MONTH)) {
            val oldUsage = usage
            usage = 0
            updateValues.put(KEY_OLDUSAGE, oldUsage)
        }

        updateValues.put(KEY_LASTUSED, now)
        updateValues.put(KEY_USAGE, usage + 1)
        mDb!!.update(TASKS_DATABASE_TABLE, updateValues, "$KEY_ROWID = $taskID", null)

        try {
            mCursor.close()
        } catch (e: IllegalStateException) {
            Log.d(TAG, "incrementTaskUsage " + e.toString())
        }
    }

    // TODO: Please tell me there's a better way of doing this.
    // This is just stupid...
    protected val tasksList: Array<String?>
        get() {
            Log.d(TAG, "getTasksList")
            val taskCursor = fetchAllTaskEntries()
            val items = arrayOfNulls<String>(taskCursor.count)
            taskCursor.moveToFirst()
            var i = 0
            while (!taskCursor.isAfterLast) {
                items[i] = taskCursor.getString(1)
                taskCursor.moveToNext()
                i++
            }
            try {
                taskCursor.close()
            } catch (e: Exception) {
                Log.i(TAG, "taskCursor close getTasksList: " + e.toString())
            }

            return items
        }

    // TODO: Please tell me there's a better way of doing this.
    // This is just stupid...
    protected val dayReportList: Array<String?>
        get() {
            Log.d(TAG, "getTasksList")
            val reportCursor = daySummary()
            try {
                val items = arrayOfNulls<String?>(reportCursor!!.count)
                reportCursor.moveToFirst()
                var i = 0
                while (reportCursor.isAfterLast) {
                    items[i] = reportCursor.getString(reportCursor.getColumnIndex(KEY_TASK))
                    reportCursor.moveToNext()
                    i++
                }
                try {
                    reportCursor.close()
                } catch (e: Exception) {
                    Log.i(TAG, "reportCursor close getDayReportList: $e")
                }

                return items
            } catch (e: NullPointerException) {
                Log.i(TAG, "getDayReportList: $e")
            }

            return arrayOfNulls(1)
        }

    /**
     * Return a Cursor positioned at the note that matches the given rowId

     * @return Cursor positioned to matching note, if found
     * *
     * @throws SQLException if note could not be found/retrieved
     */
    @Throws(SQLException::class)
    internal fun fetchVersion(): Int {
        Log.d(TAG, "fetchVersion: Issuing DB query.")

        if (mDb == null)
            open()
        val mCursor = mDb!!.query(true, DATABASE_METADATA, arrayOf(MAX_COUNT),
                null, null, null, null, null, null)
        mCursor?.moveToFirst()
        val response = mCursor!!.getInt(0)
        try {
            mCursor.close()
        } catch (e: IllegalStateException) {
            Log.d(TAG, "fetchVersion $e")
        }

        return response
    }

    /**
     * Generic SQL exec wrapper, for use with statements which do not return
     * values.
     */
    fun runSQL(sqlTorun: String) {
        if (mDb == null)
            open()
        mDb!!.execSQL(sqlTorun)
    }

    /**
     * Generic SQL update wrapper, Exposes the update method for testing.

     * @param table       the table being updated
     * *
     * @param values      the ContentValues being updated
     * *
     * @param whereClause clause to limit updates
     * *
     * @param whereArgs   arguments to fill any ? in the whereClause.
     * *
     * @return The number of rows affected
     */
    fun runUpdate(table: String, values: ContentValues,
                  whereClause: String, whereArgs: Array<String>): Int {
        Log.d(TAG, "Running update on '$table'...")
        if (mDb == null)
            open()
        return mDb!!.update(table, values, whereClause, whereArgs)
    }

    /**
     * Generic SQL insert wrapper, Exposes the insert method for testing.

     * @param table       the table being updated
     * *
     * @param nullColHack Null column hack.
     * *
     * @param values      the ContentValues being updated
     * *
     * @return The rowID is the just-inserted row
     */
    fun runInsert(table: String, nullColHack: String, values: ContentValues): Long {
        Log.d(TAG, "Running update on '$table'...")
        if (mDb == null)
            open()
        return mDb!!.insert(table, nullColHack, values)
    }

    /**
     * Dumps the contents of the tasks table to logcat, for testing.
     */
    fun dumpTasks() {
        Log.d(TAG, "Dumping tasks table")
        val myQuery = "select * from $TASKS_DATABASE_TABLE" // +
        // " order by KEY_ROWID";
        if (mDb == null)
            open()
        val tasksC = mDb!!.rawQuery(myQuery, null)
        try {
            tasksC.moveToFirst()
            while (!tasksC.isAfterLast) {
                Log.d(TAG, "${tasksC.getLong(tasksC.getColumnIndex(KEY_ROWID))} / ${tasksC.getString(tasksC.getColumnIndex(KEY_TASK))}")
                tasksC.moveToNext()
            }
            tasksC.close()
        } catch (e: Exception) {
            Log.d(TAG, "Cursor usage threw e")
        }

    }

    /**
     * Dumps the contents of the tasks table to logcat, for testing.
     */
    fun dumpClockings() {
        Log.d(TAG, "Dumping clock table")
        if (mDb == null)
            open()
        val tasksC = mDb!!.rawQuery("select * from $CLOCK_DATABASE_TABLE order by $KEY_ROWID", null)
        try {
            tasksC.moveToFirst()
            while (!tasksC.isAfterLast) {
                Log.d(TAG, tasksC.getLong(tasksC.getColumnIndex(KEY_ROWID)).toString() + " / " + tasksC.getString(tasksC.getColumnIndex(KEY_CHARGENO)))
                tasksC.moveToNext()
            }
        } catch (e: Exception) {
            Log.d(TAG, "Cursor usage threw $e")
        }

        try {
            tasksC.close()
        } catch (e: Exception) {
            Log.d(TAG, "Cursor closing threw $e")
        }

    }

    companion object {
        val KEY_VERSION = "version"
        val KEY_ROWID = "_id"
        val KEY_CHARGENO = "chargeno"
        val KEY_TIMEIN = "timein"
        val KEY_TIMEOUT = "timeout"
        val KEY_RANGE = "range"
        val KEY_TOTAL = "total"
        val MAX_ROW = "max($KEY_ROWID)"
        val MAX_COUNT = "max($KEY_VERSION)"
        val KEY_TASK = "task"
        val KEY_ACTIVE = "active"
        val KEY_USAGE = "usage"
        val KEY_OLDUSAGE = "oldusage"
        val KEY_LASTUSED = "lastused"
        val KEY_PERCENTAGE = "percentage"
        val KEY_SPLIT = "split"
        val KEY_HOURS = "hours"
        val KEY_PARENTTASK = "parenttask"
        val DB_FALSE = "0"
        val DB_TRUE = "1"

        private val TAG = "TimeSheetDbAdapter"
        private var mDbHelper: DatabaseHelper? = null
        private var mDb: SQLiteDatabase? = null

        val DATABASE_NAME = "TimeSheetDB.db"
        private val TASKS_DATABASE_TABLE = "Tasks"
        private val TASKSPLIT_DATABASE_TABLE = "TaskSplit"
        private val CLOCK_DATABASE_TABLE = "TimeSheet"
        private val SUMMARY_DATABASE_TABLE = "Summary"
        private val ENTRYITEMS_VIEW = "EntryItems"
        private val ENTRYREPORT_VIEW = "EntryReport"
        private val TASKSPLITREPORT_VIEW = "TaskSplitReport"
        private val DATABASE_METADATA = "TimeSheetMeta"
        private val DATABASE_VERSION = 3

        /**
         * Database creation SQL statements
         */
        private val CLOCK_TABLE_CREATE = """CREATE TABLE
$CLOCK_DATABASE_TABLE ( $KEY_ROWID
INTEGER PRIMARY KEY AUTOINCREMENT, $KEY_CHARGENO
INTEGER NOT NULL REFERENCES $TASKS_DATABASE_TABLE (
$KEY_ROWID), $KEY_TIMEIN INTEGER NOT NULL,
$KEY_TIMEOUT INTEGER NOT NULL DEFAULT 0);"""

        // This probably allows duplicate KEY_TASK rows, which is bad.
        // There's an assumption in the code that a lookup by KEY_TASK
        // provides a unique KEY_ROWID when queried.
        // TODO: Fix non-unique KEY_TASK problem.
        private val TASK_TABLE_CREATE = """CREATE TABLE
$TASKS_DATABASE_TABLE ( $KEY_ROWID
INTEGER PRIMARY KEY AUTOINCREMENT, $KEY_TASK
TEXT NOT NULL, $KEY_ACTIVE BOOLEAN NOT NULL DEFAULT
$DB_TRUE, $KEY_USAGE INTEGER NOT NULL DEFAULT 0,
$KEY_OLDUSAGE INTEGER NOT NULL DEFAULT 0, $KEY_LASTUSED
INTEGER NOT NULL DEFAULT 0, $KEY_SPLIT
INTEGER DEFAULT 0);"""

        private val TASKSPLIT_TABLE_CREATE = """CREATE TABLE
$TASKSPLIT_DATABASE_TABLE ( $KEY_ROWID
INTEGER PRIMARY KEY AUTOINCREMENT, $KEY_CHARGENO
INTEGER NOT NULL REFERENCES $TASKS_DATABASE_TABLE (
$KEY_ROWID), $KEY_TASK INTEGER NOT NULL REFERENCES
$TASKS_DATABASE_TABLE ( $KEY_ROWID ), $KEY_PERCENTAGE
REAL NOT NULL DEFAULT 100 CHECK ($KEY_PERCENTAGE >=0 AND
$KEY_PERCENTAGE <= 100) );"""

        private val TASK_TABLE_ALTER3 = """ALTER TABLE
$TASKS_DATABASE_TABLE ADD COLUMN $KEY_SPLIT
INTEGER DEFAULT 0;"""

        private val ENTRYITEMS_VIEW_CREATE = """CREATE VIEW
$ENTRYITEMS_VIEW AS SELECT $CLOCK_DATABASE_TABLE.$KEY_ROWID as $KEY_ROWID,
$TASKS_DATABASE_TABLE.$KEY_TASK as $KEY_TASK,
$CLOCK_DATABASE_TABLE.$KEY_TIMEIN as $KEY_TIMEIN,
$CLOCK_DATABASE_TABLE.$KEY_TIMEOUT as $KEY_TIMEOUT FROM
$CLOCK_DATABASE_TABLE,$TASKS_DATABASE_TABLE WHERE
$CLOCK_DATABASE_TABLE.$KEY_CHARGENO =
$TASKS_DATABASE_TABLE.$KEY_ROWID;"""

        private val ENTRYREPORT_VIEW_CREATE = """CREATE VIEW
$ENTRYREPORT_VIEW AS SELECT $CLOCK_DATABASE_TABLE.$KEY_ROWID as $KEY_ROWID,
$TASKS_DATABASE_TABLE.$KEY_TASK as $KEY_TASK,
$CLOCK_DATABASE_TABLE.$KEY_TIMEIN as $KEY_TIMEIN,
$CLOCK_DATABASE_TABLE.$KEY_TIMEOUT as $KEY_TIMEOUT,
strftime("%H:%M",$KEY_TIMEIN/1000,"unixepoch","localtime") || ' to ' || CASE WHEN
$KEY_TIMEOUT = 0 THEN 'now' ELSE strftime("%H:%M",
KEY_TIMEOUT/1000,"unixepoch","localtime") END as
$KEY_RANGE FROM $CLOCK_DATABASE_TABLE,
$TASKS_DATABASE_TABLE WHERE $CLOCK_DATABASE_TABLE.$KEY_CHARGENO = $TASKS_DATABASE_TABLE.$KEY_ROWID;"""

        private val TASKSPLITREPORT_VIEW_CREATE = """CREATE VIEW TaskSplitReport AS
SELECT Tasks._id as _id,
TaskSplit.chargeno as parenttask,
Tasks.task as taskdesc,
TaskSplit.percentage as percentage
FROM Tasks, TaskSplit WHERE Tasks._id = TaskSplit.task"""

        private val SUMMARY_TABLE_CREATE = """CREATE TEMP TABLE
IF NOT EXISTS $SUMMARY_DATABASE_TABLE ($KEY_ROWID
INTEGER PRIMARY KEY AUTOINCREMENT, $KEY_TASK
TEXT NOT NULL, $KEY_TOTAL REAL DEFAULT 0);"""

        private val SUMMARY_TABLE_CLEAN = """DELETE FROM $SUMMARY_DATABASE_TABLE;"""
        private val VACUUM = "VACUUM;"

        private val TASKS_INDEX = """CREATE UNIQUE INDEX
${TASKS_DATABASE_TABLE}_index ON $TASKS_DATABASE_TABLE (
$KEY_TASK);"""
        private val CHARGENO_INDEX = """CREATE INDEX
${CLOCK_DATABASE_TABLE}_chargeno_index ON
$CLOCK_DATABASE_TABLE ($KEY_CHARGENO);"""
        private val SPLIT_INDEX = """CREATE INDEX
${TASKSPLIT_DATABASE_TABLE}_chargeno_index ON
$TASKSPLIT_DATABASE_TABLE ($KEY_CHARGENO);"""
        private val TIMEIN_INDEX = """CREATE INDEX
${CLOCK_DATABASE_TABLE}_timein_index ON $CLOCK_DATABASE_TABLE
($KEY_TIMEIN);"""
        private val TIMEOUT_INDEX = """CREATE INDEX
${CLOCK_DATABASE_TABLE}_timeout_index ON
$CLOCK_DATABASE_TABLE ($KEY_TIMEOUT);"""

        private val METADATA_CREATE = "create table TimeSheetMeta(version integer primary key);"
    }
}
/**
 * Create a new time entry using the charge number provided. If the entry is
 * successfully created return the new rowId for that note, otherwise return
 * a -1 to indicate failure.

 * @param chargeno the charge number for the entry
 * *
 * @return rowId or -1 if failed
 */
/**
 * Close supplied time entry. If the entry is successfully closed, return
 * the rowId for that entry, otherwise return a -1 to indicate failure.

 * @param chargeno the charge number for the entry
 * *
 * @return rowId or -1 if failed
 */
/**
 * Retrieve list of entries for the day surrounding the current time.

 * @return rowId or -1 if failed
 */
/**
 * Method that retrieves the entries for today from the entry view.

 * @return Cursor over the results.
 */
/**
 * Method that retrieves the entries for today from the entry view.

 * @return Cursor over the results.
 */
/**
 * Retrieve list of entries for the day surrounding the current time.

 * @return rowId or -1 if failed
 */
