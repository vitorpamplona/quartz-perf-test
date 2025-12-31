package com.vitorpamplona.quartzPerfTest

import android.app.Application
import com.vitorpamplona.quartz.nip01Core.store.sqlite.EventStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class App: Application() {
    val dbName = "events.db"

    lateinit var db: EventStore
    lateinit var importer: Importer
    lateinit var queryTester: QueryTester

    override fun onCreate() {
        super.onCreate()
        instance = this

        this.deleteDatabase(dbName)
        db = EventStore(this, dbName)
        importer = Importer(db, this)
        queryTester = QueryTester(db)
    }

    fun dbSizeMB(): Int {
        val f1 = getDatabasePath(dbName)
        val f2 = getDatabasePath("$dbName-wal")
        val total = f1.length() + f2.length()
        return (total / MB).toInt()
    }

    fun usedMemoryMB(): Int {
        val runtime = Runtime.getRuntime()
        val total = runtime.totalMemory()
        val free = runtime.freeMemory()
        val used = total - free
        return (used / MB).toInt()
    }

    override fun onTerminate() {
        super.onTerminate()
        db.close()
    }

    companion object {
        lateinit var instance: App
            private set
    }
}