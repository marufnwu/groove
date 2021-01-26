package com.groove.megaapp.data.dbs;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.groove.megaapp.data.daos.DraftDao;
import com.groove.megaapp.data.entities.Draft;

@Database(entities = {Draft.class}, version = 2, exportSchema = false)
public abstract class ClientDatabase extends RoomDatabase {

    public abstract DraftDao drafts();
}
