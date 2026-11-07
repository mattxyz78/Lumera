from pathlib import Path
import re
import sqlite3
import unittest

class ProfileAgeMigrationTests(unittest.TestCase):
    def test_age_upgrade_preserves_existing_profiles(self):
        root = Path(__file__).resolve().parents[2]
        source = (root / 'app/src/main/java/com/lumera/app/di/DatabaseModule.kt').read_text()
        sql = re.search(r'MIGRATION_44_45 =.*?db.execSQL\("([^"]+)"\)', source, re.S).group(1)
        with sqlite3.connect(':memory:') as db:
            db.execute('CREATE TABLE profiles (id INTEGER PRIMARY KEY, name TEXT NOT NULL, themeId TEXT NOT NULL)')
            db.execute("INSERT INTO profiles VALUES (1, 'Existing', 'mattflix')")
            db.execute(sql)
            self.assertEqual((1, 'Existing', 'mattflix', 'adult'), db.execute('SELECT * FROM profiles').fetchone())
            db.execute("UPDATE profiles SET ageGroup='under12' WHERE id=1")
            self.assertEqual('under12', db.execute('SELECT ageGroup FROM profiles').fetchone()[0])

    def test_optional_pin_upgrade_keeps_child_age_and_saved_layout(self):
        root = Path(__file__).resolve().parents[2]
        source = (root / 'app/src/main/java/com/lumera/app/di/DatabaseModule.kt').read_text()
        sql = re.search(r'MIGRATION_45_46 =.*?db.execSQL\("([^"]+)"\)', source, re.S).group(1)
        with sqlite3.connect(':memory:') as db:
            db.execute('CREATE TABLE profiles (id INTEGER PRIMARY KEY, name TEXT NOT NULL, ageGroup TEXT NOT NULL, homeLayout TEXT NOT NULL)')
            db.execute("INSERT INTO profiles VALUES (1, 'Child', 'under12', 'saved-genres')")
            db.execute("INSERT INTO profiles VALUES (2, 'Adult', 'adult', 'saved-layout')")
            db.execute(sql)
            self.assertEqual([(1, 'Child', 'under12', 'saved-genres', 0), (2, 'Adult', 'adult', 'saved-layout', 0)], db.execute('SELECT * FROM profiles ORDER BY id').fetchall())
            db.execute('UPDATE profiles SET pinProtected=1 WHERE id=2')
            self.assertEqual([('under12', 0), ('adult', 1)], db.execute('SELECT ageGroup,pinProtected FROM profiles ORDER BY id').fetchall())
