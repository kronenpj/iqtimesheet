package com.github.kronenpj.iqtimesheet.IQTimeSheet

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import org.jetbrains.anko.db.*

/**
 * Created by kronenpj on 6/21/17.
 */
class MySqlHelper(ctx: Context) : ManagedSQLiteOpenHelper(ctx, "TimeSheetDB.db") {

    companion object {
        private var instance: MySqlHelper? = null
        private val DATABASE_VERSION = 3
        private val TAG = "MySqlHelper"

        @Synchronized
        fun getInstance(ctx: Context): MySqlHelper {
            if (instance == null) {
                instance = MySqlHelper(ctx.applicationContext)
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.createTable("Tasks", true,
                "_id" to INTEGER + PRIMARY_KEY + AUTOINCREMENT,
                "task" to TEXT + NOT_NULL,
                // Active should be a BOOLEAN, default to TRUE
                "active" to INTEGER + NOT_NULL + DEFAULT("1"),
                "usage" to INTEGER + NOT_NULL + DEFAULT("0"),
                "oldusage" to INTEGER + NOT_NULL + DEFAULT("0"),
                "lastused" to INTEGER + NOT_NULL + DEFAULT("0"),
                "split" to INTEGER + DEFAULT("0")
        )

        db.createTable("TimeSheet", true,
                "_id" to INTEGER + PRIMARY_KEY + AUTOINCREMENT,
                "chargeno" to INTEGER + NOT_NULL,
                "timein" to INTEGER + NOT_NULL + DEFAULT("0"),
                "timeout" to INTEGER + NOT_NULL + DEFAULT("0"),
                FOREIGN_KEY("chargeno", "Tasks", "_id")
        )

        db.createTable("TaskSplit", true,
                "_id" to INTEGER + PRIMARY_KEY + AUTOINCREMENT,
                "chargeno" to INTEGER + NOT_NULL,
                "task" to INTEGER + NOT_NULL,
                "percentage" to REAL + NOT_NULL + DEFAULT("100"),
                FOREIGN_KEY("chargeno", "Tasks", "_id"),
                FOREIGN_KEY("task", "Tasks", "_id")
                // Need bounds check here, preferably.
                //"percentage" to REAL + NOT_NULL + DEFAULT("100") +
                // CHECK ("percentage" >=0 AND "percentage" <= 100
        )

        /*
        db.execSQL("""ALTER TABLE TaskSplit
                ADD 'percent' REAL NOT NULL DEFAULT 100 CONSTRAINT CHECK
                ("percentage" >= 0 AND 'percentage' <= 100)"""
        )
        */

        db.execSQL("""CREATE VIEW EntryItems AS
                SELECT TimeSheet._id AS _id,
                Tasks.task AS task,
                TimeSheet.timein AS timein,
                TimeSheet.timeout AS timeout
                FROM TimeSheet, Tasks, WHERE TimeSheet.chargeno = Tasks._id"""
        )

        db.execSQL("""CREATE VIEW TaskSplitReport AS
                SELECT Tasks._id AS _id,
                TaskSplit.chargeno AS parenttask,
                Tasks.task AS taskdesc,
                TaskSplit.percentage AS percentage
                FROM Tasks, TaskSplit WHERE Tasks._id = TaskSplit.task"""
        )

        db.execSQL("""CREATE VIEW EntryReport AS SELECT 
                TimeSheet._id AS _id, 
                Tasks.task AS task 
                TimeSheet.timein AS timein 
                TimeSheet.timeout AS timeout 
                strftime("%H:%M", timein/1000,"unixepoch","localtime")
                || ' to ' || CASE WHEN timeout = 0 THEN 'now' ELSE
                strftime("%H:%M", timeout/1000,"unixepoch","localtime") END as range
                FROM TimeSheet, Tasks, WHERE 
                TimeSheet.chargeno = Tasks._id"""
        )

        db.execSQL("""CREATE UNIQUE INDEX Tasks_index ON "Tasks" ("task")""")
        db.execSQL("""CREATE UNIQUE INDEX TimeSheet_chargeno_index ON "TimeSheet" ("chargeno")""")
        db.execSQL("""CREATE UNIQUE INDEX TaskSplit_chargeno_index ON "TaskSplit" ("chargeno")""")
        db.execSQL("""CREATE UNIQUE INDEX TimeSheet_timein_index ON "TimeSheet" ("timein")""")
        db.execSQL("""CREATE UNIQUE INDEX TimeSheet_timeout_index ON "TimeSheet" ("timeout")""")
        db.createTable("TimeSheetMeta", true, "version" to INTEGER + PRIMARY_KEY)

        db.insert("TimeSheetMeta", "version" to DATABASE_VERSION)
        db.insert("Tasks",
                "task" to "Example task entry",
                "lastused" to System.currentTimeMillis()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                + newVersion + ".")
        // db.execSQL("ALTER TABLE TimeSheet ADD...");
        // db.execSQL("UPDATE TimeSheet SET ");
        when (oldVersion) {
            1 -> {
                Log.d(TAG, "Old DB version <= 1")
                Log.d(TAG, "Running: CHARGENO_INDEX")
                db.execSQL("""CREATE UNIQUE INDEX TimeSheet_chargeno_index ON "TimeSheet" ("chargeno")""")
                db.execSQL("""CREATE UNIQUE INDEX Tasks_index ON "Tasks" ("task")""")
                Log.d(TAG, "Running: TIMEIN_INDEX")
                db.execSQL("""CREATE UNIQUE INDEX TimeSheet_timein_index ON "TimeSheet" ("timein")""")
                Log.d(TAG, "Running: TIMEOUT_INDEX")
                db.execSQL("""CREATE UNIQUE INDEX TimeSheet_timeout_index ON "TimeSheet" ("timeout")""")
                Log.d(TAG, "Old DB version <= 2")
                Log.d(TAG, "Running: TASK_TABLE_ALTER3")
                db.execSQL("""ALTER TABLE "Tasks" ADD COLUMN "split" INTEGER DEFAULT 0""")
                Log.d(TAG, "Running: TASKSPLIT_TABLE_CREATE")
                db.createTable("TaskSplit", true,
                        "_id" to INTEGER + PRIMARY_KEY + AUTOINCREMENT,
                        "chargeno" to INTEGER + NOT_NULL,
                        "task" to INTEGER + NOT_NULL,
                        "percentage" to REAL + NOT_NULL + DEFAULT("100"),
                        FOREIGN_KEY("chargeno", "Tasks", "_id"),
                        FOREIGN_KEY("task", "Tasks", "_id")
                        // Need bounds check here, preferably.
                        //"percentage" to REAL + NOT_NULL + DEFAULT("100") +
                        // CHECK ("percentage" >=0 AND "percentage" <= 100
                )
                Log.d(TAG, "Running: TASKSPLITREPORT_VIEW_CREATE")
                db.execSQL("""CREATE VIEW TaskSplitReport AS
                    SELECT Tasks._id AS _id,
                    TaskSplit.chargeno AS parenttask,
                    Tasks.task AS taskdesc,
                    TaskSplit.percentage AS percentage
                    FROM Tasks, TaskSplit WHERE Tasks._id = TaskSplit.task"""
                )
                Log.d(TAG, "Running: SPLIT_INDEX")
                db.execSQL("""CREATE UNIQUE INDEX TaskSplit_chargeno_index ON "TaskSplit" ("chargeno")""")
                if (newVersion != oldVersion)
                    db.update("TimeSheetMetadata", "version" to newVersion)
            }
            2 -> {
                Log.d(TAG, "Old DB version <= 2")
                Log.d(TAG, "Running: TASK_TABLE_ALTER3")
                db.execSQL("""ALTER TABLE "Tasks" ADD COLUMN "split" INTEGER DEFAULT 0""")
                Log.d(TAG, "Running: TASKSPLIT_TABLE_CREATE")
                db.createTable("TaskSplit", true,
                        "_id" to INTEGER + PRIMARY_KEY + AUTOINCREMENT,
                        "chargeno" to INTEGER + NOT_NULL,
                        "task" to INTEGER + NOT_NULL,
                        "percentage" to REAL + NOT_NULL + DEFAULT("100"),
                        FOREIGN_KEY("chargeno", "Tasks", "_id"),
                        FOREIGN_KEY("task", "Tasks", "_id")
                        // Need bounds check here, preferably.
                        //"percentage" to REAL + NOT_NULL + DEFAULT("100") +
                        // CHECK ("percentage" >=0 AND "percentage" <= 100
                )
                Log.d(TAG, "Running: TASKSPLITREPORT_VIEW_CREATE")
                db.execSQL("""CREATE VIEW TaskSplitReport AS
                    SELECT Tasks._id AS _id,
                    TaskSplit.chargeno AS parenttask,
                    Tasks.task AS taskdesc,
                    TaskSplit.percentage AS percentage
                    FROM Tasks, TaskSplit WHERE Tasks._id = TaskSplit.task"""
                )
                Log.d(TAG, "Running: SPLIT_INDEX")
                db.execSQL("""CREATE UNIQUE INDEX TaskSplit_chargeno_index ON "TaskSplit" ("chargeno")""")
                if (newVersion != oldVersion)
                    db.update("TimeSheetMetadata", "version" to newVersion)
            }
            else -> if (newVersion != oldVersion)
                db.update("TimeSheetMetadata", "version" to newVersion)
        }
    }
}

// Access property for Context
val Context.database: MySqlHelper
    get() = MySqlHelper.getInstance(applicationContext)
