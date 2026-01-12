package com.example.scorifynative.database

import android.provider.BaseColumns

object GameContract {

    object GameEntry : BaseColumns {
        const val TABLE_NAME = "games"
        const val COLUMN_ID = BaseColumns._ID
        const val COLUMN_HOME_TEAM = "home_team"
        const val COLUMN_AWAY_TEAM = "away_team"
        const val COLUMN_HOME_SCORE = "home_score"
        const val COLUMN_AWAY_SCORE = "away_score"
        const val COLUMN_DATE = "date"
        const val COLUMN_LOCATION = "location"
        const val COLUMN_SPORT_TYPE = "sport_type"
        const val COLUMN_STATUS = "status"
        const val COLUMN_NOTES = "notes"

        // New columns for server synchronization
        const val COLUMN_SERVER_ID = "server_id"           // ID from server (null if not synced)
        const val COLUMN_PENDING_SYNC = "pending_sync"     // 1 if waiting to sync, 0 if synced
        const val COLUMN_SYNC_OPERATION = "sync_operation" // "CREATE", "UPDATE", "DELETE", or null
    }

    const val SQL_CREATE_ENTRIES = """
        CREATE TABLE ${GameEntry.TABLE_NAME} (
            ${GameEntry.COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
            ${GameEntry.COLUMN_HOME_TEAM} TEXT NOT NULL,
            ${GameEntry.COLUMN_AWAY_TEAM} TEXT NOT NULL,
            ${GameEntry.COLUMN_HOME_SCORE} INTEGER NOT NULL DEFAULT 0,
            ${GameEntry.COLUMN_AWAY_SCORE} INTEGER NOT NULL DEFAULT 0,
            ${GameEntry.COLUMN_DATE} INTEGER NOT NULL,
            ${GameEntry.COLUMN_LOCATION} TEXT NOT NULL,
            ${GameEntry.COLUMN_SPORT_TYPE} TEXT NOT NULL,
            ${GameEntry.COLUMN_STATUS} TEXT NOT NULL,
            ${GameEntry.COLUMN_NOTES} TEXT,
            ${GameEntry.COLUMN_SERVER_ID} INTEGER,
            ${GameEntry.COLUMN_PENDING_SYNC} INTEGER NOT NULL DEFAULT 0,
            ${GameEntry.COLUMN_SYNC_OPERATION} TEXT
        )
    """

    const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${GameEntry.TABLE_NAME}"

    // Migration SQL for upgrading from version 1 to version 2
    const val SQL_ALTER_ADD_SERVER_ID =
        "ALTER TABLE ${GameEntry.TABLE_NAME} ADD COLUMN ${GameEntry.COLUMN_SERVER_ID} INTEGER"

    const val SQL_ALTER_ADD_PENDING_SYNC =
        "ALTER TABLE ${GameEntry.TABLE_NAME} ADD COLUMN ${GameEntry.COLUMN_PENDING_SYNC} INTEGER NOT NULL DEFAULT 0"

    const val SQL_ALTER_ADD_SYNC_OPERATION =
        "ALTER TABLE ${GameEntry.TABLE_NAME} ADD COLUMN ${GameEntry.COLUMN_SYNC_OPERATION} TEXT"
}