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
            ${GameEntry.COLUMN_NOTES} TEXT
        )
    """

    const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${GameEntry.TABLE_NAME}"
}