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
