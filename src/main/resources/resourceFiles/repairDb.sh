mv home_history_weekly.db home_history_weekly_backup.db
sqlite3 home_history_weekly_backup.db ".dump" | sqlite3 home_history_weekly.db
