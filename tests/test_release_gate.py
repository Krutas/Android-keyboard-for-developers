import importlib.util
import json
from pathlib import Path
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('publisher', Path(__file__).resolve().parents[1] / 'tools/publish_verified_release.py')
publisher = importlib.util.module_from_spec(spec)
spec.loader.exec_module(publisher)


class ReleaseGateTest(unittest.TestCase):
    def check_blocked(self, dirty='', audit='', runs=(), expected=''):
        calls = []
        def fake_run(*args):
            calls.append(args)
            if args[:3] == ('git', 'status', '--porcelain'): return dirty
            if args[:3] == ('git', 'rev-parse', 'HEAD'): return 'abc123'
            if args[:2] == ('gh', 'api'): return json.dumps({'workflow_runs': list(runs)})
            raise AssertionError(f'Unexpected release mutation: {args}')
        with patch.object(publisher, 'run', fake_run), patch.object(Path, 'read_text', return_value=audit):
            with self.assertRaisesRegex(SystemExit, expected): publisher.main()
        self.assertFalse(any(args[:3] == ('gh', 'release', 'create') for args in calls))

    def test_dirty_checkout(self): self.check_blocked(dirty=' M file', expected='dirty checkout')
    def test_pending_approval(self): self.check_blocked(audit='pending explicit approval', expected='decision remains unresolved')
    def test_missing_workflow(self): self.check_blocked(expected='android.yml has not succeeded')
    def test_latest_failed_run_cannot_use_older_success(self):
        self.check_blocked(runs=[{'id': 1, 'path': '.github/workflows/android.yml', 'status': 'completed', 'conclusion': 'success'},
                                 {'id': 2, 'path': '.github/workflows/android.yml', 'status': 'completed', 'conclusion': 'failure'}], expected='android.yml has not succeeded')
    def test_other_platform_failure_blocks_release(self):
        self.check_blocked(runs=[{'id': 1, 'path': '.github/workflows/android.yml', 'status': 'completed', 'conclusion': 'success'}], expected='ios.yml has not succeeded')


if __name__ == '__main__': unittest.main()
