package com.github.kronenpj.iqtimesheet.IQTimeSheet

import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException
import com.github.kronenpj.iqtimesheet.TimeHelpers

interface ITimeSheetDbAdapter {
    data class timeTotalTuple(val id: Long, val task: String, val total: Float)
    data class timeEntryTuple(val id: Long, val task: String, val timein: Long, val timeout: Long)
    data class chargeNoTuple(val id: Long, val chargeno: Long, val timein: Long, val timeout: Long)
    // TODO: active should be: Boolean
    data class tasksTuple(val id: Long, val task: String, val active: Long, val usage: Long,
                          val oldusage: Long, val lastused: Long)

    data class taskUsageTuple(val id: Long, val usage: Long, val oldusage: Long, val lastused: Long)

    /**
     * Create a new time entry using the charge number provided. If the entry is
     * successfully created return the new rowId for that note, otherwise return
     * a -1 to indicate failure.
     *
     * @param task the charge number for the entry
     *
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
     *
     * @param task   the charge number for the entry
     *
     * @param timeIn the time in milliseconds of the clock-in
     *
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
     *
     * @param chargeno the charge number for the entry
     *
     * @param timeInP  the time in milliseconds of the clock-in
     *
     * @return rowId or -1 if failed
     */
    fun createEntry(chargeno: Long, timeInP: Long = System.currentTimeMillis()): Long

    /**
     * Close last time entry. If the entry is successfully closed, return the
     * rowId for that entry, otherwise return a -1 to indicate failure.
     *
     * @return rowId or -1 if failed
     */
    fun closeEntry(): Int {
        val rowID = lastClockEntry()
        val chargeTuple = getChargeNoTuple(rowID)
        val chargeno = chargeTuple?.chargeno ?: -1

        return closeEntry(chargeno)
    }

    /**
     * Close supplied time entry. If the entry is successfully closed, return
     * the rowId for that entry, otherwise return a -1 to indicate failure.
     *
     * @param task the charge number for the entry
     *
     * @return rowId or -1 if failed
     */
    fun closeEntry(task: String): Int {
        val chargeno = getTaskIDByName(task)
        return closeEntry(chargeno)
    }

    /**
     * Close supplied time entry with the supplied time. If the entry is
     * successfully closed, return the rowId for that entry, otherwise return a
     * -1 to indicate failure.
     *
     * @param task    the charge number for the entry
     *
     * @param timeOut the time in milliseconds of the clock-out
     *
     * @return rowId or -1 if failed
     */
    fun closeEntry(task: String, timeOut: Long): Int {
        val chargeno = getTaskIDByName(task)
        return closeEntry(chargeno, timeOut)
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
    fun closeEntry(chargeno: Long, timeOutP: Long = System.currentTimeMillis()): Int

    /**
     * Delete the entry with the given rowId
     *
     * @param rowId code id of note to delete
     *
     * @return true if deleted, false otherwise
     */
    fun deleteEntry(rowId: Long): Boolean

    /**
     * Return a Cursor over the list of all entries in the database
     *
     * @return Cursor over all database entries
     */
    fun fetchAllTimeEntries(): Cursor?

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
    fun fetchEntry(rowId: Long): Cursor?

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
    fun fetchEntry(rowId: Long, column: String): Cursor?

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
    fun updateEntry(rowIdP: Long, chargeno: Long, date: String?,
                    timein: Long, timeout: Long): Boolean

    /**
     * Retrieve the taskID of the last entry in the clock table.
     *
     * @return rowId or -1 if failed
     */
    fun taskIDForLastClockEntry(): Long

    /**
     * Retrieve the timeIn of the last entry in the clock table.
     *
     * @return time in in milliseconds or -1 if failed
     */
    fun timeInForLastClockEntry(): Long

    /**
     * Retrieve the time out of the last entry in the clock table.
     *
     * @return time out in milliseconds or -1 if failed
     */
    fun timeOutForLastClockEntry(): Long

    /**
     * Retrieve the row of the last entry in the tasks table.
     *
     * @return rowId or -1 if failed
     */
    fun lastTaskEntry(): Long

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
    fun getTimeEntryTuple(rowId: Long): timeEntryTuple?

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
    fun getChargeNoTuple(rowId: Long): chargeNoTuple?

    /**
     * Retrieve the entry in the timesheet table immediately prior to the
     * supplied entry.
     *
     * @return rowId or -1 if failed
     */
    fun getPreviousClocking(rowID: Long): Long

    /**
     * Retrieve the entry in the timesheet table immediately following the
     * supplied entry.
     *
     * @return rowId or -1 if failed
     */
    // TODO: Should this be chronological or ordered by _id? as it is now?
    // And, if it should be chronological by time in or time out or both... :(
    fun getNextClocking(rowID: Long): Long

    /**
     * Retrieve the row of the last entry in the clock table.
     *
     * @return rowId or -1 if failed
     */
    fun lastClockEntry(): Long

    /**
     * Retrieve an array of rows for today's entry in the clock table.
     *
     * @return array of rowId's or null if failed
     */
    fun todaysEntries(): LongArray?

    /**
     * Retrieve list of entries for the day surrounding the supplied time.
     *
     * @return rowId or -1 if failed
     */
    fun getEntryReportCursor(distinct: Boolean, columns: Array<String>,
                             start: Long, end: Long): Cursor? {
        return getEntryReportCursor(distinct, columns, null, null, start, end)
    }

    /**
     * Retrieve list of entries for the day surrounding the supplied time.
     *
     * @return rowId or -1 if failed
     */
    fun getEntryReportCursor(distinct: Boolean, columns: Array<String>,
                             groupBy: String?, orderBy: String?, start: Long, end: Long): Cursor?

    /**
     * Retrieve list of entries for the day surrounding the supplied time.
     *
     * @return rowId or -1 if failed
     */
    fun getSummaryCursor(distinct: Boolean, columns: Array<String>,
                         start: Long, end: Long): Cursor? {
        return getSummaryCursor(distinct, columns, null, null, start, end)
    }

    /**
     * Given a task ID, close the task and, if the ID differs, open a new task.
     * Refresh the data displayed.
     *
     * @return true if a new task was started, false if the old task was
     * stopped.
     */
    fun processChange(taskID: Long): Boolean

    /**
     * Retrieve list of entries for the day surrounding the supplied time.
     *
     * @return rowId or -1 if failed
     */
    fun getSummaryCursor(distinct: Boolean, columns: Array<String>,
                         groupBy: String?, orderBy: String?, start: Long, end: Long): Cursor?

    /**
     * Retrieve list of entries for the day surrounding the supplied time.
     *
     * @return rowId or -1 if failed
     */
    fun dayEntryReport(timeP: Long = TimeHelpers.millisNow()): Cursor?

    /**
     * Method that retrieves the entries for today from the entry view.
     *
     * @param omitOpen Omit an open task in the result
     *
     * @return Cursor over the results.
     */
    fun daySummary(omitOpen: Boolean): Cursor? {
        return daySummary(TimeHelpers.millisNow(), omitOpen)
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
    fun daySummary(timeP: Long = TimeHelpers.millisNow(), omitOpen: Boolean = true): Cursor?

    /**
     * Retrieve list of entries for the week surrounding the supplied time.
     *
     * @return Cursor over the entries
     */
    fun weekEntryReport(timeP: Long = TimeHelpers.millisNow()): Cursor?

    /**
     * Method that populates a temporary table for a single specified day from
     * the entry view.
     *
     * @return Cursor over the results.
     */
    // TODO: Finish and replace the other routines with it.
    fun weekSummary(timeP: Long, omitOpen: Boolean): Cursor?

    /**
     *
     * @param summaryStart The start time for the summary
     *
     * @param summaryEnd   The end time for the summary
     */
    fun populateSummary(summaryStart: Long, summaryEnd: Long) {
        populateSummary(summaryStart, summaryEnd, true)
    }

    /**
     *
     * @param summaryStart The start time for the summary
     * *
     * @param summaryEnd   The end time for the summary
     * *
     * @param omitOpen     Whether the summary should omit an open task
     */
    fun populateSummary(summaryStart: Long, summaryEnd: Long,
                        omitOpen: Boolean)

    /**
     * Create a new time entry using the charge number provided. If the entry is
     * successfully created return the new rowId for that number, otherwise
     * return a -1 to indicate failure.
     *
     * @param task the charge number text for the entry
     *
     * @return rowId or -1 if failed
     */
    fun createTask(task: String): Long

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
    fun createTask(task: String, parent: String, percentage: Int): Long

    /**
     * Return a Cursor over the list of all tasks in the database eligible to be
     * split task parents.
     *
     * @return Cursor over all database entries
     */
    fun fetchParentTasks(): Array<String>?

    /**
     * Return an array over the list of all tasks in the database that are children
     * of the supplied split task parent.
     *
     * @return Array over all matching database entries
     */
    fun fetchChildTasks(parentID: Long): Array<Long>

    /**
     * Return an Array of all Tasks entries in the database
     *
     * @return Array of all Tasks database entries
     */
    fun fetchAllTaskEntries(): Array<tasksTuple>?

    /**
     * Return a Cursor over the list of all entries in the database
     *
     * @return Cursor over all database entries
     */
    fun fetchAllDisabledTasks(): Array<tasksTuple>?

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
    fun fetchTask(rowId: Long): Cursor

    /**
     * Retrieve the task ID of the supplied task name.
     *
     * @return rowId or -1 if failed
     */
    fun getTaskIDByName(name: String): Long

    /**
     * Retrieve the task name for the supplied task ID.
     *
     * @param taskID The identifier of the desired task
     *
     * @return Name of the task identified by the taskID
     */
    fun getTaskNameByID(taskID: Long): String?

    /**
     * Return the entry that matches the given rowId
     *
     * @param splitTask id of task to retrieve
     *
     * @return parent's task ID, if found, 0 if not
     */
    fun getSplitTaskParent(splitTask: String): Long {
        return getSplitTaskParent(getTaskIDByName(splitTask))
    }

    /**
     * Return the entry that matches the given rowId
     *
     * @param rowId id of task to retrieve
     *
     * @return parent's task ID, if found, 0 if not
     */
    fun getSplitTaskParent(rowId: Long): Long

    /**
     * Return the entry that matches the given rowId
     *
     * @param splitTask id of task to retrieve
     *
     * @return parent's task ID, if found, 0 if not
     */
    fun getSplitTaskPercentage(splitTask: String): Int {
        return getSplitTaskPercentage(getTaskIDByName(splitTask))
    }

    /**
     * Return the entry that matches the given rowId
     *
     * @param rowId id of task to retrieve
     *
     * @return parent's task ID, if found, 0 if not
     */
    fun getSplitTaskPercentage(rowId: Long): Int

    /**
     * Return the flag whether the task that matches the given rowId is a split
     * task.
     *
     * @param splitTask name of task to retrieve
     *
     * @return parent's task ID, if found, 0 if not
     */
    fun getSplitTaskFlag(splitTask: String): Int {
        return getSplitTaskFlag(getTaskIDByName(splitTask))
    }

    /**
     * Return the flag whether the task that matches the given rowId is a split
     * task.
     *
     * @param rowId id of task to retrieve
     *
     * @return 1 if the task is part of a split, and if found, 0 otherwise
     */
    fun getSplitTaskFlag(rowId: Long): Int

    /**
     * Return the number of children whose parent matches the given rowId
     *
     * @param rowId id of task to retrieve
     *
     * @return Number of "children" of this task, 0 if none.
     */
    fun getQuantityOfSplits(rowId: Long): Long

    /**
     * Rename specified task
     *
     * @param origName Old task name
     *
     * @param newName  New task name
     */
    fun renameTask(origName: String, newName: String)

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
    fun alterSplitTask(rowID: Long, parentIDP: Long, percentage: Int, split: Int)

    /**
     * Deactivate / retire the task supplied.
     *
     * @param taskName The name of the task to be deactivated.
     */
    fun deactivateTask(taskName: String) {
        val taskID = getTaskIDByName(taskName)
        deactivateTask(taskID)
    }

    /**
     * Deactivate / retire the task supplied.
     *
     * @param taskID The ID of the task to be deactivated.
     */
    fun deactivateTask(taskID: Long)

    /**
     * Activate the task supplied.
     *
     * @param taskName The name of the task to be activated.
     */
    fun activateTask(taskName: String) {
        val taskID = getTaskIDByName(taskName)
        activateTask(taskID)
    }

    /**
     * Activate the task supplied.
     *
     * @param taskID The ID of the task to be activated.
     */
    fun activateTask(taskID: Long)

    /**
     * Increment the usage counter of the supplied task.
     *
     * @param taskID The ID of the task's usage to be incremented.
     */
    fun incrementTaskUsage(taskID: Long)

    fun getTaskUsageTuple(taskID: Long): taskUsageTuple?

    fun getTasksList(): Array<String>

    fun getDayReportList(): Array<String?>

    /**
     * Return a Cursor positioned at the note that matches the given rowId
     *
     * @return Cursor positioned to matching note, if found
     *
     * @throws SQLException if note could not be found/retrieved
     */
    @Throws(SQLException::class)
    fun fetchVersion(): Int

    /**
     * Generic SQL exec wrapper, for use with statements which do not return
     * values.
     */
    fun runSQL(sqlTorun: String)

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
    fun runUpdate(table: String, values: ContentValues,
                  whereClause: String, whereArgs: Array<String>): Int

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
    fun runInsert(table: String, nullColHack: String, values: ContentValues): Long

    /**
     * Dumps the contents of the tasks table to logcat, for testing.
     */
    fun dumpTasks()

    /**
     * Dumps the contents of the tasks table to logcat, for testing.
     */
    fun dumpClockings()

    companion object {
        const val DB_FALSE: String = "0"
        const val DB_TRUE: String = "1"
    }
}