package com.github.kronenpj.iqtimesheet.IQTimeSheet

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import android.util.Log
import com.github.kronenpj.iqtimesheet.IQTimeSheet.ITimeSheetDbAdapter.Companion.DB_FALSE
import com.github.kronenpj.iqtimesheet.IQTimeSheet.ITimeSheetDbAdapter.Companion.DB_TRUE
import com.github.kronenpj.iqtimesheet.TimeHelpers
import org.jetbrains.anko.db.*
import java.util.*

/**
 * Replacement for direct-converted Kotlin code.
 * Created by kronenpj on 6/22/17.
 */

class TimeSheetDbAdapter
/**
 * Primary Constructor - takes the context to allow the database to be
 * opened/created
 */
@JvmOverloads constructor(private val mCtx: Context,
                          private var instance: MySqlHelper = MySqlHelper.getInstance(mCtx)) : ITimeSheetDbAdapter {

    companion object {
        private const val TAG = "TimeSheetDbAdapter"
    }

    /* From Anko README
    fun getUsers(db: ManagedSQLiteOpenHelper): List<User> = db.use {
        db.select("Users")
                .whereSimple("family_name = ?", "John")
                .doExec()
                .parseList(UserParser)
    }
    */

    val timeTotalParser = rowParser { id: Long, task: String, total: Float ->
        ITimeSheetDbAdapter.timeTotalTuple(id, task, total)
    }
    val timeEntryParser = rowParser { id: Long, task: String, timein: Long, timeout: Long ->
        ITimeSheetDbAdapter.timeEntryTuple(id, task, timein, timeout)
    }
    val chargeNoParser = rowParser { id: Long, chargeno: Long, timein: Long, timeout: Long ->
        ITimeSheetDbAdapter.chargeNoTuple(id, chargeno, timein, timeout)
    }
    // TODO: active should be: Boolean
    val tasksParser = rowParser { id: Long, task: String, active: Long, usage: Long,
                                  oldusage: Long, lastused: Long ->
        ITimeSheetDbAdapter.tasksTuple(id, task, active, usage, oldusage, lastused)
    }
    val taskUsageParser = rowParser { id: Long, usage: Long, oldusage: Long, lastused: Long ->
        ITimeSheetDbAdapter.taskUsageTuple(id, usage, oldusage, lastused)
    }

    /**
     * Create a new time entry using the charge number provided. If the entry is
     * successfully created return the new rowId for that note, otherwise return
     * a -1 to indicate failure.
     *
     * @param chargeno the charge number for the entry
     *
     * @param timeInP  the time in milliseconds of the clock-in
     *
     * @return rowId or -1 if failed
     */
    override fun createEntry(chargeno: Long, timeInP: Long): Long {
        var timeIn = timeInP
        if (TimeSheetActivity.prefs!!.alignMinutesAuto) {
            timeIn = TimeHelpers.millisToAlignMinutes(timeIn,
                    TimeSheetActivity.prefs!!.alignMinutes)
        }

        incrementTaskUsage(chargeno)

        var result: Long = 0L

        Log.d(TAG, "createEntry: $chargeno at $timeIn (${TimeHelpers.millisToTimeDate(timeIn)})")
        instance.use {
            result = insert("TimeSheet", "chargeno" to chargeno, "timein" to timeIn)
        }
        return result
    }

    /**
     * Close an existing time entry using the charge number provided. If the
     * entry is successfully created return the new rowId for that note,
     * otherwise return a -1 to indicate failure.
     *
     * @param chargeno the charge number for the entry
     *
     * @param timeOutP the time in milliseconds of the clock-out
     *
     * @return rowId or -1 if failed
     */
    override fun closeEntry(chargeno: Long, timeOutP: Long): Int {
        var timeOut = timeOutP
        if (TimeSheetActivity.prefs!!.alignMinutesAuto) {
            timeOut = TimeHelpers.millisToAlignMinutes(timeOut,
                    TimeSheetActivity.prefs!!.alignMinutes)
            // TODO: Fix in a more sensible way.
            // Hack to account for a cross-day automatic clock out.
            // if (timeOut - origTimeOut == 1)
            // timeOut = origTimeOut;
        }

        // Stop the time closed from landing on a day boundary.
        val timeIn = getTimeEntryTuple(lastClockEntry())?.timein ?: -1L
        // TODO: Maybe need a boolean preference to enable / disable this?
        // long boundary = TimeHelpers.millisToEoDBoundary(timeIn,
        // TimeSheetActivity.prefs.getTimeZone());
        val boundary = TimeHelpers.millisToEoDBoundary(timeIn,
                TimeSheetActivity.prefs!!.timeZone)
        Log.d(TAG, "Boundary: $boundary")
        if (timeOut > boundary)
            timeOut = boundary - 1000

        Log.d(TAG, "closeEntry: $chargeno at $timeOut (${TimeHelpers.millisToTimeDate(timeOut)})")

        var result = 0
        // TODO: Figure out how to get the Long return code here.
        try {
            instance.use {
                result = update("TimeSheet", "timeout" to timeOut)
                        .whereArgs("_id = ${lastClockEntry()} and chargeno = $chargeno")
                        .exec()
            }
        } catch (e: SQLiteConstraintException) {
            result = -1
        }
        return result
    }

    /**
     * Delete the entry with the given rowId
     *
     * @param rowId code id of note to delete
     *
     * @return true if deleted, false otherwise
     */
    override fun deleteEntry(rowId: Long): Boolean {
        Log.i("Delete called", "value__ $rowId")

        var retcode = 0
        instance.use {
            retcode = delete("TimeSheet", "_id = $rowId")
        }
        return retcode > 0
    }

    /**
     * Return a Cursor over the list of all entries in the database
     *
     * @return Cursor over all database entries
     */
    override fun fetchAllTimeEntries(): Cursor? {
        val c: Cursor? = null
        instance.readableDatabase
                .query("TimeSheet", arrayOf("_id", "chargeno", "timein", "timeout"),
                        null, null, null, null, null)
        return c
    }

    /**
     * Return a Cursor positioned at the entry that matches the given rowId
     *
     * @param rowId id of entry to retrieve
     *
     * @return Cursor positioned to matching entry, if found
     *
     * @throws SQLException if entry could not be found/retrieved
     */
    @Throws(SQLException::class)
    override fun fetchEntry(rowId: Long): Cursor? {
        val mCursor = instance.readableDatabase
                .query("TimeSheet", arrayOf("_id", "chargeno", "timein", "timeout"),
                        "_id = $rowId", null, null, null, null)
        mCursor?.moveToFirst()
        return mCursor
    }

    /**
     * Return a Cursor positioned at the entry that matches the given rowId
     *
     * @param rowId id of entry to retrieve
     *
     * @return Cursor positioned to matching entry, if found
     *
     * @throws SQLException if entry could not be found/retrieved
     */
    @Throws(SQLException::class)
    override fun fetchEntry(rowId: Long, column: String): Cursor? {
        val mCursor = instance.readableDatabase
                .query("TimeSheet", arrayOf(column), "_id = $rowId", null, null, null, null)
        mCursor?.moveToFirst()
        return mCursor
    }

    /**
     * Update the note using the details provided. The entry to be updated is
     * specified using the rowId, and it is altered to use the date and time
     * values passed in
     *
     * @param rowIdP    id of entry to update
     *
     * @param chargeno change number to update
     *
     * @param date     the date of the entry
     *
     * @param timein   the time work started on the task
     *
     * @param timeout  the time work stopped on the task
     *
     * @return true if the entry was successfully updated, false otherwise
     */
    override fun updateEntry(rowIdP: Long, chargeno: Long, date: String?,
                             timein: Long, timeout: Long): Boolean {
        var rowId = rowIdP
        val args = ContentValues()
        // Only change items that aren't null or -1.
        if (timein != -1L)
            args.put("timein", timein)
        if (chargeno != -1L)
            args.put("chargeno", chargeno)
        if (timeout != -1L)
            args.put("timeout", timeout)

        if (rowId == -1L)
            rowId = lastClockEntry()

        var retbool = false
        instance.use {
            retbool = update("TimeSheet", args, "_id = ?", arrayOf("$rowId")) > 0
        }
        return retbool
    }

    /**
     * Retrieve the taskID of the last entry in the clock table.
     *
     * @return rowId or -1 if failed
     */
    override fun taskIDForLastClockEntry(): Long {
        val lastClockID = lastClockEntry()

        var retval: Long? = null
        instance.use {
            retval = select("TimeSheet", "chargeno")
                    .whereArgs("_id = $lastClockID")
                    .parseOpt(LongParser)
        }

        return retval ?: -1L
    }

    /**
     * Retrieve the timeIn of the last entry in the clock table.
     *
     * @return time in in milliseconds or -1 if failed
     */
    override fun timeInForLastClockEntry(): Long {
        val lastClockID = lastClockEntry()

        var retval: Long? = -1L
        instance.use {
            retval = select("TimeSheet", "timein")
                    .whereArgs("_id = $lastClockID")
                    .parseOpt(LongParser)
        }

        return retval ?: -1L
    }

    /**
     * Retrieve the time out of the last entry in the clock table.
     *
     * @return time out in milliseconds or -1 if failed
     */
    override fun timeOutForLastClockEntry(): Long {
        val lastClockID = lastClockEntry()

        var retval: Long? = -1
        instance.use {
            retval = select("TimeSheet", "timeout")
                    .whereArgs("_id = $lastClockID")
                    .parseOpt(LongParser)
        }

        return retval ?: -1L
    }

    /**
     * Retrieve the row of the last entry in the tasks table.
     *
     * @return rowId or -1 if failed
     */
    override fun lastTaskEntry(): Long {
        var retval: Long? = -1
        instance.use {
            retval = select("Tasks", "max(_id)")
                    .parseOpt(LongParser)
        }

        return retval ?: -1L
    }

    /**
     * Return a timeEntryTuple of the entry that matches the given rowId
     *
     * @param rowId id of entry to retrieve
     *
     * @return timeEntryTuple of the matching entry, if found. null if not.
     *
     * @throws SQLException if entry could not be found/retrieved
     */
    @Throws(SQLException::class)
    override fun getTimeEntryTuple(rowId: Long): ITimeSheetDbAdapter.timeEntryTuple? {
        var retval: ITimeSheetDbAdapter.timeEntryTuple? = null
        instance.use {
            retval = select("EntryItems", "_id", "task", "timein", "timeout")
                    .whereArgs("_id = $rowId")
                    .parseOpt(parser = timeEntryParser)
        }
        return retval
    }

    /**
     * Return a getChargeNoTuple of the entry that matches the given rowId
     *
     * @param rowId id of entry to retrieve
     *
     * @return timeEntryTuple of the matching entry, if found. null if not.
     *
     * @throws SQLException if entry could not be found/retrieved
     */
    @Throws(SQLException::class)
    override fun getChargeNoTuple(rowId: Long): ITimeSheetDbAdapter.chargeNoTuple? {
        var retval: ITimeSheetDbAdapter.chargeNoTuple? = null
        instance.use {
            retval = select("TimeSheet", "_id", "chargeno", "timein", "timeout")
                    .whereArgs("_id = $rowId")
                    .parseOpt(parser = chargeNoParser)
        }
        return retval
    }

    /**
     * Retrieve the entry in the timesheet table immediately prior to the
     * supplied entry.
     *
     * @return rowId or -1 if failed
     */
    override fun getPreviousClocking(rowID: Long): Long {
        var thisTimeIn: Long = -1
        var prevTimeOut: Long = -1

        Log.d(TAG, "getPreviousClocking for row: $rowID")

        // Get the tuple from the provided row
        var mCurrent: ITimeSheetDbAdapter.timeEntryTuple? = getTimeEntryTuple(rowID)

        // KEY_ROWID, KEY_TASK, KEY_TIMEIN, KEY_TIMEOUT
        thisTimeIn = mCurrent?.timein ?: return -1L
        Log.d(TAG, "timeIn for current: $thisTimeIn")

        var retval: ITimeSheetDbAdapter.timeEntryTuple? = null
        instance.use {
            retval = select("EntryItems", "_id", "task", "timein", "timeout")
                    .whereArgs("_id < $rowID")
                    .orderBy("_id", SqlOrderDirection.DESC)
                    .limit(1)
                    .parseOpt(parser = timeEntryParser)
        }

        var prevRowID = retval?.id ?: return -1L
        Log.d(TAG, "rowID for previous: $prevRowID")

        // Get the tuple from the just-retrieved row
        mCurrent = getTimeEntryTuple(prevRowID)
        // KEY_ROWID, KEY_TASK, KEY_TIMEIN, KEY_TIMEOUT

        prevTimeOut = mCurrent?.timeout ?: return -1L
        Log.d(TAG, "timeOut for previous: $prevTimeOut")

        // If the two tasks don't flow from one to another, don't allow the
        // entry to be adjusted.
        if (thisTimeIn != prevTimeOut)
            prevRowID = -1

        return prevRowID
    }

    /**
     * Retrieve the entry in the timesheet table immediately following the
     * supplied entry.
     *
     * @return rowId or -1 if failed
     */
    // TODO: Should this be chronological or ordered by _id? as it is now?
    // And, if it should be chronological by time in or time out or both... :(
    override fun getNextClocking(rowID: Long): Long {
        var thisTimeOut: Long = -1L
        var nextTimeIn: Long = -1L

        Log.d(TAG, "getNextClocking for row: $rowID")

        // Get the tuple from the provided row
        var mCurrent: ITimeSheetDbAdapter.timeEntryTuple? = getTimeEntryTuple(rowID)
        // KEY_ROWID, KEY_TASK, KEY_TIMEIN, KEY_TIMEOUT
        thisTimeOut = mCurrent?.timeout ?: return -1L
        Log.d(TAG, "timeOut for current: $thisTimeOut")

        var retval: ITimeSheetDbAdapter.timeEntryTuple? = null
        instance.use {
            retval = select("EntryItems", "_id", "task", "timein", "timeout")
                    .whereArgs("_id > $rowID")
                    .orderBy("_id")
                    .limit(1)
                    .parseOpt(parser = timeEntryParser)
        }

        var nextRowID = retval?.id ?: return -1
        Log.d(TAG, "rowID for next: $nextRowID")

        // Get the tuple from the just-retrieved row
        mCurrent = getTimeEntryTuple(nextRowID)
        // KEY_ROWID, KEY_TASK, KEY_TIMEIN, KEY_TIMEOUT
        nextTimeIn = mCurrent?.timein ?: return -1L
        Log.d(TAG, "timeIn for next: $nextTimeIn")

        // If the two tasks don't flow from one to another, don't allow the
        // entry to be adjusted.
        if (thisTimeOut != nextTimeIn)
            nextRowID = -1

        return nextRowID
    }

    /**
     * Retrieve the row of the last entry in the clock table.
     *
     * @return rowId or -1 if failed
     */
    override fun lastClockEntry(): Long {
        var response: Long = -1
        instance.use {
            select("TimeSheet", "max(_id)").exec {
                response = parseOpt(LongParser) ?: -1L
            }
        }
        return response
    }

    /**
     * Retrieve an array of rows for today's entry in the clock table.
     *
     * @return array of rowId's or null if failed
     */
    override fun todaysEntries(): LongArray? {
        val now = TimeHelpers.millisNow()
        val todayStart = TimeHelpers.millisToStartOfDay(now)
        val todayEnd = TimeHelpers.millisToEndOfDay(now)
        val rows: LongArray

        // public Cursor query(boolean distinct, String table, String[] columns,
        // String selection, String[] selectionArgs, String groupBy, String
        // having, String orderBy) {
        // cursor.asSequence()

        var tmp: List<Long>? = null
        instance.use {
            tmp = select("TimeSheet", "_id")
                    .whereArgs("timein >= $todayStart and (timeout <= $todayEnd or timeout = 0")
                    .parseList(LongParser)
        }

        if (tmp == null || (tmp as List<Long>).count() == 0) {
            // TODO: Figure out how to get a context into this class...
            // toast("No entries in the database for today." as CharSequence)
            return null
        }

        rows = LongArray((tmp as List<Long>).count())
        var count = 0
        (tmp as List<Long>).forEach {
            rows[count++] = it
        }

        return rows
    }

    /**
     * Retrieve list of entries for the day surrounding the supplied time.
     *
     * @return rowId or -1 if failed
     */
    override fun getEntryReportCursor(distinct: Boolean, columns: Array<String>,
                                      groupBy: String?, orderBy: String?, start: Long, end: Long): Cursor? {
        // public Cursor query(boolean distinct, String table, String[] columns,
        // String selection, String[] selectionArgs, String groupBy, String
        // having, String orderBy, String limit) {

        val selection: String
        val endDay = TimeHelpers.millisToDayOfMonth(end - 1000)
        val now = TimeHelpers.millisNow()
        selection = if (TimeHelpers.millisToDayOfMonth(now - 1000) == endDay ||
                TimeHelpers.millisToDayOfMonth(TimeHelpers.millisToEndOfWeek(now - 1000,
                        TimeSheetActivity.prefs!!.weekStartDay,
                        TimeSheetActivity.prefs!!.weekStartHour)) == endDay) {
            Log.d(TAG, "getEntryReportCursor: Allowing selection of zero-end-hour entries.")
            "timein >=? and timeout <= ? and (timeout >= timein or timeout = 0)"
        } else {
            "timein >=? and timeout <= ? and timeout >= timein"
        }

        Log.d(TAG, "getEntryReportCursor: Selection criteria: $selection")
        Log.d(TAG, "getEntryReportCursor: Selection arguments: $start, $end")
        Log.d(TAG, "getEntryReportCursor: Selection arguments: ${TimeHelpers.millisToTimeDate(start)}, ${TimeHelpers.millisToTimeDate(end)}")

        val mCursor = instance.readableDatabase.query(distinct, "EntryReport", columns,
                selection, arrayOf(start.toString(), end.toString()), groupBy, null,
                orderBy, null)

        mCursor?.moveToLast() ?: Log.e(TAG, "getEntryReportCursor: mCursor for range is null.")

        if (mCursor?.isAfterLast != false) {
            Log.d(TAG, "getEntryReportCursor: mCursor for range is empty.")
            // Toast.makeText(mCtx,
            // "No entries in the database for supplied range.",
            // Toast.LENGTH_SHORT).show();
            return null
        }

        return mCursor
    }

    /**
     * Given a task ID, close the task and, if the ID differs, open a new task.
     * Refresh the data displayed.
     *
     * @return true if a new task was started, false if the old task was
     * stopped.
     */
    override fun processChange(taskID: Long): Boolean {
        Log.d(TAG, "processChange for task ID: $taskID")

        var lastRowID = lastClockEntry()
        val lastTaskID = taskIDForLastClockEntry()

        Log.d(TAG, "Last Task Entry Row: $lastRowID")

        val timeOut = getTimeEntryTuple(lastRowID)?.timeout ?: -1L

        // Determine if the task has already been chosen and is now being
        // closed.
        if (timeOut == 0L && lastTaskID == taskID) {
            closeEntry(taskID)
            Log.d(TAG, "Closed task ID: $taskID")
            return false
        } else {
            if (timeOut == 0L)
                closeEntry()
            createEntry(taskID)

            lastRowID = lastClockEntry()
            val tempClockCursor = fetchEntry(lastRowID)

            tempClockCursor?.close()

            Log.d(TAG, "processChange ID from $lastTaskID to $taskID")
            return true
        }
    }

    /**
     * Retrieve list of entries for the day surrounding the supplied time.
     *
     * @return rowId or -1 if failed
     */
    override fun getSummaryCursor(distinct: Boolean, columns: Array<String>,
                                  groupBy: String?, orderBy: String?, start: Long, end: Long): Cursor? {
        // public Cursor query(boolean distinct, String table, String[] columns,
        // String selection, String[] selectionArgs, String groupBy, String
        // having, String orderBy, String limit) {

        // String selection;

        // Log.d(TAG, "getSummaryCursor: Selection criteria: " +
        // selection);
        try {
            Log.d(TAG, "getSummaryCursor: Columns: ${columns[0]}, ${columns[1]} and ${columns[2]}")
        } catch (e: Exception) {
            Log.d(TAG, "getSummaryCursor has fewer than 3 columns.")
        }

        Log.d(TAG, "getSummaryCursor: Selection arguments: $start, $end")
        Log.d(TAG, "getSummaryCursor: Selection arguments: ${TimeHelpers.millisToTimeDate(start)}, ${TimeHelpers.millisToTimeDate(end)}")
        // Cursor mCursor = mDb.query(distinct, SUMMARY_DATABASE_TABLE, columns,
        // selection, new String[] { String.valueOf(start).toString(),
        // String.valueOf(end).toString() }, groupBy, null, orderBy, null);

        // TODO: Below KEY_STOTAL was KEY_TOTAL
        // Cursor mCursor = mDb.query(distinct, SUMMARY_DATABASE_TABLE,
        // columns,
        // KEY_STOTAL + " > 0",
        // new String[] { String.valueOf(0).toString(),
        // String.valueOf(end).toString() }, groupBy, null,
        // orderBy, null);

        var select = "SELECT "
        if (distinct) select += "DISTINCT "
        for (c in columns.indices) {
            select += if (c == 0) {
                columns[c]
            } else {
                (", " + columns[c])
            }
        }
        select += " FROM Summary WHERE total > 0"
        if (groupBy != null) select += " GROUP BY $groupBy"
        if (orderBy != null) select += " ORDER BY $orderBy"
        Log.d(TAG, "getSummaryCursor: query: $select")

        val mCursor = instance.readableDatabase.rawQuery(select, null)

        mCursor?.moveToLast() ?: Log.e(TAG, "entryReport mCursor for range is null.")

        if (mCursor?.isAfterLast != false) {
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
     *
     * @return rowId or -1 if failed
     */
    override fun dayEntryReport(timeP: Long): Cursor? {
        var time = timeP
        if (time <= 0) time = TimeHelpers.millisNow()

        val todayStart = TimeHelpers.millisToStartOfDay(time)
        val todayEnd = TimeHelpers.millisToEndOfDay(time)

        Log.d(TAG, "dayEntryReport start: ${TimeHelpers.millisToTimeDate(todayStart)}")
        Log.d(TAG, "dayEntryReport   end: ${TimeHelpers.millisToTimeDate(todayEnd)}")

        val columns = arrayOf("_id", "task", "range", "timein", "timeout")
        return getEntryReportCursor(false, columns, null, "timein", todayStart, todayEnd)
    }

    /**
     * Method that populates a temporary table for a single specified day from
     * the entry view.
     *
     * @param timeP    The time around which to report
     *
     * @param omitOpen Include an open task in the result
     *
     * @return Cursor over the results.
     */
    // TODO: Finish and replace the other routines with it.
    override fun daySummary(timeP: Long, omitOpen: Boolean): Cursor? {
        var time = timeP
        Log.d(TAG, "In daySummary.")
        if (time <= 0)
            time = TimeHelpers.millisNow()

        var todayStart = TimeHelpers.millisToStartOfDay(time)
        var todayEnd = TimeHelpers.millisToEndOfDay(time)

        // TODO: This is a first attempt and is likely to be convoluted.
        // If today is the split day for a 9/80-style week, adjust the start/end times appropriately.
        if (TimeHelpers.millisToDayOfWeek(todayStart) == TimeSheetActivity.prefs!!.weekStartDay
                && TimeSheetActivity.prefs!!.weekStartHour > 0) {
            val splitMillis = (TimeSheetActivity.prefs!!.weekStartHour * 3600 * 1000).toLong()
            // This is the partial day where the week splits, only for schedules like a 9/80.
            if (time < todayStart + splitMillis)
            // Move the end of the day to the split time.
                todayEnd = todayStart + splitMillis
            else
                todayStart += splitMillis // Move the start of the day to the split time.
        }

        Log.d(TAG, "daySummary start: ${TimeHelpers.millisToTimeDate(todayStart)}")
        Log.d(TAG, "daySummary   end: ${TimeHelpers.millisToTimeDate(todayEnd)}")

        populateSummary(todayStart, todayEnd, omitOpen)

        val columns = arrayOf("_id", "task", "total")
        val groupBy = "task"
        val orderBy = "total DESC"
        return try {
            getSummaryCursor(true, columns, groupBy, orderBy,
                    todayStart, todayEnd)
        } catch (e: SQLiteException) {
            Log.e(TAG, "getSummaryCursor: $e")
            null
        }
    }

    /**
     * Retrieve list of entries for the week surrounding the supplied time.
     *
     * @return Cursor over the entries
     */
    override fun weekEntryReport(timeP: Long): Cursor? {
        var time = timeP
        if (time <= 0) time = TimeHelpers.millisNow()

        val todayStart = TimeHelpers.millisToStartOfWeek(time,
                TimeSheetActivity.prefs!!.weekStartDay,
                TimeSheetActivity.prefs!!.weekStartHour)
        val todayEnd = TimeHelpers.millisToEndOfWeek(time,
                TimeSheetActivity.prefs!!.weekStartDay,
                TimeSheetActivity.prefs!!.weekStartHour)

        // public Cursor query(boolean distinct, String table, String[] columns,
        // String selection, String[] selectionArgs, String groupBy, String
        // having, String orderBy) {

        Log.d(TAG, "weekEntryReport start: ${TimeHelpers.millisToTimeDate(todayStart)}")
        Log.d(TAG, "weekEntryReport   end: ${TimeHelpers.millisToTimeDate(todayEnd)}")

        val columns = arrayOf("_id", "task", "range", "timein", "timeout")
        return getEntryReportCursor(false, columns, todayStart, todayEnd)
    }

    /**
     * Method that populates a temporary table for a single specified day from
     * the entry view.
     *
     * @return Cursor over the results.
     */
    // TODO: Finish and replace the other routines with it.
    override fun weekSummary(timeP: Long, omitOpen: Boolean): Cursor? {
        var time = timeP
        Log.d(TAG, "In weekSummary.")
        if (time <= 0)
            time = TimeHelpers.millisNow()
        Log.d(TAG, "weekSummary time arg: ${TimeHelpers.millisToTimeDate(time)}")

        val weekStart = TimeHelpers.millisToStartOfWeek(time,
                TimeSheetActivity.prefs!!.weekStartDay,
                TimeSheetActivity.prefs!!.weekStartHour)
        val weekEnd = TimeHelpers.millisToEndOfWeek(weekStart + 86400000,
                TimeSheetActivity.prefs!!.weekStartDay,
                TimeSheetActivity.prefs!!.weekStartHour)

        Log.d(TAG, "weekSummary start: ${TimeHelpers.millisToTimeDate(weekStart)}")
        Log.d(TAG, "weekSummary   end: ${TimeHelpers.millisToTimeDate(weekEnd)}")

        populateSummary(weekStart, weekEnd, omitOpen)

        // String[] columns = { KEY_TASK, KEY_HOURS };
        // String groupBy = KEY_TASK;
        // String orderBy = KEY_TASK;
        val columns = arrayOf("_id", "task", "total")
        val groupBy = "task"
        val orderBy = "total DESC"
        return try {
            getSummaryCursor(true, columns, groupBy, orderBy, weekStart,
                    weekEnd)
        } catch (e: SQLiteException) {
            Log.e(TAG, "getSummaryCursor: ${e.localizedMessage}")
            null
        }
    }

    /**
     *
     * @param summaryStart The start time for the summary
     * *
     * @param summaryEnd   The end time for the summary
     * *
     * @param omitOpen     Whether the summary should omit an open task
     */
    override fun populateSummary(summaryStart: Long, summaryEnd: Long,
                                 omitOpen: Boolean) {

        Log.v(TAG, "populateSummary: Creating summary table.")
        instance.readableDatabase.execSQL("""CREATE TEMP TABLE
IF NOT EXISTS Summary ('_id' INTEGER PRIMARY KEY AUTOINCREMENT,
'task' TEXT NOT NULL,
'total' REAL DEFAULT 0);""")

        Log.v(TAG, "populateSummary: Cleaning summary table.")
        instance.readableDatabase.execSQL("DELETE from Summary")
        instance.readableDatabase.execSQL("VACUUM")

        var omitOpenQuery = ""
        if (omitOpen) omitOpenQuery = "TimeSheet.timeout > 0 AND "

        val populateTemp1 = """INSERT INTO Summary ( task, total ) SELECT Tasks.task,
SUM((CASE WHEN TimeSheet.timeout = 0 THEN ${TimeHelpers.millisNow()} ELSE
TimeSheet.timeout END - TimeSheet.timein )/3600000.0) AS total FROM TimeSheet, Tasks
WHERE TimeSheet.timeout <= $summaryEnd AND $omitOpenQuery TimeSheet.timein >=
$summaryStart AND TimeSheet.chargeno = Tasks._id AND Tasks.split = 0 GROUP BY task"""
        Log.v(TAG, "populateTemp1\n$populateTemp1")
        instance.readableDatabase.execSQL(populateTemp1)

        val populateTemp2 = """INSERT INTO Summary (task,total) SELECT
TaskSplitReport.taskdesc, (SUM((CASE WHEN TimeSheet.timeout = 0 THEN
${TimeHelpers.millisNow()} ELSE TimeSheet.timeout END - TimeSheet.timein )/3600000.0) *
(TaskSplit.percentage /100.0)) AS total FROM TimeSheet, TaskSplit, Tasks, TaskSplitReport
WHERE TimeSheet.timeout <= $summaryEnd AND $omitOpenQuery TimeSheet.timein >= $summaryStart AND
TimeSheet.chargeno = Tasks._id AND Tasks._id = TaskSplit.chargeno AND
Tasks._id = TaskSplitReport.parenttask AND TaskSplit.task ||
TaskSplit.percentage = TaskSplitReport._id || TaskSplitReport.percentage
GROUP BY TaskSplit.task"""
// TODO: Figure out why the second-to-last line above makes no sense.
        Log.v(TAG, "populateTemp2\n$populateTemp2")
        instance.readableDatabase.execSQL(populateTemp2)
    }

    /**
     * Create a new time entry using the charge number provided. If the entry is
     * successfully created return the new rowId for that number, otherwise
     * return a -1 to indicate failure.
     *
     * @param task the charge number text for the entry
     *
     * @return rowId or -1 if failed
     */
    override fun createTask(task: String): Long {
        Log.d(TAG, "createTask: $task")
        val tempDate = System.currentTimeMillis() // Local time...

        var retval: Long = -1L
        instance.use {
            retval = insert("Tasks", "task" to task, "lastused" to tempDate)
        }
        return retval
    }

    /**
     * Create a new split time entry using the information provided. If the
     * entry is successfully created return the new rowId for that number,
     * otherwise return a -1 to indicate failure.
     *
     * @param task       the charge number text for the entry
     *
     * @param parent     The parent for the split task
     *
     * @param percentage The amount this task contributes to the task.
     *
     * @return rowId or -1 if failed
     */
    override fun createTask(task: String, parent: String, percentage: Int): Long {
        Log.d(TAG, "createTask: $task")
        Log.d(TAG, "    parent: $parent")
        Log.d(TAG, "percentage: $percentage")
        val tempDate = System.currentTimeMillis() // Local time...
        val parentId = getTaskIDByName(parent)

        var newRow: Long = -1L
        var retval: Long = -1L
        instance.use {
            newRow = insert("Tasks", "task" to task, "lastused" to tempDate, "split" to 1)
            Log.d(TAG, "new row   : $newRow")
            update("Tasks", "split" to 2).whereArgs("_id = $parentId").exec()
            retval = insert("TaskSplit", "task" to newRow, "chargeno" to parentId,
                    "percentage" to percentage)
        }

        return retval
    }

    /**
     * Return a Cursor over the list of all tasks in the database eligible to be
     * split task parents.
     *
     * @return Cursor over all database entries
     */
    override fun fetchParentTasks(): Array<String>? {
        Log.d(TAG, "fetchParentTasks: Issuing DB query.")

        var retval: Array<String>? = null
        instance.use {
            retval = select("Tasks", "task")
                    .whereArgs("active = '$DB_TRUE' and split != 1")
                    .parseList(StringParser).toTypedArray()
        }
        return retval
    }

    /**
     * Return an array over the list of all tasks in the database that are children
     * of the supplied split task parent.
     *
     * @return Array over all matching database entries
     */
    /*
    CREATE TABLE TaskSplit (
        _id INTEGER PRIMARY KEY AUTOINCREMENT,
        chargeno INTEGER NOT NULL REFERENCES Tasks(_id),
        task INTEGER NOT NULL REFERENCES Tasks(_id),
        percentage INTEGER NOT NULL DEFAULT 100 CHECK(percentage>=0 AND percentage<=100)
     );
     */
    override fun fetchChildTasks(parentID: Long): Array<Long> {
        var retval: Array<Long>? = null
        instance.use {
            retval = select("TaskSplit", "task")
                    .whereArgs("chargeno = '$parentID'")
                    .parseList(LongParser).toTypedArray()
        }
        return retval ?: emptyArray()
    }

    /**
     * Return an Array of all Tasks entries in the database
     *
     * @return Array of all Tasks database entries
     */
    override fun fetchAllTaskEntries(): Array<ITimeSheetDbAdapter.tasksTuple>? {
        Log.d(TAG, "fetchAllTaskEntries: Issuing DB query.")

        var retval: Array<ITimeSheetDbAdapter.tasksTuple>? = null
        instance.use {
            retval = select("Tasks", "_id", "task", "active", "usage", "oldusage", "lastused")
                    .whereArgs("active = '$DB_TRUE'")
                    .orderBy("usage + oldusage / 2", SqlOrderDirection.DESC)
                    .parseList(tasksParser).toTypedArray()
        }
        return retval
    }

    /**
     * Return a Cursor over the list of all entries in the database
     *
     * @return Cursor over all database entries
     */
    override fun fetchAllDisabledTasks(): Array<ITimeSheetDbAdapter.tasksTuple>? {
        Log.d(TAG, "fetchAllDisabledTasks: Issuing DB query.")
        var retval: Array<ITimeSheetDbAdapter.tasksTuple>? = null
        instance.use {
            retval = select("Tasks",
                    "_id", "task", "active", "usage", "oldusage", "lastused")
                    .whereArgs("active='$DB_FALSE'")
                    .orderBy("task")
                    .parseList(tasksParser).toTypedArray()
        }
        Log.i(TAG, "Fetched ${retval!!.size} disabled tasks.")
        return retval
    }

    /**
     * Return a Cursor positioned at the entry that matches the given rowId
     *
     * @param rowId id of entry to retrieve
     *
     * @return Cursor positioned to matching entry, if found
     *
     * @throws SQLException if entry could not be found/retrieved
     */
    @Throws(SQLException::class)
    override fun fetchTask(rowId: Long): Cursor {
        Log.d(TAG, "fetchTask: Issuing DB query.")
        val mCursor = instance.readableDatabase.query(true, "Tasks",
                arrayOf("_id", "task", "active", "usage", "oldusage", "lastused"),
                "_id = $rowId", null, null, null, null, null)
        mCursor?.moveToFirst()
        return mCursor
    }

    /**
     * Retrieve the task ID of the supplied task name.
     *
     * @return rowId or -1 if failed
     */
    override fun getTaskIDByName(name: String): Long {
        Log.d(TAG, "getTaskIDByName: Issuing DB query.")

        var response: Long = -1L
        instance.use {
            response = select("Tasks", "_id")
                    .whereArgs("task = '$name'")
                    .parseSingle(LongParser)
        }

        Log.d(TAG, "getTaskIDByName: $response")
        return response
    }

    /**
     * Retrieve the task name for the supplied task ID.
     *
     * @param taskID The identifier of the desired task
     *
     * @return Name of the task identified by the taskID
     */
    override fun getTaskNameByID(taskID: Long): String? {
        Log.d(TAG, "getTaskNameByID: Issuing DB query for ID: $taskID")

        var response = ""
        if (taskID < 0) {
            Log.d(TAG, "getTaskNameByID: $taskID is less than 0, returning empty string.")
            return response
        }
        try {
            instance.use {
                response = select("Tasks", "task")
                        .whereArgs("_id = '$taskID'")
                        .parseSingle(StringParser)
            }
        } catch (e: SQLException) {
            Log.d(TAG, "getTaskNameByID: Task ID '$taskID' doesn't have an entry.")
            Log.d(TAG, "getTaskNameByID: Ignoring SQLException: $e")
        }

        return response
    }

    /**
     * Return the entry that matches the given rowId
     *
     * @param splitTask id of task to retrieve
     *
     * @return parent's task ID, if found, 0 if not
     */
    override fun getSplitTaskParent(splitTask: String): Long {
        Log.d(TAG, "getSplitTaskParent: $splitTask")
        return getSplitTaskParent(getTaskIDByName(splitTask))
    }

    /**
     * Return the entry that matches the given rowId
     *
     * @param rowId id of task to retrieve
     *
     * @return parent's task ID, if found, 0 if not
     */
    override fun getSplitTaskParent(rowId: Long): Long {
        Log.d(TAG, "getSplitTaskParent: Issuing DB query. requesting rrrow ID: $rowId")
        var retval: Long = -1L

        if (rowId < 0) {
            Log.d(TAG, "getSplitTaskParent: $rowId is less than 0, returning -1.")
            return retval
        }
        try {
            instance.use {
                retval = select("TaskSplit", "chargeno")
                        .whereArgs("task = $rowId")
                        .parseSingle(LongParser)
            }
        } catch (e: SQLException) {
            Log.d(TAG, "getSplitTaskParent: '${getTaskNameByID(rowId)}' doesn't have a parent.")
            Log.d(TAG, "getSplitTaskParent: Ignoring SQLException: $e")
        }

        Log.d(TAG, "getSplitTaskParent: $retval / ${getTaskNameByID(retval)}")
        return retval
    }

    /**
     * Return the entry that matches the given rowId
     *
     * @param rowId id of task to retrieve
     *
     * @return parent's task ID, if found, 0 if not
     */
    override fun getSplitTaskPercentage(rowId: Long): Int {
        Log.d(TAG, "getSplitTaskPercentage: Issuing DB query.")
        var retval: Int = -1

        if (rowId < 0) {
            Log.d(TAG, "getSplitTaskPercentage: $rowId is less than 0, returning -1.")
            return retval
        }
        try {
            instance.use {
                retval = select("tasksplit", "percentage")
                        .whereArgs("task = $rowId")
                        .parseSingle(IntParser)
            }
        } catch (e: SQLException) {
            Log.d(TAG, "getSplitTaskPercentage: '${getTaskNameByID(rowId)}' doesn't have a percentage.")
            Log.d(TAG, "getSplitTaskPercentage: Ignoring SQLException: $e")
        }

        Log.d(TAG, "getSplitTaskPercentage: $retval")
        return retval
    }

    /**
     * Return the flag whether the task that matches the given rowId is a split
     * task.
     *
     * @param rowId id of task to retrieve
     *
     * @return 1 if the task is part of a split, and if found, 0 otherwise
     */
    override fun getSplitTaskFlag(rowId: Long): Int {
        Log.d(TAG, "getSplitTaskFlag: Issuing DB query.")
        var retval: Int = 0

        if (rowId < 0) {
            Log.d(TAG, "getSplitTaskFlag: $rowId is less than 0, returning 0 (False).")
            return retval
        }
        try {
            instance.use {
                retval = select("Tasks", "split")
                        .whereArgs("_id = $rowId")
                        .parseSingle(IntParser)
            }
        } catch (e: SQLException) {
            Log.d(TAG, "getSplitTaskFlag: '${getTaskNameByID(rowId)}' isn't part of a split.")
            Log.d(TAG, "getSplitTaskFlag: Ignoring SQLException: $e")
        }

        Log.d(TAG, "getSplitTaskFlag: $retval")
        return retval
    }

    /**
     * Return the number of children whose parent matches the given rowId
     *
     * @param rowId id of task to retrieve
     *
     * @return Number of "children" of this task, 0 if none.
     */
    override fun getQuantityOfSplits(rowId: Long): Long {
        Log.d(TAG, "getQuantityOfSplits: Issuing DB query.")
        var retval: Long = 0

        try {
            instance.use {
                retval = select("taskSplit", "count(task)")
                        .whereArgs("chargeno = $rowId")
                        .parseSingle(LongParser)
            }
        } catch (e: SQLException) {
            Log.d(TAG, "getQuantityOfSplits: '${getTaskNameByID(rowId)}' isn't part of a split.")
            Log.d(TAG, "getQuantityOfSplits: Ignoring SQLException: $e")
        }

        Log.d(TAG, "getQuantityOfSplits: $retval")
        return retval
    }

    /**
     * Rename specified task
     *
     * @param origName Old task name
     *
     * @param newName  New task name
     */
    override fun renameTask(origName: String, newName: String) {
        Log.d(TAG, "renameTask: Issuing DB query.")
        val taskID = getTaskIDByName(origName)

        instance.use {
            update("Tasks", "task" to newName)
                    .whereArgs("_id = $taskID")
                    .exec()
        }
    }

    /**
     * Alter specified split task
     *
     * @param rowID      Task ID to change
     *
     * @param parentIDP   New parent ID
     *
     * @param percentage New percentage
     *
     * @param split      New split flag state
     */
    override fun alterSplitTask(rowID: Long, parentIDP: Long, percentage: Int, split: Int) {
        var parentID = parentIDP
        Log.d(TAG, "alterSplitTask: Issuing DB query.")
        val currentParent = getSplitTaskParent(rowID)
        val currentSplit = getSplitTaskFlag(rowID)

        if (split == 0 && currentSplit == 1)
            parentID = -1

        // If the number of sub-splits under the parent task is 1 (<2) and we
        // are changing the parent, set the split flag to 0.
        if (getQuantityOfSplits(currentParent) < 2 && currentParent != parentID) {
            val i = instance.use {
                update("Tasks", "split" to 0).whereArgs(
                        "_id = $currentParent").exec()
            }
            Log.d(TAG, "Reverting task $currentParent to standard task returned $i")
        }

        // Set the flag on the new parent
        if (currentParent != parentID && parentID > 0) {
            val initialValues = ContentValues(1)
            initialValues.put("split", 2)
            val i = instance.use {
                update("Tasks", "split" to 2)
                        .whereArgs("_id = $parentID").exec()
            }
            Log.d(TAG, "Converting task $parentID to parent task returned: $i")
        }

        // If the new split state is 1, a child, set the appropriate values
        if (split == 1) {
            Log.d(TAG, "alterSplitTask: Setting up child")
            var i: Int = -1
            var j: Long = -1L

            instance.use {
                i = update("TaskSplit", "chargeno" to parentID, "percentage" to percentage)
                        .whereArgs("task = $rowID").exec()
            }
            Log.d(TAG, "Setting child task $rowID details returned: $i")
            if (i == 0) {
                instance.use {
                    j = insert("TaskSplit", "chargeno" to parentID, "percentage" to percentage,
                            "task" to rowID)
                }
                Log.d(TAG, "Inserting child task $rowID details returned: $j")
            }

            instance.use {
                i = update("Tasks", "split" to 1).whereArgs("_id = $rowID").exec()
            }
            Log.d(TAG, "Converting task $rowID to child task returned: $i")
        }

        // Delete the record in tasksplit if the new split state is 0
        if (currentSplit == 1 && split == 0) {
            var i: Int = -1
            Log.d(TAG, "alterSplitTask: Tearing down child")
            try {
                instance.use {
                    i = delete("TaskSplit", "task = $rowID")
                }
                Log.d(TAG, "Setting child task $rowID details returned: $i")
            } catch (e: RuntimeException) {
                Log.e(TAG, e.localizedMessage)
            }

            val newData = ContentValues(1)
            newData.put("split", 0)
            i = -1
            try {
                instance.use {
                    i = update("Tasks", "split" to 0)
                            .whereArgs("_id = $rowID").exec()
                }
                Log.d(TAG, "Converting child task $rowID to standard task returned: $i")
            } catch (e: RuntimeException) {
                Log.e(TAG, e.localizedMessage)
            }
        }
    }

    /**
     * Deactivate / retire the task supplied.
     *
     * @param taskID The ID of the task to be deactivated.
     */
    override fun deactivateTask(taskID: Long) {
        Log.d(TAG, "deactivateTask: Issuing DB query.")
        instance.use {
            update("Tasks", "active" to DB_FALSE).whereArgs("_id = $taskID").exec()
        }
    }

    /**
     * Activate the task supplied.
     *
     * @param taskID The ID of the task to be activated.
     */
    override fun activateTask(taskID: Long) {
        Log.d(TAG, "activateTask: Issuing DB query.")

        instance.use {
            update("Tasks", "active" to DB_TRUE).whereArgs("_id = $taskID").exec()
        }
    }

    /**
     * Increment the usage counter of the supplied task.
     *
     * @param taskID The ID of the task's usage to be incremented.
     */
    override fun incrementTaskUsage(taskID: Long) {
        Log.d(TAG, "incrementTaskUsage: Issuing DB query.")

        val usageTuple: ITimeSheetDbAdapter.taskUsageTuple? = getTaskUsageTuple(taskID)
        var usage = usageTuple?.usage ?: 0
        var oldUsage = usageTuple?.oldusage ?: 0
        val lastUsed = usageTuple?.lastused ?: 0

        val now = System.currentTimeMillis()
        val todayCal = GregorianCalendar.getInstance()
        val dateLastUsedCal = GregorianCalendar.getInstance()
        todayCal.timeInMillis = now
        dateLastUsedCal.timeInMillis = lastUsed

        // Roll-over the old usage when transitioning into a new month.
        if (todayCal.get(Calendar.MONTH) != dateLastUsedCal.get(Calendar.MONTH)) {
            oldUsage = usage
            usage = if (usage >= 0) 0 else -1
        }

        instance.use {
            update("Tasks", "lastused" to now, "usage" to usage + 1, "oldusage" to oldUsage)
                    .whereArgs("_id = $taskID").exec()
        }
    }

    override fun getTaskUsageTuple(taskID: Long): ITimeSheetDbAdapter.taskUsageTuple? {
        var usageTuple: ITimeSheetDbAdapter.taskUsageTuple? = null
        instance.use {
            usageTuple = select("Tasks", "_id", "usage", "oldusage", "lastused")
                    .whereArgs("_id = $taskID").parseOpt(taskUsageParser)
        }
        return usageTuple
    }

    // TODO: Is there a better way of doing this?
    override fun getTasksList(): Array<String> {
        Log.d(TAG, "getTasksList")
        val taskArray = fetchAllTaskEntries()
        val items = mutableListOf<String>()

        taskArray?.forEach {
            items.add(it.task)
        }

        return items.toTypedArray()
    }

    // TODO: Is there a better way of doing this?
    override fun getDayReportList(): Array<String?> {
        Log.d(TAG, "getDayReportList")
        val reportCursor = daySummary()
        try {
            val items = arrayOfNulls<String?>(reportCursor!!.count)
            reportCursor.moveToFirst()
            var i = 0
            while (reportCursor.isAfterLast) {
                items[i] = reportCursor.getString(reportCursor.getColumnIndex("task"))
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
     *
     * @return Cursor positioned to matching note, if found
     *
     * @throws SQLException if note could not be found/retrieved
     */
    @Throws(SQLException::class)
    override fun fetchVersion(): Int {
        Log.d(TAG, "fetchVersion: Issuing DB query.")

        var response = -1
        instance.use {
            response = select("TimeSheetMeta", "max(version)").parseSingle(IntParser)
        }
        return response
    }

    /**
     * Generic SQL exec wrapper, for use with statements which do not return
     * values.
     */
    override fun runSQL(sqlTorun: String) {
        instance.use {
            execSQL(sqlTorun)
        }
    }

    /**
     * Generic SQL update wrapper, Exposes the update method for testing.
     *
     * @param table       the table being updated
     *
     * @param values      the ContentValues being updated
     *
     * @param whereClause clause to limit updates
     *
     * @param whereArgs   arguments to fill any ? in the whereClause.
     *
     * @return The number of rows affected
     */
    override fun runUpdate(table: String, values: ContentValues,
                           whereClause: String, whereArgs: Array<String>): Int {
        Log.d(TAG, "Running update on '$table'...")

        var retval: Int = -1
        instance.use {
            retval = update(table, values, whereClause, whereArgs)
        }
        return retval
    }

    /**
     * Generic SQL insert wrapper, Exposes the insert method for testing.
     *
     * @param table       the table being updated
     *
     * @param nullColHack Null column hack.
     *
     * @param values      the ContentValues being updated
     *
     * @return The rowID is the just-inserted row
     */
    override fun runInsert(table: String, nullColHack: String, values: ContentValues): Long {
        Log.d(TAG, "Running update on '$table'...")

        var retval: Long = -1L
        instance.use {
            retval = insert(table, nullColHack, values)
        }
        return retval
    }

    /**
     * Dumps the contents of the tasks table to logcat, for testing.
     */
    override fun dumpTasks() {
        Log.d(TAG, "Dumping tasks table")

        data class taskDumpTuple(val id: Long, val task: String)

        val taskDumpParser = rowParser { id: Long, task: String ->
            taskDumpTuple(id, task)
        }

        var taskDump: List<taskDumpTuple>? = null
        instance.use {
            taskDump = select("Tasks", "_id", "task")
                    .parseList(taskDumpParser)
        }
        taskDump?.forEach {
            Log.d(TAG, "$it.id / $it.task")
        }
    }

    /**
     * Dumps the contents of the tasks table to logcat, for testing.
     */
    override fun dumpClockings() {
        Log.d(TAG, "Dumping clock table")

        data class clockingDumpTuple(val id: Long, val chargeno: Long)

        val clockingDumpParser = rowParser { id: Long, chargeno: Long ->
            clockingDumpTuple(id, chargeno)
        }

        var clockingDump: List<clockingDumpTuple>? = null
        instance.use {
            clockingDump = select("Tasks", "_id", "chargeno")
                    .parseList(clockingDumpParser)
        }
        clockingDump?.forEach {
            Log.d(TAG, "$it.id / $it.task")
        }
    }
}
