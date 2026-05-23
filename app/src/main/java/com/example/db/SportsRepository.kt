package com.example.db

import com.example.model.Favorite
import com.example.model.NotificationLog
import kotlinx.coroutines.flow.Flow

class SportsRepository(private val database: SportsDatabase) {
    val favorites: Flow<List<Favorite>> = database.favoriteDao.getAllFavorites()
    val notificationLogs: Flow<List<NotificationLog>> = database.notificationLogDao.getAllLogs()

    suspend fun addFavorite(id: String, name: String, type: String) {
        val fav = Favorite(id, name, type)
        database.favoriteDao.insertFavorite(fav)
    }

    suspend fun removeFavorite(id: String) {
        database.favoriteDao.deleteFavoriteById(id)
    }

    suspend fun logNotification(matchId: String, home: String, away: String, type: String, min: Int, desc: String, score: String) {
        val log = NotificationLog(
            matchId = matchId,
            homeTeam = home,
            awayTeam = away,
            eventType = type,
            minute = min,
            description = desc,
            score = score
        )
        database.notificationLogDao.insertLog(log)
    }

    suspend fun clearLogs() {
        database.notificationLogDao.clearAllLogs()
    }

    fun isFavorite(id: String): Flow<Favorite?> {
        return database.favoriteDao.getFavoriteById(id)
    }
}
