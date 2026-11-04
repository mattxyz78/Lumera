"""Exercise the additive upgrade SQL against SQLite with existing user rows."""
from pathlib import Path
import re
import sqlite3
import unittest


class GenreMigrationTests(unittest.TestCase):
    def test_existing_layout_and_watchlist_survive(self):
        source = (Path(__file__).resolve().parents[2] / 'app/src/main/java/com/lumera/app/di/DatabaseModule.kt').read_text()
        migration = re.search(r'MIGRATION_43_44 =.*?db.execSQL\("([^"]+)"\)', source, re.S).group(1)
        with sqlite3.connect(':memory:') as db:
            db.execute('CREATE TABLE catalog_configs (uniqueId TEXT PRIMARY KEY, customTitle TEXT, homeOrder INTEGER)')
            db.execute("INSERT INTO catalog_configs VALUES ('original', 'My films', 2)")
            db.execute('CREATE TABLE watchlist (id TEXT PRIMARY KEY)')
            db.execute("INSERT INTO watchlist VALUES ('saved-film')")
            db.execute(migration)
            self.assertEqual(('original', 'My films', 2, None), db.execute('SELECT * FROM catalog_configs').fetchone())
            self.assertEqual(('saved-film',), db.execute('SELECT * FROM watchlist').fetchone())
            db.execute("INSERT INTO catalog_configs VALUES ('action', 'Action', 3, 'Action')")
            db.execute("INSERT INTO catalog_configs VALUES ('crime', 'Crime', 4, 'Crime')")
            self.assertEqual(3, db.execute('SELECT COUNT(*) FROM catalog_configs').fetchone()[0])
