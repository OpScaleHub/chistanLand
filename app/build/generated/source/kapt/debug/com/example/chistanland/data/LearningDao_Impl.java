package com.example.chistanland.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@SuppressWarnings({"unchecked", "deprecation"})
public final class LearningDao_Impl implements LearningDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LearningItem> __insertionAdapterOfLearningItem;

  private final EntityDeletionOrUpdateAdapter<LearningItem> __updateAdapterOfLearningItem;

  public LearningDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLearningItem = new EntityInsertionAdapter<LearningItem>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `learning_items` (`id`,`character`,`word`,`phonetic`,`imageUrl`,`level`,`lastReviewTime`,`nextReviewTime`,`isMastered`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LearningItem entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
        if (entity.getCharacter() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getCharacter());
        }
        if (entity.getWord() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getWord());
        }
        if (entity.getPhonetic() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getPhonetic());
        }
        if (entity.getImageUrl() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getImageUrl());
        }
        statement.bindLong(6, entity.getLevel());
        statement.bindLong(7, entity.getLastReviewTime());
        statement.bindLong(8, entity.getNextReviewTime());
        final int _tmp = entity.isMastered() ? 1 : 0;
        statement.bindLong(9, _tmp);
      }
    };
    this.__updateAdapterOfLearningItem = new EntityDeletionOrUpdateAdapter<LearningItem>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `learning_items` SET `id` = ?,`character` = ?,`word` = ?,`phonetic` = ?,`imageUrl` = ?,`level` = ?,`lastReviewTime` = ?,`nextReviewTime` = ?,`isMastered` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LearningItem entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
        if (entity.getCharacter() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getCharacter());
        }
        if (entity.getWord() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getWord());
        }
        if (entity.getPhonetic() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getPhonetic());
        }
        if (entity.getImageUrl() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getImageUrl());
        }
        statement.bindLong(6, entity.getLevel());
        statement.bindLong(7, entity.getLastReviewTime());
        statement.bindLong(8, entity.getNextReviewTime());
        final int _tmp = entity.isMastered() ? 1 : 0;
        statement.bindLong(9, _tmp);
        if (entity.getId() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getId());
        }
      }
    };
  }

  @Override
  public Object insertItems(final List<LearningItem> items,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLearningItem.insert(items);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateItem(final LearningItem item, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfLearningItem.handle(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<LearningItem>> getAllItems() {
    final String _sql = "SELECT * FROM learning_items";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"learning_items"}, new Callable<List<LearningItem>>() {
      @Override
      @NonNull
      public List<LearningItem> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCharacter = CursorUtil.getColumnIndexOrThrow(_cursor, "character");
          final int _cursorIndexOfWord = CursorUtil.getColumnIndexOrThrow(_cursor, "word");
          final int _cursorIndexOfPhonetic = CursorUtil.getColumnIndexOrThrow(_cursor, "phonetic");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "level");
          final int _cursorIndexOfLastReviewTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastReviewTime");
          final int _cursorIndexOfNextReviewTime = CursorUtil.getColumnIndexOrThrow(_cursor, "nextReviewTime");
          final int _cursorIndexOfIsMastered = CursorUtil.getColumnIndexOrThrow(_cursor, "isMastered");
          final List<LearningItem> _result = new ArrayList<LearningItem>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LearningItem _item;
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpCharacter;
            if (_cursor.isNull(_cursorIndexOfCharacter)) {
              _tmpCharacter = null;
            } else {
              _tmpCharacter = _cursor.getString(_cursorIndexOfCharacter);
            }
            final String _tmpWord;
            if (_cursor.isNull(_cursorIndexOfWord)) {
              _tmpWord = null;
            } else {
              _tmpWord = _cursor.getString(_cursorIndexOfWord);
            }
            final String _tmpPhonetic;
            if (_cursor.isNull(_cursorIndexOfPhonetic)) {
              _tmpPhonetic = null;
            } else {
              _tmpPhonetic = _cursor.getString(_cursorIndexOfPhonetic);
            }
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final int _tmpLevel;
            _tmpLevel = _cursor.getInt(_cursorIndexOfLevel);
            final long _tmpLastReviewTime;
            _tmpLastReviewTime = _cursor.getLong(_cursorIndexOfLastReviewTime);
            final long _tmpNextReviewTime;
            _tmpNextReviewTime = _cursor.getLong(_cursorIndexOfNextReviewTime);
            final boolean _tmpIsMastered;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsMastered);
            _tmpIsMastered = _tmp != 0;
            _item = new LearningItem(_tmpId,_tmpCharacter,_tmpWord,_tmpPhonetic,_tmpImageUrl,_tmpLevel,_tmpLastReviewTime,_tmpNextReviewTime,_tmpIsMastered);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getItemById(final String id, final Continuation<? super LearningItem> $completion) {
    final String _sql = "SELECT * FROM learning_items WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (id == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, id);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<LearningItem>() {
      @Override
      @Nullable
      public LearningItem call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCharacter = CursorUtil.getColumnIndexOrThrow(_cursor, "character");
          final int _cursorIndexOfWord = CursorUtil.getColumnIndexOrThrow(_cursor, "word");
          final int _cursorIndexOfPhonetic = CursorUtil.getColumnIndexOrThrow(_cursor, "phonetic");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "level");
          final int _cursorIndexOfLastReviewTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastReviewTime");
          final int _cursorIndexOfNextReviewTime = CursorUtil.getColumnIndexOrThrow(_cursor, "nextReviewTime");
          final int _cursorIndexOfIsMastered = CursorUtil.getColumnIndexOrThrow(_cursor, "isMastered");
          final LearningItem _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpCharacter;
            if (_cursor.isNull(_cursorIndexOfCharacter)) {
              _tmpCharacter = null;
            } else {
              _tmpCharacter = _cursor.getString(_cursorIndexOfCharacter);
            }
            final String _tmpWord;
            if (_cursor.isNull(_cursorIndexOfWord)) {
              _tmpWord = null;
            } else {
              _tmpWord = _cursor.getString(_cursorIndexOfWord);
            }
            final String _tmpPhonetic;
            if (_cursor.isNull(_cursorIndexOfPhonetic)) {
              _tmpPhonetic = null;
            } else {
              _tmpPhonetic = _cursor.getString(_cursorIndexOfPhonetic);
            }
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final int _tmpLevel;
            _tmpLevel = _cursor.getInt(_cursorIndexOfLevel);
            final long _tmpLastReviewTime;
            _tmpLastReviewTime = _cursor.getLong(_cursorIndexOfLastReviewTime);
            final long _tmpNextReviewTime;
            _tmpNextReviewTime = _cursor.getLong(_cursorIndexOfNextReviewTime);
            final boolean _tmpIsMastered;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsMastered);
            _tmpIsMastered = _tmp != 0;
            _result = new LearningItem(_tmpId,_tmpCharacter,_tmpWord,_tmpPhonetic,_tmpImageUrl,_tmpLevel,_tmpLastReviewTime,_tmpNextReviewTime,_tmpIsMastered);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<LearningItem>> getItemsToReview(final long currentTime) {
    final String _sql = "SELECT * FROM learning_items WHERE nextReviewTime <= ? OR level = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, currentTime);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"learning_items"}, new Callable<List<LearningItem>>() {
      @Override
      @NonNull
      public List<LearningItem> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCharacter = CursorUtil.getColumnIndexOrThrow(_cursor, "character");
          final int _cursorIndexOfWord = CursorUtil.getColumnIndexOrThrow(_cursor, "word");
          final int _cursorIndexOfPhonetic = CursorUtil.getColumnIndexOrThrow(_cursor, "phonetic");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "level");
          final int _cursorIndexOfLastReviewTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastReviewTime");
          final int _cursorIndexOfNextReviewTime = CursorUtil.getColumnIndexOrThrow(_cursor, "nextReviewTime");
          final int _cursorIndexOfIsMastered = CursorUtil.getColumnIndexOrThrow(_cursor, "isMastered");
          final List<LearningItem> _result = new ArrayList<LearningItem>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LearningItem _item;
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpCharacter;
            if (_cursor.isNull(_cursorIndexOfCharacter)) {
              _tmpCharacter = null;
            } else {
              _tmpCharacter = _cursor.getString(_cursorIndexOfCharacter);
            }
            final String _tmpWord;
            if (_cursor.isNull(_cursorIndexOfWord)) {
              _tmpWord = null;
            } else {
              _tmpWord = _cursor.getString(_cursorIndexOfWord);
            }
            final String _tmpPhonetic;
            if (_cursor.isNull(_cursorIndexOfPhonetic)) {
              _tmpPhonetic = null;
            } else {
              _tmpPhonetic = _cursor.getString(_cursorIndexOfPhonetic);
            }
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final int _tmpLevel;
            _tmpLevel = _cursor.getInt(_cursorIndexOfLevel);
            final long _tmpLastReviewTime;
            _tmpLastReviewTime = _cursor.getLong(_cursorIndexOfLastReviewTime);
            final long _tmpNextReviewTime;
            _tmpNextReviewTime = _cursor.getLong(_cursorIndexOfNextReviewTime);
            final boolean _tmpIsMastered;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsMastered);
            _tmpIsMastered = _tmp != 0;
            _item = new LearningItem(_tmpId,_tmpCharacter,_tmpWord,_tmpPhonetic,_tmpImageUrl,_tmpLevel,_tmpLastReviewTime,_tmpNextReviewTime,_tmpIsMastered);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
