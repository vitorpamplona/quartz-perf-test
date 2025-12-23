package com.vitorpamplona.quartzPerfTest

import android.app.Application
import com.vitorpamplona.quartz.nip01Core.store.sqlite.EventStore
import com.vitorpamplona.quartzPerfTest.Importer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class App: Application() {
    val dbName = "events.db"

    lateinit var db: EventStore
    lateinit var importer: Importer
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this

        this.deleteDatabase(dbName)
        db = EventStore(this, dbName)
        importer = Importer(this, scope)
    }

    fun dbSize(): Long {
        val file = getDatabasePath(dbName)
        val file2 = getDatabasePath("$dbName-wal")
        return (file.length() + file2.length()) / (1024 * 1014)
    }

    fun usedMemoryMB(): Long {
        val totalMemory = Runtime.getRuntime().totalMemory()
        val freeMemory = Runtime.getRuntime().freeMemory()
        val usedMemory = totalMemory - freeMemory
        return usedMemory / (1024 * 1024)
    }

    override fun onTerminate() {
        super.onTerminate()
        db.close()
        scope.cancel()
    }

    companion object {
        lateinit var instance: App
            private set
    }
}